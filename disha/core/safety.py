"""Disha safety sidecar (mirrors PRD §11 / GEMMA_INTEGRATION §10).

Deterministic guards that wrap every Gemma call: prompt-injection defense,
red-flag detection, no-invention checks, disclaimer enforcement. Adapted from the
resilience-copilot-gemma4 bounded-agent pattern.
"""
from __future__ import annotations

import re

RED_FLAG_TERMS = (
    "not breathing", "no breath", "stopped breathing", "unconscious", "not responding",
    "heavy bleeding", "bleeding heavily", "spurting", "choking", "no pulse", "drowning",
    "শ্বাস নিচ্ছে না", "অজ্ঞান", "রক্তক্ষরণ", "ডুবে",
)

# Phrases that indicate an injection attempt inside user/retrieved DATA.
_INJECTION = re.compile(
    r"(ignore\s+[\w\s]{0,25}?instructions|"
    r"disregard\s+[\w\s]{0,25}?(system|instructions|prompt|rules)|"
    r"you are now|forget\s+[\w\s]{0,20}?(rules|instructions|prompt)|"
    r"reveal your (system )?prompt|new instructions:|act as (if|a|an|though))",
    re.IGNORECASE)

DISCLAIMER = "This is first-aid guidance, not a substitute for professional medical care."

# Claims Gemma must not invent (used for a light post-generation audit).
_INVENTION_HINTS = (
    "hospital has", "beds available", "road is open", "road is clear",
    "capacity is", "ambulance is on the way", "help is arriving in",
)


def wrap_as_data(untrusted: str) -> str:
    """Wrap user/retrieved content in structural markers (Beacon injection defense)."""
    return f"<<<DATA_START>>>\n{untrusted}\n<<<DATA_END>>>"


def detect_injection(untrusted: str) -> bool:
    return bool(_INJECTION.search(untrusted or ""))


def is_red_flag(text: str) -> bool:
    t = (text or "").lower()
    return any(term in t for term in RED_FLAG_TERMS)


def red_flag_banner(lang: str = "bn") -> str:
    return ("🚨 জীবন-সংকটজনক অবস্থা — এখনই সাহায্য নিন / "
            "LIFE-THREATENING: seek emergency help NOW.")


def audit_output(answer: str, allowed_facts: str = "") -> list[str]:
    """Return warnings if the answer asserts facts not grounded in allowed_facts."""
    warns = []
    low = (answer or "").lower()
    for hint in _INVENTION_HINTS:
        if hint in low and hint not in (allowed_facts or "").lower():
            warns.append(f"possible invented claim: '{hint}'")
    return warns


def ensure_disclaimer(answer: str, medical: bool = True) -> str:
    if medical and DISCLAIMER not in answer:
        return answer.rstrip() + "\n" + DISCLAIMER
    return answer


def guard_medical_answer(query: str, answer: str, allowed_facts: str = "") -> dict:
    """One-call post-check for first-aid/assistant answers."""
    return {
        "answer": ensure_disclaimer(answer, medical=True),
        "red_flag": is_red_flag(query),
        "banner": red_flag_banner() if is_red_flag(query) else None,
        "warnings": audit_output(answer, allowed_facts),
        "injection_detected": detect_injection(query),
    }
