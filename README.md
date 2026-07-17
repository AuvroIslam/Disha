# Disha — দিশা

**An offline, [Gemma 4](https://ai.google.dev/gemma)–powered AI disaster-response companion for Bangladesh.**
Built for the *Build with Gemma 4* Community Hackathon (Kaggle).

*Disha (দিশা) means "direction / guidance".* When floods and cyclones knock out mobile networks
in Bangladesh — exactly when coordination matters most — Disha keeps working: **Gemma 4 runs
entirely on-device**, so the app guides, triages, routes, and coordinates with no internet.

> Gemma 4 is the reasoning core, not a chatbot bolted on. It reasons over text, images, and
> location and **calls tools** to coordinate disaster response — all offline, on the phone.

Verified end-to-end on real Android phones (Pixel 10a, Samsung Galaxy S21 FE), fully offline.

---

## What works today

| Feature | What it does |
| --- | --- |
| 🚑 **Rescue Triage** | Type or **photograph** an SOS → Gemma returns **structured-JSON** priority + risk signals + rationale, ranked into a queue (deterministic rule fallback if the model is off). Captures real GPS. |
| 🩹 **First Aid** | **Grounded, cited** steps from offline WHO/IFRC/Red Cross packs (RAG). Answers in the app language, life-threat red-flag banner, and citations that show **only the sources actually used**. |
| 🗺️ **Safe Shelter & Route** | Uses your **real GPS** anywhere in Bangladesh. In a detailed region it draws a **flood-avoiding walking route on real OpenStreetMap streets**; elsewhere it finds the nearest of **9,500+ real schools/colleges** (the buildings used as flood shelters) + safe-direction guidance. |
| 📋 **Coordinator Summary** | Aggregates the **real** reports on the device (from Triage + Mesh) — counts computed in code, Gemma writes the briefing. Never invented numbers. |
| 📻 **Mesh SOS** | Ed25519-signed SOS **phone-to-phone** over Bluetooth/Wi-Fi (Nearby Connections), multi-hop relay, verify-before-trust. Works with no internet. |
| 💬 **AI Assistant** | On-device flood-safety & first-aid chat. |
| 🌐 **Full Bangla mode** | One toggle switches the **entire UI and every Gemma answer** between English and বাংলা. |
| 🧭 **Flood drill** | A guided walkthrough that seeds sample reports and hands you through each tool, so you can practise before a real emergency. |
| 🗾 **Region packs** | Nationwide basic coverage (64 districts); three districts ship a detailed offline street map. |

Everything runs on **Gemma 4 E2B** via **LiteRT-LM**, fully on-device.

---

## Technical hurdles & how we solved them

The hard part of "AI for disasters" is that the model has to run **on a phone, offline**, and be
**trustworthy** with real data. These are the problems we actually hit and how we fixed each.

### On-device Gemma
- **LiteRT-LM allows only ONE session at a time.** Calling a triage/first-aid task while the main
  chat session was open threw `FAILED_PRECONDITION`. → We wrote a `generateWith()` that **closes the
  main chat session, runs a temporary task session with its own system prompt, then restores the
  main session** — so every engine gets an isolated prompt without leaking chat state.
- **Toolchain mismatch.** System JDK 25 was too new for Gradle/AGP. → Build with **Android Studio's
  bundled JDK 21** (`JAVA_HOME=.../Android Studio/jbr`).
- **Model size vs. device RAM.** The `gemma-4-E2B-it.litertlm` model is ~2.5 GB and won't load on an
  emulator or low-RAM device. → Target real phones (~6 GB+ RAM); onboarding is scrollable with a
  "continue without the model" path so maps/mesh still work if the model isn't downloaded.

### Offline mesh
- **Nearby Connections failed with `8034 MISSING_PERMISSION_ACCESS_COARSE_LOCATION`.** A
  `maxSdkVersion` cap on the location permission and missing runtime grants blocked discovery. →
  Removed the cap, requested COARSE/FINE + Bluetooth/Nearby-Wi-Fi at runtime, and ensured location
  services are on. Signed SOS then delivered phone-to-phone with multi-hop relay.

### Trustworthy First Aid (RAG)
- **Retrieval was too shallow.** With `k=2`, a "not breathing" query pulled the drowning + recovery
  passages but **cut off the passage with the actual CPR technique** — so Gemma safely refused
  instead of giving compressions. → Raised retrieval to **k=4** so the critical passage is always in
  scope.
- **Any-language retrieval without maintaining two tag sets.** The knowledge base is English; a
  Bangla query matched nothing. → A **hybrid retriever**: English tags stay the single source of
  truth, English queries match instantly, and a non-Latin query that misses is **translated to
  English once** (a dedicated, language-directive-free Gemma call) and retried. No dual maintenance,
  no latency on the common path, and it generalises to any language.
- **Bilingual answers were stunted.** A hardcoded "Bangla then English" instruction fought the app's
  language toggle, so one language came out complete and the other a stub. → Language is now driven
  by a single **app-language directive**, plus a **completeness rule** ("keep every number/rate/dose
  verbatim, e.g. 100–120/min").
- **Keyword hijacking & noisy citations.** "Burn from boiling water" scored high on *drowning*
  (because of "water"); noisy single-word matches polluted results. → We lean on the **two-layer
  design**: cast a wide net (k=4) and let **Gemma's grounding rules** pick the right passage and
  refuse when nothing fits (verified: it declines to give drowning advice for a burn, and still
  gives correct CPR for "collapsed and won't wake up"). We also **filter the Sources list to only
  the passages the answer actually cites** — so a CPR answer no longer looks like it came from a
  snakebite guideline.

### Real data, not demos
- **Coordinator Summary was reading bundled sample scenarios.** → Introduced a shared `SosRepository`
  fed by **real** Triage and Mesh reports; the briefing now reflects the actual situation on the
  device, with a proper empty state.
- **Hardcoded coordinates.** → Real GPS via `FusedLocationProvider`, with a graceful region-centre
  fallback when GPS is unavailable.
- **The app only worked in 3 hand-authored regions.** Floods hit far more of Bangladesh. → Bundled
  the open **64-district dataset** ([nuhil/bangladesh-geocode](https://github.com/nuhil/bangladesh-geocode),
  gov.bd-sourced, with Bangla names) to reverse-geocode any GPS fix to its district, so the app
  works **anywhere** and degrades gracefully where there's no detailed map.
- **No usable shelter dataset.** The government portal (GeoDASH) was **down at the DNS level**
  (SERVFAIL from multiple resolvers). → Used **HOT-OSM education facilities**
  (`hotosm_bgd_education_facilities`, ODbL): **9,525 schools/colleges across all 64 districts** — the
  buildings the government designates as flood/cyclone shelters — as the nationwide shelter layer.
- **Routing ran on a synthetic 12-node grid.** → Pulled the **real road network from OpenStreetMap**
  (Overpass) for the three detailed districts, simplified each to a routable junction graph
  (~5k/1.5k/0.5k nodes), verified connectivity, and route flood-avoidance now runs on actual
  streets. The flood **extent** is clearly labelled as an illustrative scenario (a live extent would
  need an FFWC/satellite feed).

### Reliability engineering
- **Python-first, then port.** The eight reasoning engines (triage, gis, rag, summary, safety, mesh,
  compress, gemma) were built and **unit-tested in Python**, then ported to Kotlin with JVM tests —
  so the disaster logic is deterministic and verifiable independent of the model.

---

## Architecture

```
User (text / photo / GPS)
        │
        ▼
  Gemma 4 E2B  ──►  reasons + selects tools + writes prose      (LiteRT-LM, on-device)
        │
        ▼
  Deterministic core (Kotlin, unit-tested):
     triage JSON • RAG retrieval + citations • GIS geometry (Dijkstra, flood-avoid)
     • summary counts • Ed25519 mesh • safety guards
        │
        ▼
  Offline data packs: OSM roads • 64 districts • 9,525 shelters • first-aid packs
```

Gemma does the **reasoning and language**; math, routing, retrieval, crypto, and transport stay
**deterministic** so they're testable and never "hallucinate" a number.

---

## Repository layout

```
Disha/
├── prd/            Product + technical PRD (vision, features, architecture, build plan)
├── disha/          Python reasoning core (8 engines) + tests + Kaggle notebook
├── disha-android/  Android app (Kotlin + Jetpack Compose + LiteRT-LM)  ← the product
│   └── app/src/main/
│       ├── java/com/example/gemmachat/
│       │   ├── core/       reasoning engines (triage, gis, rag, summary, safety, mesh…)
│       │   ├── data/       AppPrefs · Regions · BdGeo · PublicShelters · SosRepository · RegionAssets
│       │   ├── inference/  EngineHolder (LiteRT-LM), GemmaLlmEngine
│       │   ├── location/   FusedLocation provider
│       │   └── ui/         home · triage · firstaid · gis · mesh · summary · chat · settings · demo · guide
│       └── assets/         OSM road graphs · bd_districts.json · bd_shelters.json · first_aid_packs.json · region packs
└── uiImages/       Design references and app art
```

---

## Run it

### Android app (on-device Gemma 4)
1. Open `disha-android/` in **Android Studio** (uses its bundled JDK 21).
2. Build & run on a **~6 GB+ RAM phone** (Android 12+). The app downloads
   **Gemma 4 E2B** (`litert-community/gemma-4-E2B-it-litert-lm`, ~2.5 GB) on first launch.
3. Everything after that runs **offline**. Try the **flood drill** on the home screen for a tour.

### Python core (no GPU / no model needed)
```bash
python -m disha.run_demo          # full pipeline via a deterministic mock
python -m disha.tests.test_core   # unit checks
```

---

## Gemma 4 integration
- **Model:** Gemma 4 E2B (`.litertlm`), on-device, offline — the **only** LLM used.
- **Runtime:** LiteRT-LM (`com.google.ai.edge.litertlm:litertlm-android`).
- **Uses:** multimodal (image) triage, structured-JSON output, grounded cited RAG, cross-language
  translation for retrieval, tool selection for GIS, and coordinator briefings. Math, routing,
  retrieval, crypto, and transport stay deterministic.

---

## Data & attribution
- **Roads & shelters:** © **OpenStreetMap** contributors (ODbL) — road networks via Overpass;
  shelters via HOT-OSM `hotosm_bgd_education_facilities`.
- **Districts:** [nuhil/bangladesh-geocode](https://github.com/nuhil/bangladesh-geocode) (gov.bd-sourced).
- **First-aid content:** grounded in WHO / IFRC / Red Cross guidance.
- Flood extents in the detailed packs are **illustrative scenarios**, not live flood data.

## Credits & license
- The Android app started from the open-source **[amrrs/gemmachat-android](https://github.com/amrrs/gemmachat-android)**
  by [1littlecoder](https://x.com/1littlecoder) — a LiteRT-LM Gemma 4 chat starter. Thanks!
- Gemma is a Google DeepMind model; weights via the LiteRT Community on Hugging Face.
- Released under the MIT License (see `LICENSE`).
