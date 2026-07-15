# Disha — Project Brief & Reference Repositories

> An offline, Gemma-powered AI disaster-response companion for Bangladesh.
> This document consolidates the project concept, the role of Gemma, expected impact,
> and a curated set of open-source reference repositories (medical, disaster,
> first-aid, offline-AI) worth studying or borrowing from.

---

## Table of Contents

1. [Overview](#1-overview)
2. [The Problem](#2-the-problem)
3. [Our Solution](#3-our-solution)
4. [Core Features](#4-core-features)
5. [Role of Gemma](#5-role-of-gemma)
6. [Expected Impact](#6-expected-impact)
7. [Reference Repositories](#7-reference-repositories)
8. [Reference Papers](#8-reference-papers)
9. [Recommended Repositories to Combine](#9-recommended-repositories-to-combine)
10. [How Each Reference Maps to Disha](#10-how-each-reference-maps-to-disha)
11. [Suggested Architecture Sketch](#11-suggested-architecture-sketch)
12. [Notes & Next Steps](#12-notes--next-steps)

---

## 1. Overview

**Disha** is an offline, AI-powered disaster-response platform designed for Bangladesh.
It helps affected people, volunteers, and rescue coordinators **communicate, coordinate,
and make informed decisions** during floods and other natural disasters — even when
internet connectivity is unavailable.

Powered by **Gemma**, the application serves as the central intelligence of the system,
analyzing images, voice messages, and text to provide emergency guidance, prioritize
rescue requests, and generate actionable summaries for rescue teams.

Rather than being just another chatbot, Disha functions as an **AI-powered disaster-response
companion** that assists throughout the entire rescue process.

> **Key framing for the submission:** Gemma is *not* just answering questions — it is
> actively **reasoning over multiple inputs** (text, voice, images, and location) to help
> coordinate disaster response. This aligns with the hackathon's emphasis on meaningful
> Gemma integration.

---

## 2. The Problem

During floods and other natural disasters in Bangladesh, communication, coordination,
and emergency response become extremely difficult.

### For affected people
- Internet and mobile networks may become unavailable or unreliable.
- Many people cannot accurately share their location with rescuers.
- People often do not know what to do immediately after an injury or medical emergency
  while waiting for professional help.
- Reaching volunteers or emergency services can be difficult.

### For volunteers
- Most volunteers are ordinary citizens, not trained medical professionals. They want to
  help but may not know correct first-aid procedures or how to safely treat injuries until
  professional help arrives.
- In high-pressure situations, volunteers struggle to determine which victims need
  immediate attention.
- They may not know the safest route to reach affected communities.
- They receive numerous SOS requests but have no efficient way to prioritize them.

### For rescue coordinators
- Rescue teams need timely information about shelters, blocked roads, and affected areas
  to plan operations.
- Hundreds of incoming SOS messages, photos, and voice recordings are difficult to process
  manually.
- Without a clear overview of the disaster situation, allocating limited rescue resources
  effectively is challenging.

**Result:** emergency response becomes slower, resources are distributed inefficiently,
volunteers may unintentionally give incorrect assistance, and people with life-threatening
conditions may not receive help in time.

---

## 3. Our Solution

Disha brings together **AI, offline communication, location intelligence, and emergency
guidance** into a single platform designed specifically for disaster response in Bangladesh.

The application continues functioning even when internet connectivity is unavailable by using
**device-to-device communication** and **on-device AI inference**. Gemma acts as the
decision-making engine throughout the system.

---

## 4. Core Features

### 4.1 Offline AI Assistant
Users interact with Gemma using **text, voice, or images**. The AI can:
- Understand emergency situations.
- Analyze uploaded images.
- Answer disaster-related questions.
- Provide personalized safety recommendations.
- Guide users until professional help arrives.

### 4.2 Offline Mesh Communication
Inspired by mesh networking, nearby devices communicate directly through **Bluetooth**,
enabling users to:
- Send SOS requests.
- Exchange emergency messages.
- Share location information.
- Continue communication without internet access.

Messages can propagate through nearby devices, extending communication beyond a single
Bluetooth connection.

### 4.3 AI-Powered First Aid Assistance
Helps volunteers and affected people perform basic emergency response before trained medical
personnel arrive. The AI provides:
- Step-by-step first-aid instructions.
- Guidance based on symptoms or injury descriptions.
- Image-assisted emergency assessment.
- Recommendations for immediate actions while waiting for professional care.

> The system is intended to **support — not replace** — professional medical assistance.

### 4.4 Intelligent Rescue Prioritization
Instead of showing rescuers hundreds of individual SOS requests, Gemma analyzes incoming
reports and estimates urgency, considering:
- Reported symptoms.
- Injury severity.
- Presence of children or elderly individuals.
- Pregnancy or chronic medical conditions.
- Flood conditions described by the user.

The app then organizes rescue requests by priority, helping volunteers focus on the most
urgent situations first.

### 4.5 GIS-Assisted Rescue Support
Location intelligence assists rescue operations by helping volunteers and coordinators:
- Locate nearby shelters.
- Identify safer travel routes.
- Avoid flooded or inaccessible roads.
- Navigate toward affected communities.

Rather than requiring users to understand complex GIS software, Gemma interprets
location-based information and provides simple, actionable guidance.

### 4.6 AI Disaster Summary
Instead of reading hundreds of individual reports, rescue coordinators receive AI-generated
summaries such as:
- Number of SOS requests.
- High-priority emergencies.
- Resource shortages.
- Shelter capacity.
- Blocked roads.
- Areas requiring immediate attention.

This enables faster decision-making and more effective allocation of resources.

---

## 5. Role of Gemma

Gemma is the **core intelligence** behind the platform. It is responsible for:
- Understanding text, voice, and image inputs.
- Assessing emergency situations.
- Providing first-aid guidance.
- Prioritizing rescue requests.
- Summarizing incoming reports.
- Generating recommendations for volunteers and coordinators.

Rather than acting as a simple chatbot, Gemma functions as the **reasoning engine** that
powers the entire disaster-response workflow.

---

## 6. Expected Impact

Disha aims to improve disaster response in Bangladesh by:
- Enabling communication when internet connectivity is unavailable.
- Helping volunteers provide safer, more informed assistance.
- Reducing the time required to identify high-priority rescue cases.
- Improving coordination among rescue teams.
- Supporting faster, more effective response through AI-powered decision support.

By combining offline communication, AI reasoning, first-aid assistance, and location-aware
rescue support into a single platform, Disha has the potential to make emergency response
more **organized, efficient, and accessible** during natural disasters.

---

## 7. Reference Repositories

> **Note:** Links below are constructed as `https://github.com/<account>/<repo>` from the
> account/repository names provided. A few names may be shortened or renamed upstream —
> verify each link before citing it in the final submission.

### 🏥 Medical / Healthcare

| Account | Repository | Link | Purpose |
| --- | --- | --- | --- |
| Mosss-OS | trij | https://github.com/Mosss-OS/trij | Offline AI medical triage for community health workers |
| narender-rk10 | AI-CareCompanion-Offline-Health-By-Gemma | https://github.com/narender-rk10/AI-CareCompanion-Offline-Health-By-Gemma | Offline AI health assistant: symptom checker, drug info, first aid, nearby doctors |
| saifxyzyz | DermaGemma | https://github.com/saifxyzyz/DermaGemma | Dermatology diagnosis using Gemma |
| LukasDrews97 | Gemma-4-IT-SFT-RLVR-Medical | https://github.com/LukasDrews97/Gemma-4-IT-SFT-RLVR-Medical | Medical fine-tuning of Gemma using PubMedQA |
| shroff45 | Remedium | https://github.com/shroff45/Remedium | Offline AI pharmacist that explains medicines |
| jmdevita | medical-wayfinder | https://github.com/jmdevita/medical-wayfinder | Offline hospital navigation assistant |
| cyberNoman | sehatgemma | https://github.com/cyberNoman/sehatgemma | Diabetes & nutrition assistant |
| theankitdash | AI-Nutritional-Health-Assistant-Personalized-Guidance-for-Indian-Diets | https://github.com/theankitdash/AI-Nutritional-Health-Assistant-Personalized-Guidance-for-Indian-Diets | Nutrition planning with medical reasoning |
| Valentinetemi | fisibel | https://github.com/Valentinetemi/fisibel | Synthetic healthcare dataset generation |
| bkttt0429 | GemmaFit | https://github.com/bkttt0429/GemmaFit | AI fitness & elderly exercise coach |
| Aqta-ai | bounds | https://github.com/Aqta-ai/bounds | Medical document / PHI redaction (HIPAA / privacy) |
| 47thtechcorner | RayCodes_LFM2.5 | https://github.com/47thtechcorner/RayCodes_LFM2.5 | Medical document search & RAG |
| PerzivaL099 | Nexus-Learner | https://github.com/PerzivaL099/Nexus-Learner | Academic wellness & counseling |
| snipeart007 | student-buddy | https://github.com/snipeart007/student-buddy | Mental health assistant |

### 🚨 Disaster / Emergency / Survival

| Account | Repository | Link | Purpose |
| --- | --- | --- | --- |
| RaccoonOnion | ash | https://github.com/RaccoonOnion/ash | Offline survival assistant with emergency response packs |
| helenkwok | offlineaid | https://github.com/helenkwok/offlineaid | Offline multilingual disaster-resilience app |
| helenkwok | offlineaid-pack-builder | https://github.com/helenkwok/offlineaid-pack-builder | Builds OfflineAid knowledge packs |
| huier5635-cmd | resilience-copilot-gemma4 | https://github.com/huier5635-cmd/resilience-copilot-gemma4 | Disaster triage AI agent |
| wimi321 | Beacon | https://github.com/wimi321/Beacon | Offline multilingual emergency survival guide |
| JpCurada | likas | https://github.com/JpCurada/likas | AI evacuation platform during natural disasters |
| Mitchell-Odili | floodpulse-nairobi | https://github.com/Mitchell-Odili/floodpulse-nairobi | Flood resilience & disaster monitoring |
| CompleteTech-LLC-AI-Research | gemma4good | https://github.com/CompleteTech-LLC-AI-Research/gemma4good | Water reporting & humanitarian disaster response |
| couzip | sonae | https://github.com/couzip/sonae | Disaster preparedness assistant |
| TaylorAmarelTech | gemma4_comp (DueCare) | https://github.com/TaylorAmarelTech/gemma4_comp | Protection of vulnerable migrant workers in emergencies |

### 🩹 First Aid / Emergency Medical (subset overlap)

| Account | Repository | Link |
| --- | --- | --- |
| narender-rk10 | AI-CareCompanion-Offline-Health-By-Gemma | https://github.com/narender-rk10/AI-CareCompanion-Offline-Health-By-Gemma |
| Mosss-OS | trij | https://github.com/Mosss-OS/trij |
| RaccoonOnion | ash | https://github.com/RaccoonOnion/ash |
| wimi321 | Beacon | https://github.com/wimi321/Beacon |
| helenkwok | offlineaid | https://github.com/helenkwok/offlineaid |
| huier5635-cmd | resilience-copilot-gemma4 | https://github.com/huier5635-cmd/resilience-copilot-gemma4 |

---

## 8. Reference Papers

These papers underpin the **GIS-assisted rescue** component (Feature 4.5). The pattern they
describe — an **LLM as the reasoning core that calls GIS tools rather than doing the spatial
math itself** — maps directly onto how Gemma should drive Disha's location intelligence.

### ⭐ Paper 1 (Primary technical reference) — GIS Copilot

**GIS Copilot: Towards an Autonomous GIS Agent for Spatial Analysis**

- **arXiv (free PDF):** https://arxiv.org/abs/2411.03205
- **Journal version:** *International Journal of Digital Earth* — https://www.tandfonline.com/doi/abs/10.1080/17538947.2025.2497489
- **Authors:** Temitope Akinboyewa, Zhenlong Li, Huan Ning, M. Naser Lessani (Pennsylvania State University)

**What it does:** Builds an AI agent that operates **QGIS using natural language**. Instead of
manually chaining *Import → Buffer → Spatial Join → Intersect → Export*, the user types a
request like *"Show all hospitals within 5 km of flooded schools,"* and the agent:

1. Understands the request.
2. Selects the appropriate GIS tools.
3. Generates Python / QGIS code.
4. Executes the workflow.
5. Produces maps and results.

**Key insight:** The LLM **does not perform GIS calculations**. It *plans* which operations are
needed (Buffer, Spatial Join, Clip, Distance…) and delegates execution to QGIS / GeoPandas.

```
Their architecture:
User → LLM → Agent Planner → QGIS Tools → GeoPandas → Spatial Analysis → Maps + Results
```

**Why it fits Disha:** Users never need to know GIS. A volunteer asks *"Which shelter is
safest?"* and Gemma reasons: *need shelter DB → need flood map → need road accessibility →
need user location → run GIS → return safest shelter* — the same pattern.

```
Disha mapping:
Flood victim → Gemma → Tool calling → GeoPandas / GIS engine
            → Shelter search → Flood map → Road analysis → Volunteer guidance
```

### Paper 2 (Foundational concept) — Autonomous GIS

**Autonomous GIS: The Next-Generation AI-Powered GIS**

- **arXiv:** https://arxiv.org/abs/2305.06453

Introduced the broader concept of **Autonomous GIS**: the LLM becomes the *reasoning core* of a
GIS system that can automatically collect data, plan analyses, execute geoprocessing workflows,
and present results.

### Paper 3 (Vision / research agenda) — GIScience in the Era of AI

**GIScience in the Era of Artificial Intelligence: A Research Agenda Towards Autonomous GIS**

- **arXiv:** https://arxiv.org/abs/2503.23633

Expands the vision to future GIS systems where AI retrieves geospatial datasets, plans analyses,
executes GIS workflows, generates maps, and explains results with minimal human intervention.

### Which to follow — and scope guidance

Use **GIS Copilot (Paper 1)** as the primary technical reference — it is an actual
implementation, not just a vision, and its architecture aligns closely with Disha.

> **Scope recommendation:** Do **not** try to recreate a full GIS Copilot in a hackathon.
> Reuse the same *LLM + GIS tool-calling* pattern but limit it to **3–4 disaster-specific
> tools**:
>
> 1. **Nearest safe shelter**
> 2. **Safest evacuation route**
> 3. **Flooded road detection**
> 4. **Nearby hospitals / relief centers**
>
> This keeps scope manageable while still demonstrating the paper's core concept.

**Papers → feature mapping**

| Paper | Feature it supports | Take-away for Disha |
| --- | --- | --- |
| GIS Copilot (2411.03205) | 4.5 GIS-Assisted Rescue | Concrete LLM→GIS tool-calling architecture to imitate |
| Autonomous GIS (2305.06453) | 4.5 + 4.6 Disaster Summary | Justifies the "LLM as reasoning core" design |
| GIScience Research Agenda (2503.23633) | Vision / future work | Framing & roadmap language for the writeup |

---

## 9. Recommended Repositories to Combine

Because Disha spans **offline AI + disaster response + first aid + community health
workers + GIS/map support + Bangla support**, these seven together cover almost everything
you need as reference material:

| # | Repository | Why it matters for Disha |
| --- | --- | --- |
| 1 | [Mosss-OS/trij](https://github.com/Mosss-OS/trij) | Community health worker + medical triage patterns |
| 2 | [narender-rk10/AI-CareCompanion-Offline-Health-By-Gemma](https://github.com/narender-rk10/AI-CareCompanion-Offline-Health-By-Gemma) | First aid + symptoms + drug information |
| 3 | [helenkwok/offlineaid](https://github.com/helenkwok/offlineaid) | Offline, multilingual disaster assistant |
| 4 | [RaccoonOnion/ash](https://github.com/RaccoonOnion/ash) | Offline survival knowledge packs |
| 5 | [huier5635-cmd/resilience-copilot-gemma4](https://github.com/huier5635-cmd/resilience-copilot-gemma4) | Disaster-response AI agent / triage |
| 6 | [JpCurada/likas](https://github.com/JpCurada/likas) | Evacuation planning |
| 7 | [Mitchell-Odili/floodpulse-nairobi](https://github.com/Mitchell-Odili/floodpulse-nairobi) | Flood monitoring & spatial intelligence |

---

## 10. How Each Reference Maps to Disha

| Disha Feature | Best Reference(s) | What to Borrow |
| --- | --- | --- |
| Offline AI Assistant | offlineaid, ash, Beacon | On-device inference, knowledge-pack packaging, multilingual prompts |
| Offline Mesh Communication | *(build custom)* | Bluetooth mesh is largely Disha-specific; study general BLE mesh libs |
| AI First Aid | AI-CareCompanion, trij, Beacon | First-aid content structure, symptom→action mapping, safety disclaimers |
| Rescue Prioritization | resilience-copilot-gemma4, trij | Triage scoring, urgency estimation prompts |
| GIS-Assisted Rescue | floodpulse-nairobi, likas, medical-wayfinder | Flood spatial data, evacuation routing, offline navigation |
| AI Disaster Summary | resilience-copilot-gemma4, gemma4good | Report aggregation, humanitarian summarization |
| Bangla / Multilingual | offlineaid, Beacon | Multilingual prompting and localization approach |
| Privacy / PHI Safety | Aqta-ai/bounds | Redaction of sensitive medical/personal info |

---

## 11. Suggested Architecture Sketch

```
                        ┌─────────────────────────────┐
                        │        Disha App          │
                        │        (on-device)           │
                        └──────────────┬──────────────┘
                                       │
     ┌─────────────┬───────────────────┼───────────────────┬─────────────┐
     ▼             ▼                   ▼                   ▼             ▼
 Text / Voice   Image input      Location / GIS       SOS inbox     Bluetooth
   input        (camera)         (offline maps)      (mesh msgs)    mesh radio
     │             │                   │                   │             │
     └─────────────┴─────────┬─────────┴─────────┬─────────┘             │
                             ▼                   ▼                       │
                   ┌───────────────────────────────────┐                │
                   │           GEMMA (on-device)         │◄──────────────┘
                   │  reasoning engine over all inputs   │
                   ├───────────────────────────────────┤
                   │ • First-aid guidance                │
                   │ • Triage / urgency scoring          │
                   │ • Route & shelter recommendations   │
                   │ • Disaster summary generation       │
                   └──────────────┬────────────────────┘
                                  ▼
              ┌───────────────────────────────────────────┐
              │  Role-based views                          │
              │  • Affected person: guidance + SOS         │
              │  • Volunteer: prioritized queue + first aid│
              │  • Coordinator: summary dashboard + GIS    │
              └───────────────────────────────────────────┘
```

---

## 12. Notes & Next Steps

- **Verify every repo link** before citing — a few account/repo names may be abbreviations
  or may have been renamed upstream. If a link 404s, search GitHub for the repo name.
- **Cite licenses.** If you reuse code or data from any reference repo, check its LICENSE
  and attribute appropriately in your submission.
- **Emphasize multi-input reasoning** in the writeup — this is the strongest differentiator
  for the Gemma judging criteria.
- **Add safety disclaimers** for all first-aid / medical guidance (support, not replace,
  professional care).
- **Consider a small evaluation** — even a handful of scripted disaster scenarios showing
  triage decisions strengthens the submission.

---

*Document generated as a structured brief for the Disha Gemma hackathon submission.*
