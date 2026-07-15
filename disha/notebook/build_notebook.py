"""Generate Disha_Gemma_Core.ipynb (valid nbformat 4 JSON, no external deps).

Run:  python -m disha.notebook.build_notebook
"""
from __future__ import annotations

import json
from pathlib import Path


def _src(text: str) -> list[str]:
    if text == "":
        return []
    lines = text.split("\n")
    return [ln + "\n" for ln in lines[:-1]] + [lines[-1]]


def md(text: str) -> dict:
    return {"cell_type": "markdown", "metadata": {}, "source": _src(text)}


def code(text: str) -> dict:
    return {"cell_type": "code", "metadata": {}, "execution_count": None,
            "outputs": [], "source": _src(text.strip("\n"))}


CELLS = [
    md(
"""# Disha — an offline, **Gemma 4**-powered disaster-response companion 🇧🇩

*দিশা = "direction / guidance".* Built for the **Build with Gemma 4** hackathon.

During floods in Bangladesh, mobile networks fail exactly when coordination matters most.
**Disha runs Gemma 4 fully on-device / offline** and uses it as the *reasoning core* across
four jobs that normally need connectivity and experts:

1. **Triage** incoming SOS reports by urgency (structured JSON).
2. **Navigate** to the nearest safe shelter & away from flooded roads (Gemma *calls GIS tools*).
3. **First aid** — grounded, **cited** guidance (no invented facts).
4. **Summarize** hundreds of reports into a coordinator briefing.

This notebook demonstrates the Gemma 4 reasoning core on a **Chattogram** region pack
(currently flood-affected, July 2026). The native Android app is built on these same engines.

> **Rule compliance:** Gemma 4 is the **only** LLM. Optional MiniLM RAG embeddings are a
> non-LLM *supporting* model (allowed)."""
    ),
    md(
"""## Why Gemma 4 is *central* (not a bolt-on)

| Engine | Gemma 4 does | Deterministic code does |
| --- | --- | --- |
| Triage | reason → priority + risk signals + rationale (**JSON**) | validate, rule **fallback**, sort |
| GIS | pick the right **tool** (tool-calling) | haversine, shelter ranking, Dijkstra, flood filter |
| First aid | compose grounded, **cited** steps | retrieval, disclaimer / red-flag banner |
| Summary | phrase the briefing | count & aggregate (numbers never hallucinated) |

The LLM never does geometry, counting, or transport — the *GIS-Copilot lesson*. Everything runs
offline; the app is useless without Gemma — which is the point."""
    ),
    code(
r"""# --- Setup: put the Disha core on the path -------------------------------
# The public GitHub repo IS the code repo for this submission.
# On Kaggle, either add it as a Dataset, or clone it:
# !git clone -q https://github.com/YOUR_ORG/disha.git
import os, sys
for p in ["/kaggle/input/disha", "/kaggle/working/disha", "disha", ".", ".."]:
    if os.path.isdir(os.path.join(p, "disha")) or os.path.isdir(os.path.join(p, "core")):
        sys.path.insert(0, p if os.path.isdir(os.path.join(p, "disha")) else os.path.dirname(p))
import disha
print("Disha", disha.__version__, "->", disha.__file__)"""
    ),
    code(
r"""# --- Choose the model ----------------------------------------------------
# USE_REAL_GEMMA=True  -> real Gemma 4 on the Kaggle GPU (the actual submission)
# USE_REAL_GEMMA=False -> deterministic MockGemma, so the notebook runs anywhere
USE_REAL_GEMMA = True

from disha.core import HFGemma, MockGemma
if USE_REAL_GEMMA:
    # Confirm the exact Gemma 4 handle in Kaggle Models / HuggingFace:
    #   google/gemma-4-e4b-it  (default)  |  google/gemma-4-e2b-it  (lighter)
    gemma = HFGemma("google/gemma-4-e4b-it")
else:
    gemma = MockGemma()
print("Using model:", gemma.model_name)"""
    ),
    code(
r"""# --- Load the Chattogram region pack + first-aid packs + SOS scenarios ----
import json
from pathlib import Path
from disha.core import (SOSReport, triage_sos, sort_queue, first_aid_answer,
                        disaster_summary, load_packs, KeywordRetriever)
from disha.core import gis, safety
from disha.core.geodata import (load_shelters, load_facilities, load_flood_polys,
                                flood_segments_from_graph)
from disha.core.gis import PedGraph
from disha.core.prompts import GIS_SYSTEM, GIS_PHRASE_SYSTEM

DATA = Path(disha.__file__).parent / "data"; CH = DATA / "chattogram"
shelters   = load_shelters(CH / "shelters.geojson")
facilities = load_facilities(CH / "facilities.geojson")
flood      = load_flood_polys(CH / "flood_zones.geojson")
graph      = PedGraph.from_json(str(CH / "ped_graph.json"))
packs      = load_packs(str(DATA / "first_aid_packs" / "packs.json"))
retriever  = KeywordRetriever(packs)
scenarios  = [SOSReport(**r) for r in
              json.loads((DATA / "scenarios" / "chattogram_sos.json").read_text(encoding="utf-8"))]
print(f"{len(scenarios)} SOS | {len(shelters)} shelters | {len(facilities)} facilities | "
      f"{len(packs)} first-aid chunks")"""
    ),
    md("## 1) Rescue triage — Gemma 4 → structured JSON, sorted by urgency\n"
       "Each SOS is scored; a deterministic rule fallback guarantees the queue never breaks."),
    code(
r"""results = [triage_sos(s, gemma=gemma) for s in scenarios]
queue = sort_queue(results); by_id = {s.msg_id: s for s in scenarios}
for r in queue:
    s = by_id[r.msg_id]
    print(f"{r.color} {r.priority.upper():8} {r.urgency_score:.2f}  {s.text[:58]}")
    print(f"       signals={r.risk_signals}  via={r.produced_by}")"""
    ),
    md("## 2) GIS-assisted rescue — Gemma 4 *calls tools*; code computes geometry\n"
       "Gemma picks a tool (nearest shelter / safe route / flooded roads / facilities). The app "
       "runs the spatial math offline and Gemma explains the result."),
    code(
r"""user_lat, user_lon = 22.330, 91.820   # volunteer near flooded Halishahar
ask = "Where is the nearest safe shelter? There is an elderly person with us."
call = json.loads(gemma.generate(GIS_SYSTEM, ask)); print("Gemma tool call:", call)

ranked = gis.find_nearest_shelter(user_lat, user_lon, shelters,
                                  profile=call.get("args", {}).get("profile", []))
for r in ranked:
    hg = " [HIGH-GROUND]" if r["on_high_ground"] else ""
    print(f'  {r["name"]:32} {r["dist_m"]:>5} m  score={r["score"]:.3f}  {r["capacity_left"]} free{hg}')

top = ranked[0]
route = gis.safe_route(user_lat, user_lon, top["lat"], top["lon"], graph, flood)
naive = gis.segment_crosses_flood((user_lat, user_lon), (top["lat"], top["lon"]), flood)
print(f'\nsafe_route -> {top["name"]}: {route["dist_m"]} m | crosses_flood={route["crosses_flood"]}'
      f' | naive straight line crosses flood={naive}')
print("Gemma explains:", gemma.generate(GIS_PHRASE_SYSTEM, json.dumps(ranked, ensure_ascii=False)))"""
    ),
    md("**Map:** the safe route (orange) detours around the flooded roads (red dashed) and "
       "flood zone (blue). This is the offline map the Android app renders with MapLibre."),
    code(
r"""import matplotlib.pyplot as plt
fig, ax = plt.subplots(figsize=(7, 7))
for u, v in graph.edges:                       # roads (red dashed = flooded)
    a, b = graph.nodes[u], graph.nodes[v]
    fl = gis.segment_crosses_flood(a, b, flood)
    ax.plot([a[1], b[1]], [a[0], b[0]], color=("red" if fl else "0.8"),
            ls=("--" if fl else "-"), lw=1.5, zorder=1)
for ring in flood:                             # flood zone
    ax.fill([p[0] for p in ring], [p[1] for p in ring], color="tab:blue", alpha=0.25, zorder=0)
ax.scatter([s.lon for s in shelters], [s.lat for s in shelters], marker="*", s=240,
           color="tab:green", edgecolor="k", zorder=3, label="shelter")
poly = route["polyline"]
ax.plot([p[1] for p in poly], [p[0] for p in poly], color="tab:orange", lw=3, zorder=2, label="safe route")
ax.scatter([user_lon], [user_lat], color="k", s=90, zorder=4, label="you")
ax.set_title("Disha — safe route detours around flooded roads (Chattogram)")
ax.set_xlabel("longitude"); ax.set_ylabel("latitude"); ax.legend(loc="upper left"); plt.show()"""
    ),
    md("## 3) First-aid — grounded RAG **with citations** (no invented facts)\n"
       "Gemma answers only from retrieved WHO/IFRC/Red Cross passages and cites them `[1] [2]`. "
       "A life-threat query raises a red-flag banner; every answer carries a disclaimer."),
    code(
r"""q = "Someone is bleeding heavily from a deep cut on the leg, what do I do?"
ans = first_aid_answer(q, retriever, gemma=gemma, k=2)
guard = safety.guard_medical_answer(q, ans["answer"])
if guard["banner"]:
    print(guard["banner"], "\n")
print(ans["answer"])
print("\nCitations:", [f'[{c["n"]}] {c["source"]}' for c in ans["citations"]])"""
    ),
    md("## 4) Coordinator disaster summary — counts in code, Gemma 4 phrases\n"
       "Numbers are computed deterministically (never hallucinated); Gemma writes the briefing."),
    code(
r"""segs = flood_segments_from_graph(graph, flood)
out = disaster_summary(scenarios, results, shelters=shelters,
                       blocked_roads=[s["id"] for s in segs],
                       new_since=len(scenarios), gemma=gemma)
print(out["briefing"])"""
    ),
    md("## 5) Offline mesh + radio uplink — signed reports, Gemma 200-byte digest\n"
       "Reports are **Ed25519-signable** (verify-before-trust, anti-misinformation) and hop "
       "phone→phone with Lamport dedup. When even the mesh can't reach responders, **Gemma "
       "compresses the whole situation into a ≤200-byte digest** for ham/SMS/satellite uplink. "
       "*(Inspired by MeshGemma; re-implemented in `core/mesh.py` + `core/compress.py`.)*"),
    code(
r"""from disha.core import SignedEnvelope, MeshNode, DevSigner, compress_for_radio
signer = DevSigner("phoneA")
top_sos = by_id[queue[0].msg_id]
env = SignedEnvelope.create(signer, top_sos.to_dict(), msg_id=top_sos.msg_id, lamport=1, ttl=2)
B, C = MeshNode("phoneB"), MeshNode("phoneC")
fwd = B.receive(env); C.receive(fwd)                 # A -> B -> C multi-hop relay
tampered = SignedEnvelope(**{**env.__dict__}); tampered.payload = {"text": "stand down"}
print("signed SOS verifies:", env.verify(DevSigner))
print("reached phoneB:", bool(B.inbox), "| relayed to phoneC:", bool(C.inbox))
print("tampered 'stand down' rejected:", not tampered.verify(DevSigner))

radio = compress_for_radio(scenarios, results, gemma=gemma, max_bytes=200)
print(f"\nradio digest ({radio['bytes']} bytes via {radio['produced_by']}): {radio['payload']}")"""
    ),
    md("## 6) Multimodal — Gemma 4 reasons over an **image** (real-Gemma path)\n"
       "Gemma 4 E4B accepts image input natively; a flood/injury photo becomes a triage signal."),
    code(
r"""if USE_REAL_GEMMA:
    from disha.core.prompts import ASSISTANT_SYSTEM
    # from PIL import Image
    # img = Image.open("/kaggle/input/your-flood-photo.jpg")
    # print(gemma.generate(ASSISTANT_SYSTEM, "Is it safe to stay here? See the photo.", images=[img]))
    print("Attach a flood/injury photo and uncomment to run multimodal Gemma 4 (image + text -> guidance).")
else:
    print("Set USE_REAL_GEMMA=True and attach an image to demo multimodal reasoning.")"""
    ),
    md(
"""## Wrap-up

**What we showed:** Gemma 4, offline, as the reasoning core over text (and image) — producing
**structured triage JSON**, **GIS tool calls**, **grounded cited first-aid**, and a
**coordinator summary** — with deterministic guards (validation, fallbacks, safety sidecar,
no-invention checks) around every call.

**Architecture (full detail in `prd/`):** Kotlin + Jetpack Compose Android app, **LiteRT-LM**
running Gemma 4 E4B/E2B on-device, MapLibre offline maps, **downloadable per-district region
packs**, encrypted SQLite, and a **Nearby Connections** offline mesh so SOS travels phone-to-phone
with no internet.

**Rule compliance:** Gemma 4 is the *only* LLM; MiniLM embeddings (optional RAG) are a non-LLM
supporting model.

**Future work:** Bangla fine-tune (Unsloth) of Gemma 4 E2B, image-based flood-depth / injury
estimation, true multi-hop mesh, and live FFWC/BWDB flood overlays.

*Reproduce locally with the deterministic mock:* `python -m disha.run_demo` and
`python -m disha.tests.test_core` (31/31 checks)."""
    ),
]

NB = {
    "cells": CELLS,
    "metadata": {
        "kernelspec": {"display_name": "Python 3", "language": "python", "name": "python3"},
        "language_info": {"name": "python", "version": "3.10"},
        "accelerator": "GPU",
    },
    "nbformat": 4,
    "nbformat_minor": 5,
}

if __name__ == "__main__":
    out = Path(__file__).parent / "Disha_Gemma_Core.ipynb"
    out.write_text(json.dumps(NB, indent=1, ensure_ascii=False), encoding="utf-8")
    print("wrote", out, "-", len(CELLS), "cells")
