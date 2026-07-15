"""Disha offline mesh: signed envelopes + multi-hop relay (inspired by MeshGemma).

Adds the two things our mesh design was missing versus MeshGemma:
  * Cryptographically SIGNED incident envelopes (Ed25519), so peers verify a report
    before trusting it — critical against misinformation in a disaster.
  * Lamport-clock ordering + per-node dedup for robust multi-hop flooding.

Signing is pluggable:
  * Ed25519Signer - real public-key signatures via `cryptography` (lazy import); this is
    what runs on-device (mirrors MeshGemma's tweetnacl/Ed25519).
  * DevSigner     - stdlib-only, deterministic, NON-cryptographic stand-in so the envelope /
                    relay / dedup logic is testable offline without any crypto dependency.

Transport (Nearby Connections on Android) is intentionally NOT here — this module is the
signed-envelope + relay logic that sits on top of whatever transport carries the bytes.
"""
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass, field
from typing import Optional


# --------------------------------------------------------------------------- #
# Canonical serialization (both sides must hash identical bytes)
# --------------------------------------------------------------------------- #
def canonical_bytes(obj: dict) -> bytes:
    """Deterministic JSON: sorted keys, no whitespace."""
    return json.dumps(obj, sort_keys=True, separators=(",", ":"),
                      ensure_ascii=False).encode("utf-8")


# --------------------------------------------------------------------------- #
# Signers
# --------------------------------------------------------------------------- #
class Signer:
    """node_id (public identity), sign(bytes)->hex, verify(node_id, bytes, hex)->bool."""
    node_id: str

    def sign(self, data: bytes) -> str:
        raise NotImplementedError

    @staticmethod
    def verify(node_id: str, data: bytes, sig_hex: str) -> bool:
        raise NotImplementedError


class DevSigner(Signer):
    """Deterministic, NON-cryptographic stand-in for offline tests/CI.

    sig = sha256(node_id || data). Verifiable by anyone (no secret) — good enough to exercise
    envelope/relay/dedup + tamper detection, NOT for production. Use Ed25519Signer on device.
    """

    def __init__(self, node_id: str):
        self.node_id = node_id

    @staticmethod
    def _digest(node_id: str, data: bytes) -> str:
        return hashlib.sha256(node_id.encode() + b"|" + data).hexdigest()

    def sign(self, data: bytes) -> str:
        return self._digest(self.node_id, data)

    @staticmethod
    def verify(node_id: str, data: bytes, sig_hex: str) -> bool:
        return DevSigner._digest(node_id, data) == sig_hex


class Ed25519Signer(Signer):
    """Real Ed25519 signatures (on-device). Lazy import so the module loads without crypto."""

    def __init__(self, node_id: Optional[str] = None, private_bytes: Optional[bytes] = None):
        from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
        self._sk = (Ed25519PrivateKey.from_private_bytes(private_bytes)
                    if private_bytes else Ed25519PrivateKey.generate())
        from cryptography.hazmat.primitives import serialization
        pub = self._sk.public_key().public_bytes(
            serialization.Encoding.Raw, serialization.PublicFormat.Raw)
        self.node_id = node_id or pub.hex()   # public key IS the identity

    def sign(self, data: bytes) -> str:
        return self._sk.sign(data).hex()

    @staticmethod
    def verify(node_id: str, data: bytes, sig_hex: str) -> bool:
        from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PublicKey
        from cryptography.exceptions import InvalidSignature
        try:
            Ed25519PublicKey.from_public_bytes(bytes.fromhex(node_id)).verify(
                bytes.fromhex(sig_hex), data)
            return True
        except (InvalidSignature, ValueError):
            return False


# --------------------------------------------------------------------------- #
# Signed envelope
# --------------------------------------------------------------------------- #
@dataclass
class SignedEnvelope:
    sender: str            # node_id / public key
    msg_id: str
    lamport: int
    payload: dict          # e.g. an SOSReport dict
    sig: str
    type: str = "sos"
    version: int = 1
    ttl: int = 4           # mutable relay metadata — NOT part of the signed content

    def signed_content(self) -> dict:
        return {"version": self.version, "sender": self.sender, "msg_id": self.msg_id,
                "lamport": self.lamport, "type": self.type, "payload": self.payload}

    @classmethod
    def create(cls, signer: Signer, payload: dict, msg_id: str, lamport: int,
               type: str = "sos", ttl: int = 4) -> "SignedEnvelope":
        env = cls(sender=signer.node_id, msg_id=msg_id, lamport=lamport, payload=payload,
                  sig="", type=type, ttl=ttl)
        env.sig = signer.sign(canonical_bytes(env.signed_content()))
        return env

    def verify(self, signer_cls=DevSigner) -> bool:
        return signer_cls.verify(self.sender, canonical_bytes(self.signed_content()), self.sig)

    def to_dict(self) -> dict:
        d = self.signed_content()
        d.update({"sig": self.sig, "ttl": self.ttl})
        return d


# --------------------------------------------------------------------------- #
# Node: Lamport clock + dedup + multi-hop flooding
# --------------------------------------------------------------------------- #
@dataclass
class MeshNode:
    node_id: str
    signer_cls: type = DevSigner
    clock: int = 0
    seen: set = field(default_factory=set)
    inbox: list = field(default_factory=list)     # accepted payloads

    def tick(self) -> int:
        self.clock += 1
        return self.clock

    def receive(self, env: SignedEnvelope):
        """Return an envelope to re-broadcast (ttl-1), or None (dropped/duplicate/invalid)."""
        if not env.verify(self.signer_cls):
            return None                            # tamper/forgery -> drop
        if env.msg_id in self.seen:
            return None                            # duplicate -> drop
        self.seen.add(env.msg_id)
        self.clock = max(self.clock, env.lamport) + 1   # Lamport update
        self.inbox.append(env.payload)
        if env.ttl > 0:
            fwd = SignedEnvelope(**{**env.__dict__})
            fwd.ttl = env.ttl - 1                  # signature still valid (ttl not signed)
            return fwd
        return None
