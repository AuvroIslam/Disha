"""Disha triage engine (mirrors GEMMA_INTEGRATION.md §4).

Flow: Gemma -> JSON -> validate -> (deterministic fallback if invalid) -> TriageResult.
The queue must never break, so validation + fallback are first-class.
"""
from __future__ import annotations

import json
import re
from typing import Optional

from .models import (SOSReport, TriageResult, PRIORITIES, RISK_SIGNALS,
                     LIFE_THREAT_SIGNALS)

_PRIORITY_RANK = {"critical": 0, "high": 1, "moderate": 2, "low": 3}


# --------------------------------------------------------------------------- #
# Parsing & validation
# --------------------------------------------------------------------------- #
def extract_json(raw: str) -> Optional[dict]:
    """Pull the first JSON object out of a model response (tolerates fences/prose)."""
    if not raw:
        return None
    fenced = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", raw, re.DOTALL)
    candidate = fenced.group(1) if fenced else None
    if candidate is None:
        start = raw.find("{")
        end = raw.rfind("}")
        candidate = raw[start:end + 1] if (start != -1 and end > start) else None
    if candidate is None:
        return None
    try:
        return json.loads(candidate)
    except json.JSONDecodeError:
        return None


def validate_triage(obj: dict) -> tuple[bool, list[str]]:
    """Lightweight schema check (no jsonschema dependency)."""
    errs: list[str] = []
    if not isinstance(obj, dict):
        return False, ["not an object"]
    if obj.get("priority") not in PRIORITIES:
        errs.append("priority invalid")
    score = obj.get("urgency_score")
    if not isinstance(score, (int, float)) or not (0.0 <= float(score) <= 1.0):
        errs.append("urgency_score out of range")
    sig = obj.get("risk_signals")
    if not isinstance(sig, list) or any(s not in RISK_SIGNALS for s in sig):
        errs.append("risk_signals invalid")
    if not isinstance(obj.get("needs_human_review"), bool):
        errs.append("needs_human_review not bool")
    for k in ("rationale", "recommended_action"):
        if not isinstance(obj.get(k), str) or not obj.get(k):
            errs.append(f"{k} missing")
    return (len(errs) == 0), errs


# --------------------------------------------------------------------------- #
# Deterministic fallback (mirrors resilience-copilot risk-signal detection)
# --------------------------------------------------------------------------- #
_KEYWORDS = {
    "not_breathing": ["not breathing", "no breath", "stopped breathing", "শ্বাস নিচ্ছে না"],
    "unconscious": ["unconscious", "passed out", "not responding", "অজ্ঞান"],
    "heavy_bleeding": ["bleeding heavily", "heavy bleeding", "lot of blood", "spurting",
                       "রক্তক্ষরণ", "প্রচুর রক্ত"],
    "severe_injury": ["broken", "fracture", "deep cut", "burn", "injured badly", "আহত",
                      "snake", "snakebite", "bitten", "sting", "amputat"],
    "trapped": ["trapped", "stuck", "can't get out", "roof", "rooftop", "আটকে", "ছাদে"],
    "rising_water": ["water rising", "rising water", "water is rising", "flood rising",
                     "still rising", "water level rising", "water rose",
                     "পানি বাড়", "বন্যা বাড়"],
    "child": ["child", "baby", "infant", "kid", "শিশু", "বাচ্চা"],
    "elderly": ["elderly", "old man", "old woman", "grandmother", "grandfather", "বয়স্ক"],
    "pregnant": ["pregnant", "pregnancy", "গর্ভবতী"],
    "chronic_illness": ["diabetic", "heart", "asthma", "dialysis", "chronic", "অসুস্থ"],
    "no_food_water": ["no food", "no water", "hungry", "thirsty", "no drinking",
                      "খাবার নেই", "পানি নেই"],
    "medication_needed": ["medicine", "insulin", "medication", "inhaler", "ঔষধ", "ওষুধ"],
}


def detect_signals(text: str, given_flags: Optional[list[str]] = None) -> list[str]:
    t = (text or "").lower()
    found = set(given_flags or [])
    for sig, kws in _KEYWORDS.items():
        if any(k in t for k in kws):
            found.add(sig)
    return [s for s in RISK_SIGNALS if s in found]   # stable ordering


def _priority_from_signals(signals: list[str]) -> tuple[str, float]:
    s = set(signals)
    if s & set(LIFE_THREAT_SIGNALS) or ({"trapped", "rising_water"} <= s):
        return "critical", 0.95
    vuln = s & {"child", "elderly", "pregnant", "chronic_illness"}
    if s & {"severe_injury", "trapped", "medication_needed"} or (vuln and "rising_water" in s):
        return "high", 0.75
    if s & {"rising_water", "no_food_water"} or vuln:
        return "moderate", 0.45
    return "low", 0.2


def fallback_triage(sos: SOSReport) -> TriageResult:
    signals = detect_signals(sos.text, sos.flags)
    priority, score = _priority_from_signals(signals)
    return TriageResult(
        msg_id=sos.msg_id, priority=priority, urgency_score=score,
        risk_signals=signals, needs_human_review=priority in ("critical", "high"),
        rationale=("Signals: " + ", ".join(signals)) if signals else "No strong signals detected.",
        recommended_action=_default_action(priority),
        produced_by="fallback_rules",
    )


def _default_action(priority: str) -> str:
    return {
        "critical": "Dispatch rescue immediately; flag for human review.",
        "high": "Prioritise on next rescue run; verify details.",
        "moderate": "Queue for relief/support; monitor.",
        "low": "Log; follow up when capacity allows.",
    }[priority]


# --------------------------------------------------------------------------- #
# Public entry point
# --------------------------------------------------------------------------- #
def triage_sos(sos: SOSReport, gemma=None) -> TriageResult:
    """Triage one SOS. Uses Gemma if provided & valid, else deterministic fallback."""
    if gemma is None:
        return fallback_triage(sos)

    from .prompts import TRIAGE_SYSTEM, TRIAGE_FEWSHOT
    shots = "\n".join(f"SOS: {inp}\nJSON: {json.dumps(out)}" for inp, out in TRIAGE_FEWSHOT)
    user = f"{shots}\nSOS: {sos.text}\nJSON:"
    raw = gemma.generate(TRIAGE_SYSTEM, user, temperature=0.3, max_tokens=256)
    obj = extract_json(raw)
    ok, _ = validate_triage(obj) if obj is not None else (False, [])
    if not ok:
        res = fallback_triage(sos)
        return res
    return TriageResult(
        msg_id=sos.msg_id, priority=obj["priority"],
        urgency_score=float(obj["urgency_score"]), risk_signals=list(obj["risk_signals"]),
        needs_human_review=bool(obj["needs_human_review"]),
        rationale=obj["rationale"], recommended_action=obj["recommended_action"],
        model=getattr(gemma, "model_name", "gemma-4-e4b"), produced_by="gemma",
    )


def sort_queue(results: list[TriageResult]) -> list[TriageResult]:
    """Priority tier, then urgency score desc."""
    return sorted(results, key=lambda r: (_PRIORITY_RANK[r.priority], -r.urgency_score))
