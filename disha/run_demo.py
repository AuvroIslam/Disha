"""End-to-end Disha demo over the Chattogram region pack.

Runs the full Gemma-driven pipeline (triage -> GIS tool-calling -> cited first-aid
RAG -> coordinator summary) using MockGemma so it executes with no GPU/model.
Swap `MockGemma()` for `HFGemma("google/gemma-4-e4b-it")` on Kaggle for real Gemma 4.

Run:  python -m disha.run_demo        (cwd = repo root d:/GemmaHackathon)
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

# Make emoji/Bangla print on any console (Windows cp1252, etc.)
try:
    sys.stdout.reconfigure(encoding="utf-8")
except Exception:
    pass

from disha.core import (SOSReport, triage_sos, sort_queue, first_aid_answer,
                        disaster_summary, load_packs, KeywordRetriever, MockGemma)
from disha.core import gis, safety
from disha.core.geodata import (load_shelters, load_facilities, load_flood_polys,
                                flood_segments_from_graph)
from disha.core.gis import PedGraph
from disha.core.prompts import GIS_SYSTEM, GIS_PHRASE_SYSTEM

DATA = Path(__file__).parent / "data"
CHATTOGRAM = DATA / "chattogram"


def load_scenarios() -> list[SOSReport]:
    rows = json.loads((DATA / "scenarios" / "chattogram_sos.json").read_text(encoding="utf-8"))
    return [SOSReport(**r) for r in rows]


def hr(title):
    print("\n" + "=" * 72 + f"\n{title}\n" + "=" * 72)


def main():
    gemma = MockGemma()   # <-- swap for HFGemma(...) on Kaggle
    shelters = load_shelters(CHATTOGRAM / "shelters.geojson")
    facilities = load_facilities(CHATTOGRAM / "facilities.geojson")
    flood_polys = load_flood_polys(CHATTOGRAM / "flood_zones.geojson")
    graph = PedGraph.from_json(str(CHATTOGRAM / "ped_graph.json"))
    packs = load_packs(str(DATA / "first_aid_packs" / "packs.json"))
    retriever = KeywordRetriever(packs)
    scenarios = load_scenarios()

    # ---- 1. TRIAGE all incoming SOS, then prioritise -------------------- #
    hr("1) RESCUE TRIAGE  (Gemma -> JSON, deterministic fallback if invalid)")
    results = [triage_sos(s, gemma=gemma) for s in scenarios]
    queue = sort_queue(results)
    sos_by_id = {s.msg_id: s for s in scenarios}
    for r in queue:
        s = sos_by_id[r.msg_id]
        print(f"{r.color} {r.priority.upper():8} score={r.urgency_score:.2f} "
              f"review={'Y' if r.needs_human_review else 'N'} :: {s.text[:60]}")
        print(f"     signals={r.risk_signals} via={r.produced_by}")

    # ---- 2. GIS TOOL-CALLING  (Gemma picks tool; code computes geometry) - #
    hr("2) GIS-ASSISTED RESCUE  (Gemma tool-calling over offline geodata)")
    user_lat, user_lon = 22.330, 91.820        # volunteer near flooded Halishahar
    ask = "Where is the nearest safe shelter? There is an elderly person with us."
    tool_call = json.loads(gemma.generate(GIS_SYSTEM, ask))
    print(f"user asks: {ask}\nGemma tool call: {tool_call}")
    if tool_call.get("tool") == "find_nearest_shelter":
        ranked = gis.find_nearest_shelter(user_lat, user_lon, shelters,
                                          profile=tool_call["args"].get("profile", []))
        phrase = gemma.generate(GIS_PHRASE_SYSTEM, json.dumps(ranked, ensure_ascii=False))
        print("ranked shelters:")
        for r in ranked:
            hg = " [HIGH-GROUND]" if r["on_high_ground"] else ""
            print(f"   {r['name']} — {r['dist_m']} m, score {r['score']}, "
                  f"{r['capacity_left']} free{hg}")
        print("Gemma says:", phrase)

    # safe route to the top shelter, avoiding flood
    top = ranked[0]
    route = gis.safe_route(user_lat, user_lon, top["lat"], top["lon"], graph, flood_polys)
    naive_crosses = gis.segment_crosses_flood((user_lat, user_lon),
                                              (top["lat"], top["lon"]), flood_polys)
    print(f"\nsafe_route -> {top['name']}: {route['dist_m']} m, "
          f"routable={route['routable']}, crosses_flood={route['crosses_flood']}")
    print(f"   (naive straight line would cross flood: {naive_crosses})")
    print(f"   path: {[ '%.3f,%.3f'%(p[0],p[1]) for p in route['polyline'] ]}")

    # flooded roads + nearest hospital
    segs = flood_segments_from_graph(graph, flood_polys)
    near_flood = gis.flooded_roads_near(user_lat, user_lon, 3000, segs)
    hosp = gis.nearby_facilities(user_lat, user_lon, "hospital", facilities)
    print(f"\nflooded_roads_near (3km): {near_flood['count']} blocked segments")
    print(f"nearest hospital: {hosp[0]['name']} — {hosp[0]['dist_m']} m")

    # ---- 3. FIRST-AID RAG with citations -------------------------------- #
    hr("3) FIRST-AID RAG  (grounded + cited)")
    q = "Someone is bleeding heavily from a deep cut on the leg, what do I do?"
    ans = first_aid_answer(q, retriever, gemma=gemma, k=2)
    guard = safety.guard_medical_answer(q, ans["answer"])
    print(f"Q: {q}")
    if guard["banner"]:
        print(guard["banner"])
    print("A:", ans["answer"])
    print("citations:", [f"[{c['n']}] {c['source']}" for c in ans["citations"]])

    # ---- 4. COORDINATOR SUMMARY ----------------------------------------- #
    hr("4) COORDINATOR DISASTER SUMMARY  (counts in code, Gemma phrases)")
    out = disaster_summary(scenarios, results, shelters=shelters,
                           blocked_roads=[s["id"] for s in segs],
                           new_since=len(scenarios), gemma=gemma)
    print(out["briefing"])

    # ---- 5. SIGNED MESH RELAY + RADIO COMPRESSION (MeshGemma-inspired) --- #
    hr("5) OFFLINE MESH  (Ed25519-signable envelopes, multi-hop, dedup)")
    from disha.core import SignedEnvelope, MeshNode, DevSigner, compress_for_radio
    signer = DevSigner("phoneA")
    top_sos = sos_by_id[queue[0].msg_id]
    env = SignedEnvelope.create(signer, top_sos.to_dict(), msg_id=top_sos.msg_id,
                                lamport=1, ttl=2)
    B, C = MeshNode("phoneB"), MeshNode("phoneC")
    fwd = B.receive(env)                 # A -> B (accept + forward)
    C.receive(fwd)                       # B -> C (relay hop)
    tampered = SignedEnvelope(**{**env.__dict__}); tampered.payload = {"text": "stand down"}
    print(f"signed SOS from phoneA verifies: {env.verify(DevSigner)}")
    print(f"reached phoneB inbox: {bool(B.inbox)} | relayed to phoneC inbox: {bool(C.inbox)}")
    print(f"tampered 'stand down' envelope rejected: {not tampered.verify(DevSigner)}")
    print(f"duplicate dropped on re-receive: {B.receive(env) is None}")

    hr("6) RADIO-UPLINK COMPRESSION  (Gemma -> <=200-byte digest)")
    radio = compress_for_radio(scenarios, results, gemma=gemma, max_bytes=200)
    print(f"digest ({radio['bytes']} bytes, via {radio['produced_by']}): {radio['payload']}")

    print("\n" + "=" * 72)
    print("Demo complete — all engines ran offline via MockGemma.")
    print("On Kaggle: gemma = HFGemma('google/gemma-4-e4b-it')  # real Gemma 4")
    print("=" * 72)


if __name__ == "__main__":
    main()
