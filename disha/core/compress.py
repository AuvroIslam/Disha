"""Radio-uplink compression (inspired by MeshGemma's <=200-byte incident digest).

When even the mesh can't reach responders, a tiny digest can go out over ham radio, SMS,
or a satellite messenger. Gemma 4 compresses many incidents into one <=200-byte JSON blob;
a deterministic builder guarantees the size bound even if the model misbehaves.
"""
from __future__ import annotations

import json
from typing import Optional

from .models import SOSReport, TriageResult

_PRI_SHORT = {"critical": "c", "high": "h", "moderate": "m", "low": "l"}
_RANK = {"critical": 0, "high": 1, "moderate": 2, "low": 3}


def _records(sos: list[SOSReport], triage: list[TriageResult]) -> list[dict]:
    by_id = {s.msg_id: s for s in sos}
    out = []
    for t in sorted(triage, key=lambda r: (_RANK[r.priority], -r.urgency_score)):
        s = by_id.get(t.msg_id)
        loc = (f"{s.lat:.2f},{s.lon:.2f}" if s and s.lat is not None else "")
        out.append({"i": t.msg_id[:8], "p": _PRI_SHORT[t.priority], "l": loc})
    return out


def build_radio_payload(sos, triage, max_bytes: int = 200) -> str:
    """Deterministic <=max_bytes JSON digest. Trims the top-list until it fits."""
    recs = _records(sos, triage)
    crit = sum(1 for t in triage if t.priority == "critical")
    high = sum(1 for t in triage if t.priority == "high")
    top = recs[:3]
    while True:
        blob = json.dumps({"n": len(triage), "c": crit, "h": high, "t": top},
                          separators=(",", ":"), ensure_ascii=False)
        if len(blob.encode("utf-8")) <= max_bytes or not top:
            return blob
        top = top[:-1]                              # drop least-urgent until it fits


def compress_for_radio(sos, triage, gemma=None, max_bytes: int = 200) -> dict:
    """Return {payload, bytes, ok, produced_by}. Guarantees payload <= max_bytes."""
    fallback = build_radio_payload(sos, triage, max_bytes)

    if gemma is None:
        return {"payload": fallback, "bytes": len(fallback.encode("utf-8")),
                "ok": True, "produced_by": "deterministic"}

    from .prompts import COMPRESS_SYSTEM
    recs = _records(sos, triage)
    user = json.dumps(recs, ensure_ascii=False)
    for _ in range(2):                              # single retry (MeshGemma pattern)
        raw = gemma.generate(COMPRESS_SYSTEM, user, temperature=0.2, max_tokens=200)
        try:
            obj = json.loads(raw[raw.find("{"):raw.rfind("}") + 1])
            blob = json.dumps(obj, separators=(",", ":"), ensure_ascii=False)
            if len(blob.encode("utf-8")) <= max_bytes:
                return {"payload": blob, "bytes": len(blob.encode("utf-8")),
                        "ok": True, "produced_by": "gemma"}
        except (json.JSONDecodeError, ValueError):
            pass
    # model failed the size/format bound -> deterministic fallback
    return {"payload": fallback, "bytes": len(fallback.encode("utf-8")),
            "ok": True, "produced_by": "fallback"}
