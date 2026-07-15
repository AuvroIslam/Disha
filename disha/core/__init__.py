"""Disha reasoning core — Gemma 4-powered offline disaster response.

Public surface:
    from disha.core import (SOSReport, triage_sos, first_aid_answer, disaster_summary,
                            dispatch_gis, MockGemma, HFGemma)
"""
from .models import (SOSReport, TriageResult, Shelter, Facility, KBChunk, MeshPacket,
                     PRIORITIES, RISK_SIGNALS, PRIORITY_COLOR)
from .triage import triage_sos, fallback_triage, sort_queue, detect_signals
from .rag import (load_packs, KeywordRetriever, EmbeddingRetriever, first_aid_answer)
from .summary import disaster_summary, compute_stats
from .gemma import GemmaRunner, MockGemma, HFGemma
from .mesh import (SignedEnvelope, MeshNode, DevSigner, Ed25519Signer, canonical_bytes)
from .compress import compress_for_radio, build_radio_payload
from . import gis
from . import safety

__all__ = [
    "SOSReport", "TriageResult", "Shelter", "Facility", "KBChunk", "MeshPacket",
    "PRIORITIES", "RISK_SIGNALS", "PRIORITY_COLOR",
    "triage_sos", "fallback_triage", "sort_queue", "detect_signals",
    "load_packs", "KeywordRetriever", "EmbeddingRetriever", "first_aid_answer",
    "disaster_summary", "compute_stats",
    "GemmaRunner", "MockGemma", "HFGemma", "gis", "safety",
    "SignedEnvelope", "MeshNode", "DevSigner", "Ed25519Signer", "canonical_bytes",
    "compress_for_radio", "build_radio_payload",
]
