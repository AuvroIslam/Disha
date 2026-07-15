"""Disha coordinator disaster summary (mirrors GEMMA_INTEGRATION.md §6).

Counts are computed HERE (deterministically); Gemma only phrases them, so numbers
can never be hallucinated.
"""
from __future__ import annotations

import json
from collections import Counter
from typing import Optional

from .models import SOSReport, TriageResult, Shelter


def compute_stats(sos: list[SOSReport], triage: list[TriageResult],
                  shelters: Optional[list[Shelter]] = None,
                  blocked_roads: Optional[list[str]] = None,
                  new_since: int = 0) -> dict:
    by_id = {t.msg_id: t for t in triage}
    tiers = Counter(t.priority for t in triage)
    # Top 5 by (tier, urgency)
    rank = {"critical": 0, "high": 1, "moderate": 2, "low": 3}
    ordered = sorted(triage, key=lambda t: (rank[t.priority], -t.urgency_score))[:5]
    top5 = []
    sos_by_id = {s.msg_id: s for s in sos}
    for t in ordered:
        s = sos_by_id.get(t.msg_id)
        loc = (f"{s.lat:.3f},{s.lon:.3f}" if s and s.lat is not None else "loc?")
        top5.append({"id": t.msg_id[:8], "loc": loc, "reason": t.rationale,
                     "priority": t.priority})

    # Resource shortages from flags/signals
    shortage = Counter()
    for t in triage:
        for sig in t.risk_signals:
            if sig in ("no_food_water", "medication_needed"):
                shortage[sig] += 1

    shelter_pressure = []
    if shelters:
        for sh in shelters:
            shelter_pressure.append({"name": sh.name, "occupancy": sh.occupancy,
                                     "capacity": sh.capacity,
                                     "pressure": round(sh.capacity_pressure, 2)})

    return {
        "total_sos": len(sos),
        "new_sos": new_since,
        "critical": tiers.get("critical", 0),
        "high": tiers.get("high", 0),
        "moderate": tiers.get("moderate", 0),
        "low": tiers.get("low", 0),
        "top5": top5,
        "shortages": dict(shortage),
        "shelter_pressure": shelter_pressure,
        "blocked_roads": blocked_roads or [],
    }


def _deterministic_briefing(st: dict) -> str:
    lines = [
        f"1) Situation: {st['total_sos']} SOS total ({st['new_sos']} new).",
        f"2) Critical: {st['critical']} | High: {st['high']} "
        f"| Moderate: {st['moderate']} | Low: {st['low']}.",
        "3) Top cases:",
    ]
    for c in st["top5"]:
        lines.append(f"   - {c['id']} · {c['loc']} · {c['priority']} · {c['reason']}")
    sh = ", ".join(f"{k}×{v}" for k, v in st["shortages"].items()) or "none reported"
    lines.append(f"4) Shortages: {sh}.")
    if st["shelter_pressure"]:
        sp = ", ".join(f"{s['name']} {int(s['pressure']*100)}%" for s in st["shelter_pressure"])
        lines.append(f"5) Shelter pressure: {sp}.")
    else:
        lines.append("5) Shelter pressure: n/a.")
    br = ", ".join(st["blocked_roads"]) or "none reported"
    lines.append(f"6) Blocked roads/areas: {br}.")
    focus = "critical cases first" if st["critical"] else "high-priority cases"
    lines.append(f"7) Recommended focus: {focus}.")
    return "\n".join(lines)


def disaster_summary(sos, triage, shelters=None, blocked_roads=None,
                     new_since=0, gemma=None) -> dict:
    st = compute_stats(sos, triage, shelters, blocked_roads, new_since)
    if gemma is None:
        return {"briefing": _deterministic_briefing(st), "stats": st, "produced_by": "deterministic"}

    from .prompts import SUMMARY_SYSTEM
    user = "NUMBERS + CASES (use only these):\n" + json.dumps(st, ensure_ascii=False, indent=2)
    briefing = gemma.generate(SUMMARY_SYSTEM, user, temperature=0.3, max_tokens=350)
    return {"briefing": briefing, "stats": st, "produced_by": "gemma"}
