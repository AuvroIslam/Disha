# Disha — Build Plan, Demo Script & Writeup Outline

> Companion to [PRD.md](PRD.md). Execution plan for the short hackathon runway.
> **Golden rule:** get one **end-to-end offline path working on a real phone early**, then
> widen. A working demo of 4 tight features scores far higher than 7 broken ones.

---

## 1. Pre-flight (decisions already LOCKED — just execute) — ~1–2 hrs

- [x] **Stack locked:** Kotlin + Compose + **LiteRT-LM**, fork `google-ai-edge/gallery`;
      **Gemma 4 E4B** (E2B hot-swap). *(PRD §14)*
- [x] **RAG locked:** MiniLM ONNX vector RAG + citations; FTS5 fallback.
- [x] **Geodata model locked:** downloadable **per-district region packs** (static base + dynamic
      flood overlay), not whole-country/not hardcoded (PRD F4.1). **Demo pack:** a currently-
      flooding district (Jul 2026) — Cox's Bazar / Chattogram (or Feni). **Language:** Bangla-first
      bilingual. **Voice:** Should-have.
- [ ] Get a **real flagship test phone** (Pixel 8 / Galaxy S23+ class). Emulators are unreliable.
- [ ] **Fork `google-ai-edge/gallery`** and get its on-device Gemma chat running on the phone
      (validates model + runtime immediately).
- [ ] Download **Gemma 4 E4B** (`.litertlm`); keep **E2B** ready for hot-swap.
- [ ] **Region pack (demo district):** export OSM pedestrian graph + POIs; hand-make
      `shelters.geojson` (incl. `on_high_ground`), `facilities.geojson`, and a versioned
      `flood_zones.geojson` overlay; build local MapLibre MBTiles for that one district. Wire the
      "Download your area" catalog screen (one pack populated for the demo).
- [ ] **Knowledge packs:** author 10–20 first-aid packs (EN+BN); run the build-time
      `rag_preprocessor` → MiniLM embeddings → vector index (ash pattern).
- [ ] Repo hygiene: **Apache-2.0 LICENSE**, README skeleton, `.gitignore` (exclude model blob),
      attribution notes for MIT/Apache references used.

---

## 2. Three-day sprint (adjust to real remaining time)

### Day 1 — Spine: on-device Gemma + data + one real cited answer
- [ ] App shell + role switch (Affected / Volunteer / Coordinator), **crisis UX** (large buttons,
      high contrast), Bangla-first strings scaffold.
- [ ] Gemma runtime wrapper (single warm `LlmInference`, per-task sessions, temp/maxTokens,
      **KV-cache trim**, **E2B/E4B hot-swap** stub).
- [ ] Encrypted SQLite schema (PRD §10) + **vector index** loaded with knowledge-pack chunks.
- [ ] **F2 First Aid RAG:** embed query → vector search → Gemma grounded answer → **citations
      `[1][2]`** + disclaimer + red-flag banner. FTS5 fallback works.
- [ ] Airplane-mode smoke test on the phone. ✅ *Milestone: offline cited answer on device.*

### Day 2 — Reasoning + tools: Triage, GIS, Summary
- [ ] **F3 Triage:** system prompt + `TriageResult` JSON + validator + **deterministic fallback**;
      priority-sorted **color-coded** queue. Test on 8 scripted SOS cases.
- [ ] **F4 GIS:** 4 tools (haversine+weighted rank w/ high-ground boost, Dijkstra safe-route,
      flooded filter, nearby facilities); Gemma emits tool call (constrained) → execute → phrase →
      **MapLibre overlay** (offline).
- [ ] **F5 Summary:** compute counts in code → Gemma phrases fixed-section briefing.
- [ ] Safety sidecar around all Gemma calls (**prompt-injection defense**, red-flag banner,
      no-invention checks). ✅ *Milestone: reasoning + tools working offline.*

### Day 3 — Mesh, multimodal, polish, demo
- [ ] **F6 Mesh:** Nearby Connections `P2P_CLUSTER` — SOS phone→phone; received SOS → auto-triage
      → queue. *(Stretch: multi-hop flooding, TTL+dedupe, 3 phones.)*
- [ ] **F1 multimodal:** add image input + **image-quality gate** (blur/low-light) to chat/triage
      (Gallery Ask-Image plumbing); *(Should)* audio input + **streaming TTS** read-back.
- [ ] Finish Bangla strings; empty/error states; loading/streaming polish; WCAG AA pass.
- [ ] **Freeze features.** Write README (install/usage/deps/config). Record the demo video.
- [ ] **Rules audit:** no LLM other than Gemma 4 (MiniLM is embeddings, allowed); no Gemini.
      ✅ *Milestone: submission-ready.*

---

## 3. Team roles (locked; parallelize)

| Role | Owns |
| --- | --- |
| **AI/Gemma lead** | Runtime wrapper, prompts, tool-calling, triage/summary, safety sidecar, RAG generation |
| **Mobile/UI lead** | Compose screens, role switch, chat, queue, map view, crisis UX, i18n, polish |
| **Geo/Data lead** | Sunamganj geodata, pedestrian graph, GIS tool math, MapLibre tiles, knowledge packs + embeddings |
| **Mesh/Integration + Demo lead** | Nearby Connections, wiring, device testing, README + demo video + writeup |

*(Collapse to fit team size; AI + Mobile are the two indispensable tracks.)*

---

## 4. Demo script (record it — video demo is accepted)

Shoot on a **real phone in airplane mode**. ~3 minutes.

1. **Hook (15s):** "No signal, floodwater rising. Disha runs 100% offline — Gemma 4 on the
   phone." Toggle airplane mode ON on camera.
2. **Affected person (40s):** photograph rising water → ask in Bangla "should I leave?" → Gemma
   multimodal advice. Tap **Find safe shelter** → map shows nearest **high-ground** shelter +
   route avoiding flooded roads.
3. **First aid (25s):** "heavy bleeding from leg" → grounded ordered steps with **citation chips**
   + red-flag banner + disclaimer. Tap `[1]` → opens the source passage.
4. **SOS over mesh (30s):** send SOS from Phone A → appears on Phone B (both offline).
   *(Stretch: relays to Phone C.)*
5. **Volunteer triage (30s):** the received SOS auto-triages; queue shows the pregnant-on-rooftop
   case at 🔴 **Critical** with the one-line reason.
6. **Coordinator (25s):** open briefing — counts, top-5, shortages, shelter pressure, blocked
   areas — generated by Gemma from local reports.
7. **Close (15s):** impact line + "Gemma 4 is the reasoning core: multimodal + tool-calling +
   cited RAG + triage + summaries, all offline."

Use a **known-good device pair** with pre-loaded data; don't improvise live.

---

## 5. Kaggle writeup outline (≤ 1,500 words)

1. **Problem & why it matters** (Bangladesh floods, connectivity collapse) — ~180w.
2. **Solution overview** (Disha, offline, on-device Gemma 4 as decision core) — ~180w.
3. **How Gemma 4 is integrated** *(most words — 30 pts)* — ~350w: multimodal understanding,
   **function/tool-calling** for GIS, **structured-JSON triage**, **grounded cited RAG** for first
   aid, summaries; include the **"Gemma does / code does" table** (GEMMA §12) to prove Gemma is
   central while math/transport/**embeddings** stay deterministic (also proves the "only Gemma"
   rule — MiniLM is a supporting model).
4. **System architecture** *(PRD §7 diagram)* — ~200w.
5. **Technical challenges & solutions** — ~200w: on-device memory/latency (E4B↔E2B hot-swap, GPU,
   KV-cache trim), constrained decoding + deterministic fallbacks, offline vector RAG with
   citations, offline mesh (Nearby Connections + flooding), offline routing (Dijkstra over OSM).
6. **Real-world impact** — ~150w: faster triage, safer cited first aid, coordination without
   signal, 170M flood-exposed population.
7. **Future improvements** — ~120w: Bangla fine-tune (Unsloth) of E2B, image flood-depth/injury
   estimation, true mesh protocol, real NDMA/BWDB data, PHI redaction for shared summaries.

**Framing lines to reuse:**
- "Gemma 4 is not answering questions — it **reasons over text, voice, images, and location and
  calls tools** to coordinate disaster response."
- "Everything runs **on-device and offline**, with **cited, grounded** answers; the app is
  useless without Gemma — which is the point."

---

## 6. Submission checklist (from compDescrip.md)

- [ ] Kaggle Writeup (≤1,500 words, all required sections) — **submitted, not draft**.
- [ ] Public GitHub repo — source, README, install, usage, deps, config; **stays public**.
- [ ] Working demo — public, no auth, no paywall (hosted app *or* recorded video).
- [ ] Google Form completed after Kaggle submission.
- [ ] **Rules audit:** Gemma 4 is the primary and **only** LLM; MiniLM embeddings + ML/CV/OCR/DB
      are supporting (allowed); **no Gemini** anywhere.
- [ ] Attributions for reused code: Apache-2.0 (gallery, ash, trij) + MIT (resilience-copilot) OK
      with notice; **avoid GPL copy** (offlineaid); **AI-CareCompanion is reference-only**.

---

## 7. Definition of Done (MVP)

- On a real phone in **airplane mode**: F1 (text+image chat), F2 (**cited** first aid), F3
  (color triage queue), F4 (nearest high-ground shelter + flooded-road avoidance on offline map),
  F5 (coordinator summary), F6-lite (SOS phone→phone) all work.
- No crashes on invalid model output (fallbacks fire). Sensitive data encrypted at rest.
- No LLM other than Gemma 4. Repo public with complete README. Demo recorded.
