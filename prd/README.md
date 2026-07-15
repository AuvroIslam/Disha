# Disha — PRD Folder

**Disha** (দিশা — *"direction / guidance"*): an offline, **Gemma 4–powered** disaster-response
app for Bangladesh, for the *Build with Gemma 4* hackathon. Everything needed to start building
is here. Read in order:

| # | File | What's in it |
| --- | --- | --- |
| 1 | **[PRD.md](PRD.md)** | Vision, users, scope (MoSCoW), 6 feature specs, architecture, data models, **locked tech stack**, risks, **locked decisions** |
| 2 | **[GEMMA_INTEGRATION.md](GEMMA_INTEGRATION.md)** | On-device Gemma 4 (LiteRT-LM), **vector RAG + citations**, tool-calling contract, triage/GIS/summary **prompts + JSON schemas**, multimodal, mesh, perf |
| 3 | **[REFERENCE_NOTES.md](REFERENCE_NOTES.md)** | What each of the **9 deep-read** reference repos/papers gives us, **license caveats**, verified source URLs |
| 4 | **[BUILD_PLAN.md](BUILD_PLAN.md)** | 3-day sprint, team roles, **demo script**, Kaggle writeup outline, submission checklist |

## The 30-second version
- **Model:** Gemma 4 **E4B** (E2B hot-swap), on-device, offline, Apache-2.0. The *only* LLM (rule).
- **Gemma's job:** multimodal understanding + **tool-calling** (GIS) + **structured-JSON triage**
  + **grounded, cited** first-aid RAG + coordinator summaries. Math, transport & embeddings stay
  deterministic.
- **Stack (locked):** Kotlin + Compose + **LiteRT-LM**, forking **google-ai-edge/gallery**;
  **MiniLM ONNX vector RAG** with citations; MapLibre offline maps; encrypted SQLite;
  **Nearby Connections** mesh.
- **Closest code references:** `RaccoonOnion/ash` (RAG+citations, Apache-2.0),
  `JpCurada/likas` (GIS/evacuation, Gemma 4 E2B), `huier5635-cmd/resilience-copilot-gemma4`
  (triage safety pipeline, MIT), `google-ai-edge/gallery` (fork base, Apache-2.0).

## Locked decisions (PRD §14)
1. **Kotlin + LiteRT-LM**, fork `google-ai-edge/gallery`; **Gemma 4 E4B** (E2B hot-swap).
2. **Vector RAG (MiniLM ONNX) + citations**; FTS5 fallback.
3. Offline maps = **downloadable per-district region packs** (static base + dynamic flood overlay),
   not whole-country; demo pack = a currently-flooding district (Cox's Bazar/Chattogram/Feni).
   4. **Bangla-first bilingual**. 5. **Voice = Should-have**.
6. Roles: AI/Gemma · Mobile/UI · Geo/Data · Mesh+Integration/Demo.

> Related: [`../Disha_Project_Brief_and_References.md`](../Disha_Project_Brief_and_References.md)
> (original concept brief) · [`../compDescrip.md`](../compDescrip.md) (competition rules).
