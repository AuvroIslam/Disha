"""Disha core data models (mirrors prd/PRD.md §10).

Dependency-light on purpose: stdlib only, so the reasoning core runs and is
testable without torch/transformers. The real Gemma 4 path lives in gemma.py.
"""
from __future__ import annotations

from dataclasses import dataclass, field, asdict
from typing import Optional
import time
import uuid

# --- Closed vocab (keep in sync with prompts.py and the TriageResult schema) ---

PRIORITIES = ("critical", "high", "moderate", "low")

PRIORITY_COLOR = {
    "critical": "🔴",
    "high": "🟠",
    "moderate": "🟡",
    "low": "🟢",
}

# Closed set of risk signals Gemma (and the deterministic fallback) may emit.
RISK_SIGNALS = (
    "severe_injury", "not_breathing", "unconscious", "heavy_bleeding",
    "child", "elderly", "pregnant", "chronic_illness", "trapped",
    "rising_water", "no_food_water", "medication_needed",
)

# Signals that force priority=critical when present.
LIFE_THREAT_SIGNALS = ("not_breathing", "unconscious", "heavy_bleeding")


def _now_iso() -> str:
    return time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())


def new_id() -> str:
    return str(uuid.uuid4())


@dataclass
class SOSReport:
    """A single SOS, created locally or received over the mesh."""
    text: str
    lat: Optional[float] = None
    lon: Optional[float] = None
    msg_id: str = field(default_factory=new_id)
    created_at: str = field(default_factory=_now_iso)
    reporter_role: str = "affected"          # affected | volunteer
    audio_path: Optional[str] = None
    image_path: Optional[str] = None
    loc_accuracy_m: Optional[float] = None
    people_count: int = 1
    flags: list[str] = field(default_factory=list)   # subset of RISK_SIGNALS given up-front
    status: str = "new"                       # new | triaged | acknowledged | resolved
    origin_device: Optional[str] = None
    hops: int = 0

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass
class TriageResult:
    """Output of the triage engine (Gemma JSON or deterministic fallback)."""
    msg_id: str
    priority: str                             # one of PRIORITIES
    urgency_score: float                      # 0..1
    risk_signals: list[str]
    needs_human_review: bool
    rationale: str
    recommended_action: str
    model: str = "gemma-4-e4b"
    produced_by: str = "gemma"                # gemma | fallback_rules
    created_at: str = field(default_factory=_now_iso)

    @property
    def color(self) -> str:
        return PRIORITY_COLOR.get(self.priority, "⚪")

    def to_dict(self) -> dict:
        return asdict(self)


@dataclass
class Shelter:
    id: str
    name: str
    lat: float
    lon: float
    capacity: int = 0
    occupancy: int = 0
    has_pwd_access: bool = False
    allows_pets: bool = False
    has_medical: bool = False
    on_high_ground: bool = False

    @property
    def capacity_left(self) -> int:
        return max(0, self.capacity - self.occupancy)

    @property
    def capacity_pressure(self) -> float:
        """0 = empty, 1 = full."""
        if self.capacity <= 0:
            return 1.0
        return min(1.0, self.occupancy / self.capacity)


@dataclass
class Facility:
    id: str
    name: str
    lat: float
    lon: float
    type: str                                 # hospital | relief | clinic


@dataclass
class KBChunk:
    """A first-aid knowledge chunk (RAG unit)."""
    id: str
    pack: str
    hazard: str
    text_md: str
    source: str
    lang: str = "en"
    symptom_tags: list[str] = field(default_factory=list)
    red_flags: list[str] = field(default_factory=list)
    embedding: Optional[list[float]] = None   # filled at build time on the vector path


@dataclass
class MeshPacket:
    """Transport envelope for offline P2P relay."""
    payload: dict
    msg_id: str = field(default_factory=new_id)
    ttl: int = 4
    type: str = "sos"                         # sos | ack | summary_req
    sig: Optional[str] = None

    def to_dict(self) -> dict:
        return asdict(self)
