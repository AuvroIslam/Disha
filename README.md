# Disha — দিশা

**An offline, [Gemma 4](https://ai.google.dev/gemma)–powered AI disaster-response companion for Bangladesh.**
Built for the *Build with Gemma 4* Community Hackathon (Kaggle).

*Disha (দিশা) means "direction / guidance".* When floods and cyclones knock out mobile networks
in Bangladesh — exactly when coordination matters most — Disha keeps working: **Gemma 4 runs
entirely on-device**, so the app guides, triages, routes, and coordinates with no internet.

> Gemma 4 is the reasoning core, not a chatbot bolted on. It reasons over text, images, voice, and
> location and **calls tools** to coordinate disaster response — all offline.

---

## What works today (on a real Pixel 10a, offline)

| Feature | What Gemma 4 does |
| --- | --- |
| 🚑 **Rescue Triage** | Reasons over an SOS → **structured JSON** priority + risk signals + rationale (with a deterministic rule fallback) |
| 🩹 **First Aid** | **Grounded, cited** steps from offline WHO/IFRC/Red Cross packs (RAG), bilingual (বাংলা + English), red-flag banners |
| 🗺️ **Safe Shelter & Route** | Picks the GIS tool; app computes nearest **high-ground** shelter + a **flood-avoiding route** on an offline map |
| 💬 **AI Assistant** | On-device flood-safety & first-aid chat |
| 📻 **Mesh + radio** *(logic)* | Ed25519-signed SOS envelopes, multi-hop relay, ≤200-byte radio digest |

Everything runs on **Gemma 4 E2B** via **LiteRT-LM**, fully on-device.

---

## Repository layout

```
Disha/
├── prd/            Product + technical PRD (vision, features, architecture, build plan)
├── disha/          Python reasoning core (8 engines) + tests + Kaggle notebook
│   ├── core/       triage · gis · rag · summary · safety · mesh · compress · gemma
│   ├── data/       Chattogram region pack + first-aid packs + SOS scenarios
│   ├── tests/      13 unit tests (python -m disha.tests.test_core)
│   └── notebook/   Disha_Gemma_Core.ipynb (runs real Gemma 4 on Kaggle)
└── disha-android/  Android app (Kotlin + Jetpack Compose + LiteRT-LM)
    └── app/src/main/java/com/example/gemmachat/
        ├── core/   the reasoning engines, ported to Kotlin (+ JVM unit tests)
        └── ui/      home · triage · firstaid · gis · chat
```

---

## Run it

### Python core (no GPU / no model needed)
```bash
python -m disha.run_demo          # full pipeline via a deterministic mock
python -m disha.tests.test_core   # 13/13 checks
```
On Kaggle, swap the mock for real Gemma 4: `HFGemma("google/gemma-4-e4b-it")`.

### Android app (on-device Gemma 4)
1. Open `disha-android/` in **Android Studio** (uses its bundled JDK 21).
2. Build & run on an **8 GB+ RAM phone** (Android 12+). The app downloads
   **Gemma 4 E2B** (`litert-community/gemma-4-E2B-it-litert-lm`, ~2.5 GB) on first launch.
3. Everything after that runs **offline**.

---

## Gemma 4 integration
- **Model:** Gemma 4 E2B (`.litertlm`), on-device, offline, Apache-2.0 — the **only** LLM used.
- **Runtime:** LiteRT-LM (`com.google.ai.edge.litertlm:litertlm-android`).
- **Uses:** multimodal understanding, structured-JSON triage, grounded cited RAG, tool selection
  for GIS, and briefings. Math, routing, embeddings, and transport stay deterministic.
- MiniLM RAG embeddings (optional) are a non-LLM *supporting* model — allowed by the rules.

See [`prd/`](prd/) for the full design and [`disha/README.md`](disha/README.md) for the core.

---

## Credits & license
- The Android app is built on the open-source **[amrrs/gemmachat-android](https://github.com/amrrs/gemmachat-android)**
  by [1littlecoder](https://x.com/1littlecoder) — a LiteRT-LM Gemma 4 chat starter. Thanks!
- Reasoning patterns adapted (idea-level) from the Gemma-4 community projects credited in
  [`prd/REFERENCE_NOTES.md`](prd/REFERENCE_NOTES.md).
- Gemma is a Google DeepMind model; weights via the LiteRT Community on Hugging Face.
- This project: released under the MIT License (see `LICENSE`).
