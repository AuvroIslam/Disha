"""Loaders for bundled offline geodata (a district 'region pack', see PRD F4.1)."""
from __future__ import annotations

import json
from .models import Shelter, Facility


def _features(path: str) -> list[dict]:
    with open(path, "r", encoding="utf-8") as f:
        gj = json.load(f)
    return gj.get("features", [])


def load_shelters(path: str) -> list[Shelter]:
    out = []
    for ft in _features(path):
        p = ft["properties"]
        lon, lat = ft["geometry"]["coordinates"]
        out.append(Shelter(
            id=p["id"], name=p["name"], lat=lat, lon=lon,
            capacity=p.get("capacity", 0), occupancy=p.get("occupancy", 0),
            has_pwd_access=p.get("has_pwd_access", False),
            allows_pets=p.get("allows_pets", False),
            has_medical=p.get("has_medical", False),
            on_high_ground=p.get("on_high_ground", False)))
    return out


def load_facilities(path: str) -> list[Facility]:
    out = []
    for ft in _features(path):
        p = ft["properties"]
        lon, lat = ft["geometry"]["coordinates"]
        out.append(Facility(id=p["id"], name=p["name"], lat=lat, lon=lon, type=p["type"]))
    return out


def load_flood_polys(path: str) -> list[list[list[float]]]:
    """Return exterior rings, each as [[lon,lat], ...]."""
    rings = []
    for ft in _features(path):
        geom = ft["geometry"]
        if geom["type"] == "Polygon":
            rings.append(geom["coordinates"][0])
        elif geom["type"] == "MultiPolygon":
            for poly in geom["coordinates"]:
                rings.append(poly[0])
    return rings


def flood_segments_from_graph(graph, flood_polys) -> list[dict]:
    """Derive 'flooded road' segments = graph edges that cross a flood polygon."""
    from .gis import segment_crosses_flood
    segs = []
    for i, (u, v) in enumerate(graph.edges):
        a, b = graph.nodes[u], graph.nodes[v]
        if segment_crosses_flood(a, b, flood_polys):
            segs.append({"id": f"seg_{i}", "coords": [[a[0], a[1]], [b[0], b[1]]]})
    return segs
