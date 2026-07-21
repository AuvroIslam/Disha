# Disha for Android

**Disha** is an offline-first disaster-response companion for Bangladesh. It runs
**Gemma 4 E2B** locally through LiteRT-LM and is the Android product for this repository.
Gemma 4 is the only LLM used by the app.

## What It Does

- **Rescue triage:** Gemma produces structured urgency assessments for SOS reports, with a
	deterministic fallback when output is invalid or the model is unavailable.
- **First aid:** Gemma writes grounded, cited guidance from the bundled WHO, IFRC, and Red Cross
	knowledge packs. Life-threatening queries show a red-flag warning and every response includes
	a medical disclaimer.
- **Safe shelter:** Gemma selects a GIS tool; deterministic code finds shelters, routes around
	illustrative flood zones, and locates nearby facilities using offline data.
- **Coordinator summary:** The app counts trusted reports deterministically, then Gemma turns
	those facts into a concise operational briefing.
- **Mesh SOS:** Devices exchange Ed25519-signed SOS reports over Nearby Connections. Reports that
	fail verification are quarantined and excluded from summaries.
- **Bangla and English:** A single language setting controls the interface and model instructions.

## Architecture

```
Text / photo / GPS
				|
				v
Gemma 4 E2B on-device (LiteRT-LM)
				|
				v
Kotlin reasoning core: triage, RAG, GIS, safety, summaries, mesh
				|
				v
Offline first-aid, shelter, district, and road data
```

Gemma handles language, reasoning, structured triage, tool selection, and summaries. Geometry,
retrieval, report counts, cryptography, and transport remain deterministic and testable.

## Requirements

- Android Studio with its bundled JDK 21 recommended
- Android 12 (API 31) or later
- A physical Android phone with at least 6 GB RAM; emulators are not suitable for the Gemma model
- Internet access only for the initial model download

The onboarding flow downloads
[`gemma-4-E2B-it.litertlm`](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm)
(approximately 2.5 GB). Once installed, model inference and the core disaster workflow operate
offline.

## Build and Run

1. Open `disha-android/` in Android Studio and allow Gradle to sync.
2. Connect a supported physical device and run the `app` configuration.
3. Accept the model-download consent during onboarding, then use **Flood drill** to explore the
	 complete Disha workflow with sample reports.

From a terminal in this directory:

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## Data and Safety

- First-aid packs are grounded in WHO, IFRC, and Red Cross guidance.
- Roads are derived from OpenStreetMap data; shelter data comes from HOT-OSM education-facility
	data. Flood overlays in the detailed packs are explicitly illustrative scenarios, not live
	flood intelligence.
- Disha is not a medical authority. Its first-aid guidance supports, but never replaces,
	professional care and emergency services.
- Inference stays on-device. The app stores report and chat data locally, and the user can clear
	stored content or remove the downloaded model in Settings.

## Release Preparation

This module currently uses the placeholder application ID `com.example.gemmachat`. Replace it
with the final package name before public distribution. Follow
[`RELEASE_CHECKLIST.md`](RELEASE_CHECKLIST.md) to configure a signing key, build a release APK,
and complete real-device checks.

## Credits

- Gemma 4 by [Google DeepMind](https://deepmind.google/models/gemma/)
- LiteRT-LM by [Google AI Edge](https://github.com/google-ai-edge/LiteRT-LM)
- The Android project began from the Apache-2.0
	[amrrs/gemmachat-android](https://github.com/amrrs/gemmachat-android) starter
