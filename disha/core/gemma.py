"""Gemma 4 runtime wrappers for Disha.

Two implementations behind one tiny interface `generate(system, user, ...) -> str`:

  * HFGemma   - real Gemma 4 (E4B/E2B) via `transformers`, for the Kaggle GPU notebook.
                Gemma is the ONLY LLM (competition rule). Multimodal image input supported
                where the model/processor allows it.
  * MockGemma - deterministic, dependency-free stand-in that emits VALID triage JSON,
                GIS tool calls, cited first-aid answers, and briefings, so the whole
                reasoning pipeline runs and is testable offline (no torch/model).

Swap MockGemma() -> HFGemma(...) and every engine behaves identically.
"""
from __future__ import annotations

import json
import re
from typing import Optional


class GemmaRunner:
    model_name = "gemma-4"

    def generate(self, system: str, user: str, temperature: float = 0.4,
                 max_tokens: int = 512, images: Optional[list] = None) -> str:
        raise NotImplementedError


# --------------------------------------------------------------------------- #
# Real Gemma 4 (Kaggle / GPU).  Model handle is configurable — confirm on Kaggle.
# --------------------------------------------------------------------------- #
class HFGemma(GemmaRunner):
    """Gemma 4 via HuggingFace transformers. Lazy imports so this file loads anywhere.

    Typical Kaggle handles (confirm the exact one in the Models tab):
      - "google/gemma-4-e4b-it"   (default here)
      - "google/gemma-4-e2b-it"   (lighter, hot-swap)
    """

    def __init__(self, model_id: str = "google/gemma-4-e4b-it",
                 device: str = "cuda", dtype: str = "bfloat16", load: bool = True):
        self.model_id = model_id
        self.model_name = model_id.split("/")[-1]
        self.device = device
        self._dtype = dtype
        self.model = None
        self.processor = None
        self.tokenizer = None
        if load:
            self.load()

    def load(self):
        import torch  # noqa
        from transformers import AutoTokenizer, AutoModelForCausalLM
        try:  # multimodal processor if available
            from transformers import AutoProcessor
            self.processor = AutoProcessor.from_pretrained(self.model_id)
        except Exception:
            self.processor = None
        self.tokenizer = AutoTokenizer.from_pretrained(self.model_id)
        import torch
        dt = getattr(torch, self._dtype, torch.float32)
        self.model = AutoModelForCausalLM.from_pretrained(
            self.model_id, torch_dtype=dt, device_map="auto")
        return self

    def generate(self, system, user, temperature=0.4, max_tokens=512, images=None) -> str:
        import torch
        content = []
        if images:
            for img in images:
                content.append({"type": "image", "image": img})
        content.append({"type": "text", "text": user})
        messages = [
            {"role": "system", "content": [{"type": "text", "text": system}]},
            {"role": "user", "content": content},
        ]
        proc = self.processor or self.tokenizer
        inputs = proc.apply_chat_template(
            messages, add_generation_prompt=True, tokenize=True,
            return_dict=True, return_tensors="pt").to(self.model.device)
        with torch.no_grad():
            out = self.model.generate(
                **inputs, max_new_tokens=max_tokens,
                do_sample=temperature > 0, temperature=max(temperature, 1e-4))
        gen = out[0][inputs["input_ids"].shape[-1]:]
        return proc.decode(gen, skip_special_tokens=True).strip()


# --------------------------------------------------------------------------- #
# Deterministic mock (offline testing / CI / demo without a GPU)
# --------------------------------------------------------------------------- #
class MockGemma(GemmaRunner):
    """Routes on the system prompt to produce realistic, schema-valid outputs."""
    model_name = "mock-gemma-4"

    def generate(self, system, user, temperature=0.4, max_tokens=512, images=None) -> str:
        s = system.lower()
        if "triage engine" in s:
            return self._triage(user, images)
        if "location assistant" in s and "explain it" not in s:
            return self._gis_tool(user)
        if "location assistant" in s and "explain it" in s:
            return self._gis_phrase(user)
        if "first-aid assistant" in s:
            return self._first_aid(user)
        if "briefing writer" in s:
            return self._summary(user)
        if "radio-uplink compressor" in s:
            return self._compress(user)
        return self._assistant(user)

    # -- per-task deterministic behaviours --------------------------------- #
    def _triage(self, user, images) -> str:
        from .triage import detect_signals, _priority_from_signals
        m = list(re.finditer(r"SOS:\s*(.+?)\s*\nJSON:", user, re.DOTALL))
        text = m[-1].group(1).strip() if m else user
        signals = detect_signals(text)
        if images:                              # a photo often shows rising water
            signals = sorted(set(signals) | {"rising_water"})
        priority, score = _priority_from_signals(signals)
        return json.dumps({
            "priority": priority, "urgency_score": score, "risk_signals": signals,
            "needs_human_review": priority in ("critical", "high"),
            "rationale": ("Signals: " + ", ".join(signals)) if signals else "No strong signals.",
            "recommended_action": {
                "critical": "Dispatch rescue immediately.",
                "high": "Prioritise on next rescue run.",
                "moderate": "Queue for relief; monitor.",
                "low": "Log and follow up.",
            }[priority],
        })

    def _gis_tool(self, user) -> str:
        from .gis import keyword_tool_fallback
        return json.dumps(keyword_tool_fallback(user))

    def _gis_phrase(self, user) -> str:
        try:
            data = json.loads(user[user.find("{"):user.rfind("}") + 1])
        except Exception:
            return "Nearest option found. / নিকটতম সহায়তা পাওয়া গেছে।"
        if isinstance(data, list) and data:
            d = data[0]
            return (f"নিকটতম আশ্রয়: {d.get('name')} ({d.get('dist_m')} মি)। / "
                    f"Nearest shelter: {d.get('name')} at {d.get('dist_m')} m.")
        if isinstance(data, dict) and "polyline" in data:
            warn = " Warning: route crosses flooded area!" if data.get("crosses_flood") else ""
            return f"Route is {data.get('dist_m')} m.{warn} / রাস্তা {data.get('dist_m')} মিটার।"
        return "Result ready. / ফলাফল প্রস্তুত।"

    def _first_aid(self, user) -> str:
        passages = re.findall(r"\[(\d+)\]\s*(?:\([^)]*\)\s*)?(.+)", user)
        steps = [f"{txt.strip()} [{n}]" for n, txt in passages[:4]]
        body = " ".join(steps) if steps else "Seek professional help."
        return body + "\nThis is first-aid guidance, not a substitute for professional medical care."

    def _summary(self, user) -> str:
        from .summary import _deterministic_briefing
        try:
            st = json.loads(user[user.find("{"):user.rfind("}") + 1])
            return _deterministic_briefing(st)
        except Exception:
            return "Briefing unavailable."

    def _compress(self, user) -> str:
        try:
            recs = json.loads(user[user.find("["):user.rfind("]") + 1])
        except Exception:
            recs = []
        crit = sum(1 for r in recs if r.get("p") == "c")
        high = sum(1 for r in recs if r.get("p") == "h")
        return json.dumps({"n": len(recs), "c": crit, "h": high, "t": recs[:3]},
                          separators=(",", ":"), ensure_ascii=False)

    def _assistant(self, user) -> str:
        return ("শান্ত থাকুন এবং নিরাপদ উঁচু স্থানে যান। / Stay calm and move to safe high ground. "
                "If anyone is injured, ask me for first aid.")
