# Disha — Product Requirements Document (PRD)

> **Disha** (দিশা — *"direction / guidance"*): an offline, **Gemma 4–powered** AI
> disaster-response companion for Bangladesh.
> Built for the *Build with Gemma 4* Community Hackathon (Kaggle).
>
> This `prd/` folder is the single source of truth for building the app. Read in order:
> 1. **PRD.md** (this file) — product, scope, features, architecture, data models, stack.
> 2. **[GEMMA_INTEGRATION.md](GEMMA_INTEGRATION.md)** — on-device Gemma 4 setup, RAG, tool-calling, prompts.
> 3. **[REFERENCE_NOTES.md](REFERENCE_NOTES.md)** — what each reference repo/paper gives us + licenses + sources.
> 4. **[BUILD_PLAN.md](BUILD_PLAN.md)** — 3-day sprint, demo script, Kaggle writeup outline.

---

## 0. Document Control

| Field | Value |
| --- | --- |
| Product | **Disha** (দিশা = "direction/guidance") |
| Version | PRD v2.0 (decisions locked; architecture upgraded from 9 reference repos) |
| Date | 2026-07-15 |
| Target platform | **Android** (primary), on-device |
| Primary AI model | **Gemma 4 E4B** (E2B hot-swap fallback) — the *only* LLM used, per competition rules |
| App framework | **Kotlin + Jetpack Compose**, fork of `google-ai-edge/gallery` |
| Inference runtime | **LiteRT-LM** (Kotlin) |
| License intent | **Apache-2.0** (repo public during judging) |
| Status | Locked for build kickoff |

---

## 1. Executive Summary

During floods and cyclones in Bangladesh, mobile networks fail exactly when coordination
matters most. **Disha** is an offline-first Android app in which **Gemma 4 runs entirely
on-device** and acts as the reasoning engine across four jobs that normally require
connectivity and experts:

1. **Guide** — multimodal (text / voice / image) emergency + first-aid advice, grounded in an
   offline knowledge base **with citations** so nothing is invented.
2. **Triage** — score incoming SOS reports by urgency via structured reasoning (JSON).
3. **Navigate** — nearest safe shelter / hospital and flooded-road avoidance via an
   *LLM-that-calls-GIS-tools* pattern.
4. **Summarize** — turn a flood of reports into a coordinator briefing.

Devices exchange SOS/messages **without internet** over a Bluetooth/Wi-Fi peer-to-peer mesh.

**Why this wins the rubric.** Gemma 4 is the decision core, not a bolt-on chatbot. It reasons
over *multiple modalities* (text, voice, image, location) and *calls tools* (GIS, knowledge
base) — directly maximizing the 30-pt "Gemma Integration" criterion — while offline disaster
response for a flood-prone country of 170M is a strong Innovation & Impact story.

---

## 2. Problem Statement & Why It Matters

Bangladesh is among the most flood- and cyclone-exposed countries on earth. In major disasters
cellular/internet infrastructure is damaged or overloaded for days.

- **Affected people** can't reach help, can't share location accurately, and don't know what to
  do medically while waiting.
- **Volunteers** are ordinary citizens, not medics; they can't tell which victims are most
  urgent, don't know safe routes, and get flooded with unstructured SOS requests.
- **Coordinators** drown in hundreds of photos/voice/text reports with no overview to allocate
  scarce boats, medics, and shelter space.

**Consequence:** slower response, misallocated resources, unsafe first aid, preventable deaths.
**Existing apps assume connectivity — Disha does not.**

---

## 3. Product Vision & Positioning

> **"An offline AI companion that turns any Android phone into a disaster-response node —
> guiding, triaging, routing, and coordinating without a single bar of signal."**

- **Not** a generic chatbot — Gemma reasons over multimodal inputs and calls tools.
- **Not** cloud-dependent — all inference on-device; comms peer-to-peer.
- **Not** a medical authority — supports, never replaces, professional care; safety-bounded.

| Others in the reference field do | Disha adds |
| --- | --- |
| Offline chat (offlineaid, ash, Beacon) | + multimodal triage + GIS tool-calling + mesh SOS in one app |
| Evacuation only (likas) | + medical triage + first aid + coordinator summary |
| Triage only (resilience-copilot, trij) | + on-device + offline mesh transport + maps |
| First aid only (AI-CareCompanion) | + disaster GIS + prioritization + coordination |

Disha's edge = **breadth (all four jobs in one offline app) + grounded, cited answers +
multimodal Gemma reasoning + a peer-to-peer transport** — no single reference does all of this.

---

## 4. Target Users & Personas

| # | Persona | Role in app | Primary needs |
| --- | --- | --- | --- |
| P1 | **Rahima** — flood-affected resident | *Affected* | Send SOS offline, get first-aid guidance, find nearest safe shelter |
| P2 | **Karim** — community volunteer | *Volunteer* | Prioritized SOS queue, first-aid steps, safe route to victims |
| P3 | **Nadia** — upazila rescue coordinator | *Coordinator* | Live situation summary, shortages, high-priority map |

Role switch on first launch (Affected / Volunteer / Coordinator) changes which surfaces are
foregrounded; the same Gemma engine + data layer serve all three.

---

## 5. Scope & Prioritization (MoSCoW)

> **Reality check:** the runway is short and a *working demo* is required (20 pts + the spine of
> everything). **Build the MVP end-to-end on a real phone first**, then widen.

### 5.1 MVP (Must-have) — the demo spine
- **M1** On-device Gemma 4 multimodal assistant (text + image) — [F1]
- **M2** AI First-Aid, **grounded RAG with citations** — [F2]
- **M3** Rescue triage: structured urgency scoring → priority + reasons — [F3]
- **M4** GIS tool-calling: nearest safe shelter + flooded-road avoidance on an offline map — [F4]
- **M5** Offline SOS send/receive between 2 phones (Nearby Connections) — [F6-lite]
- **M6** Coordinator summary: Gemma briefing over local SOS list — [F5]

### 5.2 Should-have
- **Voice input** (audio → Gemma) + streaming **TTS** read-back
- **Bangla-first bilingual** UI/prompts
- Multi-hop message relay (controlled flooding) across ≥3 devices
- Safe-route line drawn on map (not just nearest point)
- **Image-quality validation** before multimodal inference (blur/low-light)

### 5.3 Could-have (stretch)
- On-device fine-tune of Gemma 4 E2B (Unsloth) on Bangla first-aid/triage data
- Image-based flood-depth / injury-severity as a triage signal
- Shelter capacity crowd-updates propagated over mesh
- Saliency/explainability overlay for image assessments

### 5.4 Won't-have (this hackathon)
- iOS build, real backend server, live NDMA/BWDB data, accounts/auth, production mesh protocol.

---

## 6. Feature Specifications

Each feature: **what**, **user stories**, **Gemma's role**, **acceptance criteria (AC)**, **data**.
Prompts and schemas live in **[GEMMA_INTEGRATION.md](GEMMA_INTEGRATION.md)**.

### F1 — Offline Multimodal AI Assistant  *(MVP: M1)*
**What.** Chat where the user types, speaks, or attaches a photo; Gemma 4 responds with
disaster/safety guidance — fully offline. **Streaming** tokens; **TTS** read-back in voice mode.

**User stories.**
- As Rahima, I photograph rising water at my door and ask "is it safe to stay?" and get advice.
- As Karim, I describe a victim's symptoms and get an assessment and next steps.

**Gemma's role.** Vision + text (+ audio) understanding; system-instructed safety-bounded
disaster assistant; may delegate to F2/F4 tools.

**AC.**
- Works in airplane mode once the model is present on device.
- Accepts text; accepts ≥1 image (**image-quality check** first — see F1.1); streams tokens; renders markdown.
- Voice mode: sentence-level streaming into TTS; system prompt swaps to plain prose (no markdown). *(ash pattern)*
- Refuses/deflects out-of-scope or unsafe requests with a disclaimer.

**F1.1 Image-quality validation (trij pattern).** Before sending a photo to Gemma, run cheap
non-LLM checks (blur/variance, exposure, resolution); if poor, prompt a retake or apply
low-light preprocessing. Prevents garbage-in on real flood/injury photos.

**Data.** Conversation history (encrypted SQLite), attached image (MPImage), optional location.

---

### F2 — AI-Powered First-Aid (Grounded RAG **with citations**)  *(MVP: M2)*
**What.** Step-by-step first-aid guidance grounded in a **bundled offline knowledge base**,
returned with **inline citation chips `[1][2]`** that deep-link to the source passage.

**User stories.**
- As Karim, I ask "someone is bleeding heavily from the leg" and get ordered, safe, *cited* steps.
- As Rahima, I ask what to do for a child who swallowed floodwater.

**Gemma's role.** **Retrieval-augmented generation**: query → semantic vector retrieval →
Gemma composes grounded, ordered steps, **citing only retrieved passages**, and flags when to
seek professional help. Upgraded from plain FTS5 to **embedding RAG** (ash + Beacon pattern).

**AC.**
- Answers derive **only** from retrieved passages; every claim is citable; no invented drug dosages.
- Every medical answer appends the "support, not replace professional care" disclaimer.
- Life-threatening keywords (not breathing, severe bleeding, unconscious) raise a prominent
  "get help now" banner.
- Tapping a citation opens the source passage (deep-link). *(ash "Library Lens" pattern)*

**Data.** Knowledge packs → chunks with embeddings (see §10 + GEMMA §8).

---

### F3 — Intelligent Rescue Triage & Prioritization  *(MVP: M3)*
**What.** Each SOS is scored for urgency; volunteers/coordinators see a **priority-sorted,
color-coded queue** with reasons — not raw chronological messages.

**User stories.**
- As Nadia, the pregnant woman on a rooftop is at the top of the queue, with why.
- As Karim, I see color tiers (🔴 Critical / 🟠 High / 🟡 Moderate / 🟢 Low). *(trij color language)*

**Gemma's role.** Structured reasoning → **JSON output** (constrained decoding). Given SOS text
(+ optional image/audio), Gemma returns a `TriageResult` (priority tier, urgency score, risk
signals, one-line rationale). Adopts the **resilience-copilot** safety-bounded pipeline:
`normalize → detect risk signals → match playbook → Gemma → safety check → JSON`.

**AC.**
- Output validates against the `TriageResult` schema (GEMMA §4).
- **Deterministic rule fallback** if model output fails validation (queue never breaks).
- No diagnosis, no invented facts; unknowns marked unknown; high-risk → `needs_human_review`.
- Reproducible tier on the same input (temperature ≤ 0.4).

**Data.** `sos_report` + `triage_result` (§10).

---

### F4 — GIS-Assisted Rescue (LLM + Tool-Calling)  *(MVP: M4)*
**What.** Natural-language location help — "nearest safe shelter?", "route to the victim
avoiding flooded roads" — answered by Gemma **calling 3–4 GIS tools** over bundled offline
geodata, drawn on an offline map.

**User stories.**
- As Rahima, tap "Find safe shelter" → nearest reachable shelter + walking line.
- As Karim, ask for a route to an SOS pin that avoids flooded segments.

**Gemma's role.** **GIS Copilot / likas pattern**: Gemma does **not** compute geometry. It emits
a **tool call** (constrained-decoded) to one deterministic tool; the app runs the spatial math;
Gemma explains the result in Bangla/English.

**Tools (v1):** `find_nearest_shelter`, `safe_route`, `flooded_roads_near`, `nearby_facilities`
(schemas in GEMMA §5). Shelter ranking prefers **high-ground** shelters (`on_high_ground`,
floodpulse "safe-ridge" idea).

**AC.**
- All tools run offline over bundled GeoJSON + local tiles.
- Nearest-shelter ranking is **profile-aware** (elderly/PWD/pet/capacity), likas weighting.
- Map renders offline (MapLibre); geometry overlaid.
- Invalid/unknown tool call → deterministic keyword fallback.

**Data.** `shelters/flood_zones/facilities.geojson`, precomputed `ped_graph` (§10).

#### F4.1 — Offline map-data strategy: downloadable **region packs**  *(architecture decision)*
The app does **not** ship the whole Bangladesh map (too big — country-wide offline vector maps +
routing graph ≈ 1–2+ GB on top of the 1.4–3.7 GB model) and does **not** hardcode one area. It
uses **per-district region packs** the user downloads for where they live (offline-maps UX;
`offlineaid-pack-builder` pattern). Split static from dynamic:

- **Static region pack** (download once, ~tens of MB/district): base **vector map tiles (MBTiles)**
  + **pedestrian graph** + `shelters.geojson` + `facilities.geojson`. Rarely changes.
- **Dynamic flood overlay** (small, per event): current `flood_zones.geojson`. Refreshed from
  **FFWC/BWDB when online**, or **propagated over the mesh offline** (unique offline capability).

**UX:** first launch → "Download your area" screen (district list) → pack cached locally →
everything else works fully offline. Ship **one populated pack** for the demo; show the catalog
screen as the product vision.

**Data.** `region_pack` manifest `{district, version, tiles_uri, graph_uri, geojson_uris,
size_mb}`; flood overlays versioned separately.

---

### F5 — AI Disaster Summary (Coordinator)  *(MVP: M6)*
**What.** Gemma turns the local SOS + triage records into a concise **situation briefing**.

**Gemma's role.** Summarization over structured records into fixed sections; **counts computed
in code**, Gemma only phrases them (prevents miscounting). (GEMMA §6.)

**AC.**
- Fixed sections: total/new SOS, Critical/High counts, top-5 cases, shortages, shelter pressure,
  blocked areas, recommended focus.
- No invented numbers/facts. Regenerates as new SOS arrive over mesh.

**Data.** Aggregation over `sos_report` + `triage_result` + `shelters`.

---

### F6 — Offline Mesh Communication  *(MVP: M5; Should: multi-hop)*
**What.** Send/receive SOS + short messages **without internet**, phone-to-phone, with optional
multi-hop relay.

**Implementation.** **Google Nearby Connections API** (`P2P_CLUSTER`) — fully offline
(BLE + Bluetooth + Wi-Fi Direct/hotspot). Multi-hop via app-level **controlled flooding**
(dedupe by `msg_id` + TTL). Bridgefy SDK = commercial fallback. (GEMMA §7.)
Design hardened from **MeshGemma**: layer transports (Wi-Fi/mDNS + BT/Wi-Fi-Direct + BLE beacon)
if time allows; a single signed-envelope handler with per-transport dedup.

**F6.1 — Signed SOS envelopes (anti-misinformation).** Wrap each report in an **Ed25519-signed
envelope** over a canonical (sorted-key) serialization; peers **verify before trusting**; a
**Lamport clock** prevents replay/counter-reuse. Implemented + tested in `core/mesh.py`
(`SignedEnvelope`, `MeshNode`, `Ed25519Signer`/`DevSigner`). TTL is relay metadata, *not* signed.

**F6.2 — Gemma radio-uplink compression.** When even the mesh can't reach responders, Gemma
compresses up to ~50 incidents into a **≤200-byte JSON digest** for ham/SMS/satellite uplink,
with a deterministic builder that guarantees the size bound. Implemented + tested in
`core/compress.py` (`compress_for_radio`). *(A novel, judge-friendly Gemma use.)*

**AC.**
- Two devices discover + connect offline and exchange a **signed** SOS envelope (§10).
- Forged/tampered envelopes fail verification and are dropped; dedupe by `msg_id`; TTL bounds relay.
- Received SOS enters the queue and is auto-triaged by F3.
- Radio digest always ≤ 200 bytes, even on malformed model output.

**Data.** `SignedEnvelope` wrapping `sos_report` (§10). Payloads **encrypted at rest** (§11).

---

## 7. System Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                     Disha  (Android, on-device)                        │
│                                                                        │
│  ┌───────────────┐   INPUTS                                            │
│  │  UI (Compose) │  text · voice(wav) · image(quality-checked) · GPS   │
│  │  role-based   │  crisis UX: big buttons, high contrast, BN-first    │
│  └───────┬───────┘                                                    │
│          ▼                                                            │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │        ORCHESTRATOR  (intent router + SAFETY SIDECAR)           │  │
│  │  prompt-injection defense · red-flag detect · no-invention check │  │
│  │  routes to: Assistant · FirstAid(RAG) · Triage · GIS · Summary   │  │
│  └───┬───────────┬──────────────┬───────────────┬────────────┬─────┘  │
│      ▼           ▼              ▼               ▼            ▼         │
│  ┌────────┐ ┌──────────┐  ┌───────────┐  ┌────────────┐ ┌─────────┐   │
│  │ GEMMA 4│ │ FirstAid │  │  Triage   │  │  GIS tools │ │ Summary │   │
│  │ E4B    │ │ VECTOR   │  │ (JSON +   │  │ nearest·   │ │(counts+ │   │
│  │(LiteRT-│ │ RAG +    │  │  rules    │  │ route·     │ │ Gemma)  │   │
│  │  LM)   │ │ citations│  │ fallback) │  │ flooded·   │ │         │   │
│  │  ▲     │ │(MiniLM   │  └─────┬─────┘  │ facilities)│ └────┬────┘   │
│  │  │     │ │ ONNX+HNSW)│        │        └─────┬──────┘      │        │
│  └──┼─────┘ └────┬─────┘        │              │             │        │
│     │            ▼              ▼              ▼             ▼         │
│  ┌──┴───────────────────────────────────────────────────────────┐    │
│  │        DATA LAYER  (encrypted SQLite + vector index, assets)  │    │
│  │  conversations · sos_report · triage_result · kb_chunks(+emb) │    │
│  │  shelters/flood/facilities GeoJSON · ped_graph · map tiles    │    │
│  └───────────────────────────────┬──────────────────────────────┘    │
│                                   ▼                                   │
│  ┌────────────────────────────────────────────────────────────┐      │
│  │   MESH TRANSPORT (Nearby Connections, offline P2P + relay)  │      │
│  └────────────────────────────────────────────────────────────┘      │
└──────────────────────────────────────────────────────────────────────┘
         ▲ discover/connect/relay ▼          (no internet required)
   ┌───────────┐   ┌───────────┐   ┌───────────┐
   │  Phone A  │──▶│  Phone B  │──▶│  Phone C  │  (multi-hop flooding)
   └───────────┘   └───────────┘   └───────────┘
```

**Layers**
- **UI (Jetpack Compose)** — role-based screens; **crisis-optimized** (large buttons, high
  contrast, WCAG AA, Bangla-first, i18n-ready — Beacon pattern); offline MapLibre map.
- **Orchestrator + Safety Sidecar** — deterministic guards around Gemma: prompt-injection
  defense (structural markers), red-flag detection, no-invention checks, human-review triggers
  (resilience-copilot + Beacon patterns).
- **Gemma runtime** — Gemma 4 E4B via **LiteRT-LM** (Kotlin); single warm instance, per-task
  sessions, **E2B↔E4B hot-swap**, **KV-cache management** (ash pattern).
- **Feature engines** — Vector-RAG first aid, Triage, GIS tools, Summary.
- **Data layer** — encrypted SQLite + on-device vector index; bundled GeoJSON, ped graph, tiles.
- **Mesh transport** — Nearby Connections; controlled flooding for multi-hop.

Per-engine request/response flows: **GEMMA_INTEGRATION.md**.

---

## 8. Tech Stack — **LOCKED**

> Decisions are locked (§14). A fallback stack is noted only for risk transparency.

### 8.1 Locked stack

| Concern | Choice | Why |
| --- | --- | --- |
| App framework | **Kotlin + Jetpack Compose (native Android)** | Best-supported on-device Gemma 4 multimodal; target users are Android |
| Fork base | **`google-ai-edge/gallery`** (Apache-2.0) | A *working* on-device Gemma app: download, session, chat, **Ask-Image**, **Audio-Scribe** — de-risks the hardest plumbing |
| Gemma runtime | **LiteRT-LM (Kotlin)**, Gemma 4 **E4B** `.litertlm` (E2B hot-swap) | Production runtime; native image+audio + **function-calling with constrained decoding** |
| First-aid RAG | **MiniLM-L6-v2 ONNX (384-dim) embeddings + on-device vector index** (ObjectBox HNSW or `sqlite-vec`), FTS5 as lightweight fallback | Semantic retrieval + **citations**; grounded, no hallucination (ash/Beacon) |
| Offline maps | **MapLibre Android SDK** + local vector tiles (MBTiles), **per-district region packs** | Fully offline; user downloads only their area (not whole country); proven by likas |
| Geodata | GeoJSON (shelters/facilities + versioned flood overlay) + precomputed OSM pedestrian graph, packaged per district | Small downloads; static base + dynamic flood overlay (F4.1) |
| Local DB | **SQLite (Room), encrypted (SQLCipher / AES-GCM fields)** | Offline store; privacy (trij) |
| Mesh | **Google Nearby Connections API** (`P2P_CLUSTER`) + app-level flooding | Free, offline, Google-native P2P |
| Voice | Gemma 4 audio input (mono wav) + Android TTS read-back | Multimodal Gemma differentiator |
| Embedding model | MiniLM-L6-v2 (ONNX) — *non-LLM supporting model* | Allowed by rules (not an LLM) |

### 8.2 Fallback stack (only if the team truly cannot do Android-native)
React Native + `llama.rn` (Gemma 4 **E2B GGUF Q4_K_M**, GBNF tool calls) — proven by
**likas** + **AI-CareCompanion**; **but** multimodal (image/audio) is harder → text/tool-first,
voice/vision as stretch. *Do not split effort across both stacks.*

### 8.3 Hard constraints from competition rules
- **Gemma 4 is the ONLY LLM/generative foundation model.** No Gemini fallback anywhere (strip
  the AI-CareCompanion/floodpulse pattern). Supporting non-LLM ML/CV/OCR/speech, DBs, vector
  search, embeddings (MiniLM), and APIs are allowed.
- Public repo (README + install + usage + deps + config), public working demo (recorded video ok).

---

## 9. On-Device Gemma 4 — Summary (full detail in GEMMA_INTEGRATION.md)

- **Model:** Gemma 4 **E4B** default (native image+audio, function-calling, structured JSON,
  128K context, 140+ languages incl. Bangla, Apache-2.0); **E2B** hot-swap for low-RAM devices.
- **Runtime:** LiteRT-LM (`.litertlm`; E2B ≈ 1.4–2.6 GB, E4B ≈ 3.7 GB). MediaPipe `tasks-genai`
  is maintenance-only — use LiteRT-LM.
- **Capabilities used:** multimodal chat, constrained-decoding **tool calls** (GIS + triage
  JSON), **grounded vector RAG with citations** (first aid), structured summary.
- **Device target:** high-end Android (Pixel 8 / Galaxy S23+ class), GPU backend. **Demo on a
  real flagship** (emulators unreliable).
- **Delivery:** first-run download over Wi-Fi to app storage (too large for the APK), with
  checksum verify + progress UI.

---

## 10. Data Models (canonical)

```jsonc
// sos_report  (created locally or received via mesh; sensitive fields encrypted at rest)
{ "msg_id":"uuid", "created_at":"iso8601", "reporter_role":"affected|volunteer",
  "text":"string", "audio_path":"string|null", "image_path":"string|null",
  "lat":23.81, "lon":90.41, "loc_accuracy_m":30, "people_count":1,
  "flags":["pregnant","child","elderly","chronic","trapped"],
  "status":"new|triaged|acknowledged|resolved", "origin_device":"id", "hops":0 }

// triage_result  (F3 output; validates against schema in GEMMA §4)
{ "msg_id":"uuid", "priority":"critical|high|moderate|low", "urgency_score":0.0,
  "risk_signals":["heavy_bleeding","rising_water"], "needs_human_review":true,
  "rationale":"one line", "recommended_action":"one line",
  "model":"gemma-4-e4b", "produced_by":"gemma|fallback_rules", "created_at":"iso8601" }

// kb_chunk  (first-aid RAG; vector-indexed)  ← upgraded from flat FTS5 rows
{ "id":"cpr_003", "pack":"severe_bleeding", "hazard":"bleeding", "lang":"en|bn",
  "text_md":"Apply firm direct pressure...", "embedding":[/*384 floats*/],
  "source":"IFRC/WHO", "citation_label":"[1]" }
// (optional FTS5 mirror for keyword fallback)

// shelters.geojson (Feature.properties)
{ "id":"sh_012", "name":"Govt Primary School", "capacity":300, "occupancy":120,
  "has_pwd_access":true, "allows_pets":false, "has_medical":true, "on_high_ground":true }

// flood_zones.geojson → polygons; ped_graph edges intersecting them are excluded by safe_route
// facilities.geojson  → {id,name,type:"hospital|relief|clinic",...}

// MeshPacket (transport envelope)
{ "msg_id":"uuid", "ttl":4, "type":"sos|ack|summary_req", "payload":{...}, "sig":null }
```

---

## 11. Safety, Ethics & Privacy  *(non-negotiable)*

From **resilience-copilot** (safety), **Beacon** (defense/UX), **trij** (privacy):

- **Support, not replace.** Every medical answer carries a visible disclaimer; life-threat
  signals raise a "seek professional help now" banner.
- **Grounded + cited.** First-aid answers cite retrieved passages; **no invented** drug dosages,
  shelter capacity, road status, or clinic availability; unknowns labeled unknown.
- **Prompt-injection defense.** Wrap user/retrieved content in structural markers; the system
  prompt instructs Gemma to treat it as data, not instructions. *(Beacon)*
- **No autonomous dispatch.** High-risk cases are *flagged for human review*.
- **Deterministic fallbacks** for triage + GIS so a bad model output never breaks safety flows.
- **Privacy.** Data stays on-device / within the local mesh; **sensitive fields encrypted
  (AES-GCM / SQLCipher)**; no cloud upload. PHI redaction for shared summaries is a stretch
  (Aqta-ai/bounds).
- **Low temperature** (≤ 0.4) for triage/GIS/summary; reproducible with fixed seed for demos.

---

## 12. Success Metrics

**Demo/judging (primary):**
- All 6 MVP features demonstrable offline on one flagship (airplane mode).
- Triage orders a scripted 8-case set correctly.
- Nearest-shelter + flooded-road avoidance visibly correct on the demo map.
- First-aid answer shows working **citations**; SOS travels phone→phone (+1 relay hop stretch).

**Product (for writeup):**
- Time-to-first-guidance < 10 s offline; triage of an SOS < 5 s; briefing readable < 30 s.

---

## 13. Risks & Mitigations

| Risk | Likelihood | Impact | Mitigation |
| --- | --- | --- | --- |
| Model too slow / OOM on test phone | Med | High | **E2B hot-swap**; GPU backend; cap `maxTokens`; **KV-cache trim**; test on real flagship Day 1 |
| Multimodal (image/audio) eats time | Med | High | Land text end-to-end first; add image (Gallery Ask-Image), then audio |
| Vector-RAG setup slips | Low | Med | Ship FTS5 keyword RAG first, swap embeddings in; citations work either way |
| Mesh multi-hop flaky in demo | Med | Med | Single-hop SOS is MVP; multi-hop is Should; script with known-good devices |
| Model emits invalid tool-call/JSON | Med | Med | Constrained decoding + strict parser + deterministic fallback |
| Bundling Bangladesh geodata heavy | Low | Med | Limit to one demo upazila (Sunamganj); small AOI tiles |
| "Only Gemma" rule violated | Low | High | No Gemini/other LLM; MiniLM (embeddings) is not an LLM; audit deps pre-submit |
| Model download too big for judges | Med | Low | Recorded video demo accepted; download-on-first-run + docs |

---

## 14. Locked Decisions

1. **Framework/runtime:** **Kotlin + Jetpack Compose + LiteRT-LM**, forking
   `google-ai-edge/gallery`. Model **Gemma 4 E4B** (E2B hot-swap). ✅
2. **First-aid RAG:** **semantic vector RAG (MiniLM ONNX + vector index) with citations**;
   FTS5 keyword fallback. ✅
3. **Offline geodata = downloadable per-district region packs** (static base + dynamic flood
   overlay), *not* whole-country and *not* one hardcoded area (F4.1). **Demo pack:** a
   **currently-flooding district (Jul 2026 floods)** — recommend **Cox's Bazar or Chattogram**
   (highest impact; Cox's Bazar adds the Rohingya-camp/no-connectivity angle); **Feni** = simpler
   alternative; **Sunamganj/Sylhet** also currently at risk. Swappable — team picks by OSM data
   quality + local knowledge. ✅
4. **Language:** **Bangla-first, bilingual** (English toggle); i18n-ready. ✅
5. **Voice:** **Should-have** — text + image in MVP; audio + TTS on Day 3. ✅
6. **Team roles:** AI/Gemma · Mobile/UI · Geo/Data · Mesh+Integration/Demo (BUILD_PLAN §3). ✅

*See [GEMMA_INTEGRATION.md](GEMMA_INTEGRATION.md), [REFERENCE_NOTES.md](REFERENCE_NOTES.md),
[BUILD_PLAN.md](BUILD_PLAN.md).*
