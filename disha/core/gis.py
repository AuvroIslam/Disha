"""Disha GIS engine (mirrors GEMMA_INTEGRATION.md §5).

The LLM does NOT do geometry. Gemma picks a tool (see prompts.GIS_SYSTEM); this
module executes it deterministically over bundled offline geodata. Pure stdlib.
"""
from __future__ import annotations

import heapq
import json
import math
from dataclasses import dataclass
from typing import Optional

from .models import Shelter, Facility

EARTH_R_M = 6_371_000.0


# --------------------------------------------------------------------------- #
# Primitive geometry
# --------------------------------------------------------------------------- #
def haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Great-circle distance in metres."""
    p1, p2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlmb = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2) ** 2 + math.cos(p1) * math.cos(p2) * math.sin(dlmb / 2) ** 2
    return 2 * EARTH_R_M * math.asin(math.sqrt(a))


def _point_in_ring(lat: float, lon: float, ring: list[list[float]]) -> bool:
    """Ray-casting point-in-polygon. Ring is GeoJSON [ [lon,lat], ... ]."""
    inside = False
    n = len(ring)
    j = n - 1
    for i in range(n):
        xi, yi = ring[i][0], ring[i][1]      # lon, lat
        xj, yj = ring[j][0], ring[j][1]
        if ((yi > lat) != (yj > lat)) and (
            lon < (xj - xi) * (lat - yi) / ((yj - yi) or 1e-12) + xi
        ):
            inside = not inside
        j = i
    return inside


def point_in_flood(lat: float, lon: float, flood_polys: list[list[list[float]]]) -> bool:
    return any(_point_in_ring(lat, lon, ring) for ring in flood_polys)


def _seg_intersects_ring(a: tuple, b: tuple, ring: list[list[float]]) -> bool:
    """Does segment a-b (each (lat,lon)) cross any edge of the ring, or lie inside it?"""
    if _point_in_ring(a[0], a[1], ring) or _point_in_ring(b[0], b[1], ring):
        return True

    def ccw(p, q, r):
        return (r[1] - p[1]) * (q[0] - p[0]) > (q[1] - p[1]) * (r[0] - p[0])

    # Work in (lat, lon) space consistently.
    a_ll, b_ll = (a[0], a[1]), (b[0], b[1])
    n = len(ring)
    for i in range(n):
        c = (ring[i][1], ring[i][0])          # ring is [lon,lat] -> (lat,lon)
        d = (ring[(i + 1) % n][1], ring[(i + 1) % n][0])
        if ccw(a_ll, c, d) != ccw(b_ll, c, d) and ccw(a_ll, b_ll, c) != ccw(a_ll, b_ll, d):
            return True
    return False


def segment_crosses_flood(a: tuple, b: tuple, flood_polys: list[list[list[float]]]) -> bool:
    return any(_seg_intersects_ring(a, b, ring) for ring in flood_polys)


# --------------------------------------------------------------------------- #
# Pedestrian graph + Dijkstra safe-route
# --------------------------------------------------------------------------- #
@dataclass
class PedGraph:
    """Nodes: {id: (lat, lon)}. Edges: list of (u, v) undirected."""
    nodes: dict[str, tuple[float, float]]
    edges: list[tuple[str, str]]

    @classmethod
    def from_json(cls, path: str) -> "PedGraph":
        with open(path, "r", encoding="utf-8") as f:
            g = json.load(f)
        nodes = {k: (v[0], v[1]) for k, v in g["nodes"].items()}
        edges = [(e[0], e[1]) for e in g["edges"]]
        return cls(nodes=nodes, edges=edges)

    def nearest_node(self, lat: float, lon: float) -> str:
        return min(self.nodes, key=lambda nid: haversine_m(lat, lon, *self.nodes[nid]))

    def adjacency(self, flood_polys: Optional[list] = None) -> dict[str, list[tuple[str, float]]]:
        """Weighted adjacency; edges crossing flood are dropped when flood_polys given."""
        adj: dict[str, list[tuple[str, float]]] = {n: [] for n in self.nodes}
        for u, v in self.edges:
            a, b = self.nodes[u], self.nodes[v]
            if flood_polys and segment_crosses_flood(a, b, flood_polys):
                continue
            w = haversine_m(*a, *b)
            adj[u].append((v, w))
            adj[v].append((u, w))
        return adj


def dijkstra(adj: dict[str, list[tuple[str, float]]], src: str, dst: str):
    """Return (path_node_ids, total_dist_m) or (None, inf) if unreachable."""
    dist = {src: 0.0}
    prev: dict[str, str] = {}
    pq = [(0.0, src)]
    seen = set()
    while pq:
        d, u = heapq.heappop(pq)
        if u in seen:
            continue
        seen.add(u)
        if u == dst:
            break
        for v, w in adj.get(u, ()):
            nd = d + w
            if nd < dist.get(v, math.inf):
                dist[v] = nd
                prev[v] = u
                heapq.heappush(pq, (nd, v))
    if dst not in dist:
        return None, math.inf
    path = [dst]
    while path[-1] != src:
        path.append(prev[path[-1]])
    path.reverse()
    return path, dist[dst]


# --------------------------------------------------------------------------- #
# Tools (what Gemma may call)
# --------------------------------------------------------------------------- #
# Weights from the likas reference (profile-aware shelter ranking).
W_DIST, W_PWD, W_PET, W_CAP = 0.4, 0.3, 0.2, 0.1
HIGH_GROUND_BONUS = 0.15


def find_nearest_shelter(user_lat, user_lon, shelters: list[Shelter],
                         profile: Optional[list[str]] = None, top_n: int = 3) -> list[dict]:
    """Lower score = better. Profile-aware; boosts high-ground shelters."""
    profile = profile or []
    want_pwd = "pwd" in profile or "elderly" in profile
    want_pet = "pet" in profile
    if not shelters:
        return []
    dists = {s.id: haversine_m(user_lat, user_lon, s.lat, s.lon) for s in shelters}
    dmax = max(dists.values()) or 1.0
    ranked = []
    for s in shelters:
        norm_d = dists[s.id] / dmax
        pwd_penalty = 0.0 if (not want_pwd or s.has_pwd_access) else 1.0
        pet_penalty = 0.0 if (not want_pet or s.allows_pets) else 1.0
        score = (W_DIST * norm_d + W_PWD * pwd_penalty +
                 W_PET * pet_penalty + W_CAP * s.capacity_pressure)
        if s.on_high_ground:
            score -= HIGH_GROUND_BONUS
        if s.capacity_left <= 0:
            score += 0.5                       # deprioritise full shelters
        ranked.append((score, s, dists[s.id]))
    ranked.sort(key=lambda t: t[0])
    return [
        {
            "shelter_id": s.id, "name": s.name, "dist_m": round(dist),
            "score": round(score, 3), "capacity_left": s.capacity_left,
            "has_pwd_access": s.has_pwd_access, "on_high_ground": s.on_high_ground,
            "lat": s.lat, "lon": s.lon,
        }
        for score, s, dist in ranked[:top_n]
    ]


def safe_route(from_lat, from_lon, to_lat, to_lon, graph: PedGraph,
               flood_polys: list) -> dict:
    """Dijkstra over the pedestrian graph, excluding flooded edges."""
    adj = graph.adjacency(flood_polys=flood_polys)
    src = graph.nearest_node(from_lat, from_lon)
    dst = graph.nearest_node(to_lat, to_lon)
    path, dist = dijkstra(adj, src, dst)
    if path is None:
        # Fall back to a direct line and flag the flood crossing.
        crosses = segment_crosses_flood((from_lat, from_lon), (to_lat, to_lon), flood_polys)
        return {"polyline": [[from_lat, from_lon], [to_lat, to_lon]],
                "dist_m": round(haversine_m(from_lat, from_lon, to_lat, to_lon)),
                "crosses_flood": crosses, "routable": False}
    poly = [[graph.nodes[n][0], graph.nodes[n][1]] for n in path]
    return {"polyline": poly, "dist_m": round(dist), "crosses_flood": False, "routable": True}


def flooded_roads_near(lat, lon, radius_m: int, flood_segments: list[dict]) -> dict:
    """flood_segments: [{'id':..,'coords':[[lat,lon],[lat,lon]]}]."""
    hits = []
    for seg in flood_segments:
        mid_lat = sum(p[0] for p in seg["coords"]) / len(seg["coords"])
        mid_lon = sum(p[1] for p in seg["coords"]) / len(seg["coords"])
        if haversine_m(lat, lon, mid_lat, mid_lon) <= radius_m:
            hits.append(seg)
    return {"segments": hits, "count": len(hits)}


def nearby_facilities(lat, lon, ftype: str, facilities: list[Facility], top_n: int = 3) -> list[dict]:
    matches = [f for f in facilities if f.type == ftype]
    matches.sort(key=lambda f: haversine_m(lat, lon, f.lat, f.lon))
    return [
        {"id": f.id, "name": f.name, "type": f.type,
         "dist_m": round(haversine_m(lat, lon, f.lat, f.lon)), "lat": f.lat, "lon": f.lon}
        for f in matches[:top_n]
    ]


# --------------------------------------------------------------------------- #
# Deterministic fallback tool router (when Gemma's tool call is invalid)
# --------------------------------------------------------------------------- #
def keyword_tool_fallback(text: str) -> dict:
    t = text.lower()
    if any(k in t for k in ("shelter", "safe place", "evacuat", "আশ্রয়")):
        prof = []
        if any(k in t for k in ("elder", "old", "বয়স্ক")):
            prof.append("elderly")
        if any(k in t for k in ("wheelchair", "disab", "pwd", "প্রতিবন্ধ")):
            prof.append("pwd")
        if any(k in t for k in ("pet", "dog", "cat")):
            prof.append("pet")
        return {"tool": "find_nearest_shelter", "args": {"profile": prof}}
    if any(k in t for k in ("hospital", "clinic", "doctor", "হাসপাতাল")):
        return {"tool": "nearby_facilities", "args": {"type": "hospital"}}
    if any(k in t for k in ("relief", "food", "water", "ত্রাণ")):
        return {"tool": "nearby_facilities", "args": {"type": "relief"}}
    if any(k in t for k in ("flood road", "blocked", "which road", "রাস্তা")):
        return {"tool": "flooded_roads_near", "args": {"radius_m": 2000}}
    if any(k in t for k in ("route", "way to", "how do i get", "path")):
        return {"tool": "safe_route", "args": {}}
    return {"tool": "none", "args": {}}
