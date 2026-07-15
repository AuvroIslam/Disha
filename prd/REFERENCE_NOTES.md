# Disha — Reference Notes (Repos, Papers, Sources)

> Companion to [PRD.md](PRD.md) / [GEMMA_INTEGRATION.md](GEMMA_INTEGRATION.md).
> Repos/papers we **deep-read**, **what to borrow**, and **license caveats**.
> ⚖️ *Ideas and algorithms are free to reuse. Copying source code is governed by each repo's
> license — check the badge before pasting anything.*
>
> **Deep-read (architecture + implementation):** likas, resilience-copilot-gemma4, offlineaid,
> AI-CareCompanion, google-ai-edge/gallery, **ash, Beacon, trij, floodpulse-nairobi**.

---

## 1. Highest-value references

### 🥇 ash — `RaccoonOnion/ash`  *(offline survival RAG, Apache-2.0)* — **best RAG blueprint**
Flutter + **LiteRT-LM** running **Gemma 4 E2B (1.4 GB) / E4B (3.7 GB)**, Metal GPU. This is the
model for Disha's **first-aid RAG with citations**.
- **Borrow (→ GEMMA §8):**
  - **Semantic vector RAG:** MiniLM-L6-v2 (384-dim, 86 MB ONNX) embeddings → **ObjectBox HNSW**
    cosine index → top-k retrieval → **2-pass RAG rewrite with inline citation chips `[1][2]`**.
  - **`rag_preprocessor.py`** build-time chunking of markdown packs (56 hazard packs).
  - **"Library Lens"** = pin a chat to specific pack(s) → scoped retrieval.
  - **KV-cache management:** warn at 85%, one-tap trim oldest 30% or token-bump.
  - **Model hot-swap E2B↔E4B** without re-download (Models tab).
  - **Sentence-level streaming → TTS**; voice mode swaps to plain-prose system prompt.
  - **Citation deep-linking:** tap `[n]` → opens source passage with highlight.
- **License:** **Apache-2.0** → reusable with attribution (Gemma is a Google trademark).

### 🥇 likas — `JpCurada/likas`  *(offline AI evacuation, Gemma 4 E2B)* — **best GIS blueprint**
RN + **`llama.rn`/llama.cpp**, **Gemma 4 E2B** fine-tuned (Unsloth) → **Q4_K_M GGUF ~1.8 GB**,
**MapLibre** local tiles, Zustand.
- **Borrow (→ GEMMA §5):**
  - **Grammar-constrained tool dispatch (GBNF)** — model outputs *tool calls only*; temp ~0.4.
  - **Dijkstra over precomputed OSM pedestrian graph** for offline routing → `safe_route`.
  - **Profile-weighted shelter ranking** `distance·0.4 + pwd·0.3 + pet·0.2 + capacity·0.1`
    → `find_nearest_shelter`.
  - **Bundled OSM POI nearest-neighbor** → `nearby_facilities`.
  - **Deterministic keyword-router fallback**.
- **License:** verify on repo before copying code; patterns/algorithms safe to reuse.

### 🥇 resilience-copilot-gemma4 — `huier5635-cmd/resilience-copilot-gemma4`  *(triage agent, MIT)*
Python + **Gemma 4** + LangGraph. **Blueprint for Triage (F3), Safety Sidecar, Summary (F5).**
- **Borrow (→ GEMMA §4, §10):**
  - **Bounded-agent pipeline:** normalize → risk-signal detect → playbook match → LLM →
    safety-contract check → resource verify → human-review trigger → structured JSON export.
  - **Risk-signal set** (oxygen, medication, heat, flood, transport, shelter, pet, power…).
  - **Safety contract:** no diagnosis, no invented resources/capacity/roads, human-review for
    high-risk, memory can't override policy.
  - **Deterministic fallback mode** (reproducibility); **audit trace** (great for "auditable" story).
- **License:** **MIT** → reusable with attribution.

### 🥇 MeshGemma — `JasperG134/MeshGemma`  *(offline disaster MESH, Gemma 4)* — **best mesh blueprint**
iOS/Expo RN + **llama.rn** (Gemma 4 **E2B ~2.29 GB + vision projector ~986 MB**), Swift native
modules. The reference our F6 mesh was missing. **This is a sibling/competitor project — study it.**
- **Borrow (→ implemented in `core/mesh.py`, `core/compress.py`):**
  - **Signed envelopes:** **Ed25519** signature over a canonical (sorted-key) serialization;
    verify-before-trust; **Lamport clock** anti-replay. (We shipped this + tamper tests.)
  - **Multi-transport mesh:** WiFi/TCP over mDNS (port 4000) + **MultipeerConnectivity**
    (BT + WiFi-Direct, ~30 m, works in airplane mode) + **BLE presence beacon**; per-transport
    dedup feeding one signed-envelope handler. (Our Android plan = Nearby Connections; adopt the
    layered idea.)
  - **Gemma radio-uplink compression:** Gemma squeezes ≤50 records into a **≤200-byte JSON** for
    ham/SMS/satellite uplink. (We shipped this + size-bound tests.)
  - **On-device photo triage** via a vision projector — proves multimodal works on **llama.rn**
    (relevant to our RN fallback stack).
  - Offline MapLibre with `OfflineManager` pre-downloading OSM tiles to SQLite.
- **⚠️ License:** **CC-BY 4.0** — reuse with attribution (note: CC licenses are content-oriented;
  re-implement the *ideas* in our Apache-2.0 code rather than copying files, and credit MeshGemma).
- **What Disha does that MeshGemma doesn't:** structured triage JSON + fallback + priority queue,
  GIS safe-routing + profile/high-ground shelter ranking, grounded **cited** first-aid RAG,
  coordinator summary. (Our reasoning is broader; their mesh is more mature.)

### 🥇 Google AI Edge Gallery — `google-ai-edge/gallery`  *(Apache-2.0 — FORK THIS)*
Kotlin + Compose, **LiteRT-LM**, HF model download. The **working on-device Gemma app to fork**.
- **Borrow:** model **download + session mgmt + chat UI**, **Ask Image** (multimodal), **Audio
  Scribe** (voice→text), Thinking Mode. Folders: `Android/`, `skills/`, `mcp/`, `model_allowlists/`.
- **License:** **Apache-2.0** → fork/reuse with attribution. De-risks the hardest plumbing.

---

## 2. Strong supporting references

### Beacon — `wimi321/Beacon`  *(offline multilingual survival, LiteRT, Gemma 4)*
React + **Capacitor** + Kotlin/ObjC++; **LiteRT** running Gemma 4 (E2B ~2.6 GB, E4B ~3.7 GB).
- **Borrow:**
  - **Grounded RAG before inference** over a **14,406-entry** authoritative KB (Army Survival,
    WHO, CDC, Red Cross, FEMA/Ready.gov, NHS, MedlinePlus) → reinforces "no hallucination."
  - **Prompt-injection defense via structural markers** (→ GEMMA §10).
  - **i18n:** 20 UI languages incl. **RTL**, device-locale auto-detect + manual override → our
    Bangla-first bilingual UI.
  - **Crisis UX:** large buttons, high contrast; **WCAG AA** accessibility.
- **License:** verify on repo; adopt patterns.

### trij — `Mosss-OS/trij`  *(offline medical triage, Apache-2.0)*
Vite/React PWA + WebLLM/Ollama Gemma 4; Dexie/IndexedDB; **AES-GCM encrypted** records.
- **Borrow:**
  - **Image-quality validation** (blur/exposure/resolution + low-light preprocessing) before
    inference (→ GEMMA §9.1) — critical for real flood/injury photos.
  - **Green/Yellow/Red triage colors** (→ our tier→color map).
  - **Encrypted local records (AES-GCM)** (→ PRD §11 privacy).
  - Modular assessment (`red-flags.ts`, `vital-signs.ts`, `maternal.ts`); saliency explainability
    (stretch).
- **License:** **Apache-2.0** → reusable with attribution.

### AI-CareCompanion — `narender-rk10/AI-CareCompanion-Offline-Health-By-Gemma`  *(first aid)*
RN + **`llama.rn`**, **Gemma 4 4B GGUF ~1.5 GB**, SQLite, biometric gate.
- **Borrow:** First-Aid Guide screen + "Triage Assistant" agent routing, markdown rendering,
  local-first SQLite, drug-info lookup. Services: `LocalLlmService`, `AgentService`,
  `DatabaseService`, `DrugInfoService`.
- **⚠️ License:** **proprietary — reference only.** *Do not copy code.*
- **⚠️ Rule conflict:** uses a **Gemini cloud fallback** — **we must NOT** (Gemma is the only
  allowed LLM). Strip that pattern.

### offlineaid — `helenkwok/offlineaid`  *(offline disaster app + knowledge packs)*
Expo/RN + TS, **SQLite + FTS5**, **LiteRT** via custom native modules; dev build required.
- **Borrow:** **SQLite-FTS5 knowledge-pack** design → our **keyword-RAG fallback** behind the
  vector index; offline-first architecture; native-module approach if ever on RN.
- **⚠️ License:** **GPL-3.0-or-later** — copying code forces GPL. **Learn architecture; don't
  paste code** (we ship Apache-2.0).

---

## 3. Not a clean reference (rule conflict)

### floodpulse-nairobi — `Mitchell-Odili/floodpulse-nairobi`
Multi-agent flood system using **Gemma 4 31B + Gemini (cloud, Google ADK)** — **cloud-dependent
and uses Gemini**, so **not usable as-is** under the "only Gemma / offline" constraints.
- **Idea worth stealing (concept only):** "**safe-ridge / high-ground** logic" — prefer
  high-ground evacuation zones from terrain → we encode as `on_high_ground` in shelter ranking
  (GEMMA §5.2). Also: context-setter pattern (persist user lat/lon in session state).

---

## 4. Papers (GIS component)

### ⭐ GIS Copilot — arXiv **2411.03205**  *(primary GIS reference)*
- **Idea we implement:** an **"informed agent" documented with GIS tools/parameters** that
  **selects tools + generates the workflow**; the **LLM does not do the geometry**. Evaluated
  basic/intermediate/advanced; strong on basic/intermediate tool selection.
- **Application:** confirms F4 — Gemma **picks** among 3–4 tools; code executes. Scoping to 3–4
  tools matches the paper's "basic/intermediate = high success" finding. Journal: *Int. J.
  Digital Earth* (2025). **likas is our concrete, code-level instantiation** — follow likas to
  build, cite the paper for the concept.

### Autonomous GIS — arXiv **2305.06453**
LLM as the **reasoning core** of GIS. Justifies "Gemma as decision core" framing in the writeup.

### GIScience Research Agenda — arXiv **2503.23633**
Vision for autonomous GIS. Use for **future-work / roadmap** language.

---

## 5. On-device Gemma 4 — key technical facts (with sources)

- **Gemma 4** released 2026-04-02, Apache-2.0; variants **E2B, E4B**, 12B, 26B MoE, 31B Dense;
  edge variants run **fully offline**, **multimodal** (image/video/audio in), **function-calling
  + structured JSON**, **128K context**, **140+ languages** (incl. Bangla).
- **LiteRT-LM** = production on-device runtime (Kotlin/Swift/C++/JS/Python; Flutter community).
  Gemma-4-E2B `.litertlm` ≈ 1.4–2.6 GB, E4B ≈ 3.7 GB; **function calling w/ constrained decoding
  + streaming tool tokens**; vision/audio.
- **MediaPipe LLM Inference** (`com.google.mediapipe:tasks-genai:0.10.27`) is **maintenance-only**;
  vision via `EnableVisionModality` (≤10 images), audio via `AudioModelOptions`
  (`EnableAudioModality`, mono wav); GPU backend; unreliable on emulators.
- **AI Edge Gallery** (Apache-2.0, Kotlin/Compose) — forkable working on-device Gemma app.
- **Embeddings:** MiniLM-L6-v2 (384-dim ONNX) — a **non-LLM supporting model**, allowed by the
  rules; used only for retrieval, never generation.
- **Mesh:** **Nearby Connections API** = free, fully-offline P2P (`P2P_CLUSTER`). **Bridgefy** =
  commercial BLE mesh SDK (~100 m/hop, multi-hop, 12M users).

---

## 6. Source URLs (verified during research)

**Gemma / runtime**
- https://blog.google/innovation-and-ai/technology/developers-tools/gemma-4/
- https://ai.google.dev/gemma/docs/releases
- https://developers.google.com/edge/litert-lm/overview
- https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android
- https://github.com/google-ai-edge/gallery

**Reference repos (verified real via fetch)**
- https://github.com/RaccoonOnion/ash
- https://github.com/JpCurada/likas
- https://github.com/huier5635-cmd/resilience-copilot-gemma4
- https://github.com/wimi321/Beacon
- https://github.com/Mosss-OS/trij
- https://github.com/Mitchell-Odili/floodpulse-nairobi
- https://github.com/helenkwok/offlineaid
- https://github.com/narender-rk10/AI-CareCompanion-Offline-Health-By-Gemma

**Papers**
- https://arxiv.org/abs/2411.03205  (GIS Copilot)
- https://arxiv.org/abs/2305.06453  (Autonomous GIS)
- https://arxiv.org/abs/2503.23633  (GIScience Research Agenda)

**Mesh**
- https://android-developers.googleblog.com/2017/07/announcing-nearby-connections-20-fully.html
- https://bridgefy.me/

> Remaining brief repos not individually deep-read: `CompleteTech-LLC-AI-Research/gemma4good`,
> `Aqta-ai/bounds`, `couzip/sonae`, `TaylorAmarelTech/gemma4_comp`, and the pure-medical list —
> confirm links + licenses before citing.
