"""Disha first-aid RAG with citations (mirrors GEMMA_INTEGRATION.md §8).

Two retrieval backends:
  * KeywordRetriever  - stdlib only, always available (the on-device FTS5 fallback).
  * EmbeddingRetriever - optional; uses a provided embed_fn (MiniLM ONNX on-device /
                         sentence-transformers on Kaggle). Same interface.

Generation is grounded: Gemma may cite ONLY the retrieved passages ([1], [2], ...).
"""
from __future__ import annotations

import json
import math
import re
from typing import Callable, Optional

from .models import KBChunk


# --------------------------------------------------------------------------- #
# Corpus loading
# --------------------------------------------------------------------------- #
def load_packs(path: str) -> list[KBChunk]:
    """Load first-aid packs from a JSON file (list of chunk dicts)."""
    with open(path, "r", encoding="utf-8") as f:
        rows = json.load(f)
    return [
        KBChunk(
            id=r["id"], pack=r["pack"], hazard=r.get("hazard", r["pack"]),
            text_md=r["text_md"], source=r.get("source", ""), lang=r.get("lang", "en"),
            symptom_tags=r.get("symptom_tags", []), red_flags=r.get("red_flags", []),
            embedding=r.get("embedding"),
        )
        for r in rows
    ]


# --------------------------------------------------------------------------- #
# Retrievers
# --------------------------------------------------------------------------- #
_WORD = re.compile(r"[a-z0-9ঀ-৿]+")   # includes Bangla block

# Generic function words that must not create spurious retrieval matches.
_STOPWORDS = {
    "the", "a", "an", "i", "you", "we", "my", "me", "our", "is", "are", "am", "be",
    "do", "does", "did", "to", "of", "and", "or", "in", "on", "at", "for", "how",
    "what", "can", "should", "it", "this", "that", "with", "from", "help", "please",
    "need", "have", "has", "get", "got", "was", "were", "if", "so", "but", "not",
}


def _tokens(text: str) -> list[str]:
    return _WORD.findall((text or "").lower())


def _content_tokens(text: str) -> list[str]:
    return [t for t in _tokens(text) if t not in _STOPWORDS]


class KeywordRetriever:
    """TF-style keyword scoring over symptom_tags + text (FTS5-style fallback)."""

    def __init__(self, chunks: list[KBChunk]):
        self.chunks = chunks

    def search(self, query: str, k: int = 3, hazard: Optional[str] = None) -> list[KBChunk]:
        q = set(_content_tokens(query))
        if not q:
            return []
        scored = []
        for c in self.chunks:
            if hazard and c.hazard != hazard:
                continue
            tag_tokens = [t for tag in c.symptom_tags for t in _content_tokens(tag)]
            hay = _content_tokens(c.text_md) + tag_tokens * 3   # weight symptom tags
            overlap = sum(1 for w in hay if w in q)
            if overlap:
                scored.append((overlap, c))
        scored.sort(key=lambda t: -t[0])
        return [c for _, c in scored[:k]]


class EmbeddingRetriever:
    """Cosine-nearest over precomputed/added embeddings (ash HNSW pattern, brute-force here)."""

    def __init__(self, chunks: list[KBChunk], embed_fn: Callable[[str], list[float]]):
        self.embed_fn = embed_fn
        self.chunks = [c for c in chunks if c.embedding is not None]
        if not self.chunks:                    # embed lazily if not precomputed
            for c in chunks:
                c.embedding = embed_fn(c.text_md)
            self.chunks = chunks

    @staticmethod
    def _cos(a: list[float], b: list[float]) -> float:
        dot = sum(x * y for x, y in zip(a, b))
        na = math.sqrt(sum(x * x for x in a)) or 1e-9
        nb = math.sqrt(sum(y * y for y in b)) or 1e-9
        return dot / (na * nb)

    def search(self, query: str, k: int = 3, hazard: Optional[str] = None) -> list[KBChunk]:
        qv = self.embed_fn(query)
        pool = [c for c in self.chunks if (not hazard or c.hazard == hazard)]
        pool.sort(key=lambda c: -self._cos(qv, c.embedding))
        return pool[:k]


# --------------------------------------------------------------------------- #
# Grounded, cited generation
# --------------------------------------------------------------------------- #
LIFE_THREAT_TERMS = ("not breathing", "unconscious", "heavy bleeding", "spurting",
                     "choking", "শ্বাস", "অজ্ঞান", "রক্তক্ষরণ")

DISCLAIMER = "This is first-aid guidance, not a substitute for professional medical care."


def build_context(chunks: list[KBChunk]) -> str:
    """Number passages [1..n] for citation."""
    return "\n".join(f"[{i}] ({c.source}) {c.text_md}" for i, c in enumerate(chunks, 1))


def red_flag(query: str, chunks: list[KBChunk]) -> bool:
    t = query.lower()
    if any(term in t for term in LIFE_THREAT_TERMS):
        return True
    return any(any(rf.lower() in t for rf in c.red_flags) for c in chunks)


def first_aid_answer(query: str, retriever, gemma=None, k: int = 3,
                     hazard: Optional[str] = None) -> dict:
    """Return {answer, citations, red_flag, used_chunks}."""
    chunks = retriever.search(query, k=k, hazard=hazard)
    citations = [{"n": i, "pack": c.pack, "source": c.source, "chunk_id": c.id}
                 for i, c in enumerate(chunks, 1)]
    flag = red_flag(query, chunks)

    if not chunks:
        answer = ("I don't have specific guidance for that. Please seek professional help. "
                  + DISCLAIMER)
        return {"answer": answer, "citations": [], "red_flag": flag, "used_chunks": []}

    if gemma is None:
        # Deterministic grounded answer: stitch the retrieved steps with citations.
        lines = [f"{c.text_md.strip()} [{i}]" for i, c in enumerate(chunks, 1)]
        answer = " ".join(lines) + "\n" + DISCLAIMER
    else:
        from .prompts import FIRST_AID_SYSTEM
        ctx = build_context(chunks)
        user = f"[PASSAGES]\n{ctx}\n[USER]\n{query}"
        answer = gemma.generate(FIRST_AID_SYSTEM, user, temperature=0.4, max_tokens=400)
        if DISCLAIMER not in answer:
            answer = answer.rstrip() + "\n" + DISCLAIMER

    return {"answer": answer, "citations": citations, "red_flag": flag,
            "used_chunks": [c.id for c in chunks]}
