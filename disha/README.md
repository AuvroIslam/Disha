# Disha — Gemma 4 Reasoning Core

**Disha** (দিশা, *"direction / guidance"*) is an offline, **Gemma 4–powered** disaster-response
companion for Bangladesh. This directory is the **runnable reasoning core** + a **Kaggle
notebook** demo — the first deliverable of the project (the native Android app is built on top
of these same engines; see [`../prd/`](../prd)).

> Built for the *Build with Gemma 4* Community Hackathon. **Gemma 4 is the only LLM used.**
> MiniLM embeddings (optional, for RAG) are a non-LLM *supporting* model, which the rules allow.

---

## What Gemma 4 does here

| Engine | Gemma 4's job | Deterministic code's job |
| --- | --- | --- |
| **Triage** (`core/triage.py`) | reason over an SOS → priority + risk signals + rationale (**JSON**) | schema validation, rule **fallback**, sorting |
| **GIS** (`core/gis.py`) | pick the right **tool** (tool-calling) | haversine, weighted shelter ranking, Dijkstra safe-route, flood filter |
| **First aid** (`core/rag.py`) | compose grounded, **cited** steps | retrieval, disclaimer/red-flag banner |
| **Summary** (`core/summary.py`) | phrase the coordinator briefing | count/aggregate (numbers never hallucinated) |
| **Radio compress** (`core/compress.py`) | squeeze incidents into a **≤200-byte** uplink digest | size-bound guarantee + fallback |
| **Mesh** (`core/mesh.py`) | — | **Ed25519-signed** envelopes, verify-before-trust, Lamport dedup, multi-hop relay |
| **Safety** (`core/safety.py`) | — | injection defense, no-invention audit, disclaimers |

*Mesh signing + radio compression are inspired by [MeshGemma](https://github.com/JasperG134/MeshGemma); re-implemented in our Apache-2.0 code.*

The LLM never does geometry, counting, or transport — exactly the GIS-Copilot lesson. This is
the core argument for the writeup's "Gemma is central, yet grounded and safe."

---

## Layout

```
disha/
  core/            reasoning engines (pure stdlib — no torch needed to run/test)
    models.py      data models (PRD §10)
    prompts.py     all Gemma 4 system prompts
    gemma.py       HFGemma (real Gemma 4 via transformers) + MockGemma (deterministic)
    triage.py  gis.py  rag.py  summary.py  safety.py  geodata.py
  data/
    chattogram/    demo region pack: shelters/facilities/flood GeoJSON + pedestrian graph
    first_aid_packs/packs.json     first-aid knowledge base (WHO/IFRC/Red Cross)
    scenarios/chattogram_sos.json  8 scripted flood SOS cases
  tests/test_core.py   31 assertions (all passing)
  run_demo.py          full offline end-to-end demo (MockGemma)
  notebook/            Kaggle notebook (real Gemma 4) — see build_notebook.py
```

---

## Run it (no GPU / no model needed)

```bash
# from the repo root (d:/GemmaHackathon)
python -m disha.run_demo          # full pipeline via MockGemma
python -m disha.tests.test_core   # 31/31 checks
```

`MockGemma` is a deterministic stand-in so the whole pipeline runs offline for CI/dev.

## Run with real Gemma 4 (Kaggle GPU or a flagship)

```python
from disha.core import HFGemma, triage_sos, SOSReport
gemma = HFGemma("google/gemma-4-e4b-it")     # confirm the exact handle on Kaggle
print(triage_sos(SOSReport(text="Pregnant woman trapped on the roof, water rising"),
                 gemma=gemma))
```

Swap `MockGemma()` → `HFGemma(...)` anywhere; every engine behaves identically.

---

## Notes
- **Region packs:** `data/chattogram/` is one downloadable district pack (PRD F4.1). Static base
  (shelters/facilities/graph) + a dynamic `flood_zones` overlay.
- **Model handle:** `google/gemma-4-e4b-it` is a placeholder — confirm the exact Gemma 4 E4B/E2B
  handle in Kaggle Models / HuggingFace before running.
- **Only Gemma:** no other LLM anywhere. Optional MiniLM RAG embeddings are a supporting model.
