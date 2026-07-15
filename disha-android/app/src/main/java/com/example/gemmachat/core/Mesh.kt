package com.example.gemmachat.core

import java.security.MessageDigest

/**
 * Offline mesh: signed envelopes + multi-hop relay — Kotlin port of disha/core/mesh.py
 * (inspired by MeshGemma). Signing is pluggable; DevSigner (SHA-256) is a testable stand-in,
 * production uses Ed25519 (Android java.security, API 33+) — swap [Signer] implementation.
 */

/** Deterministic canonical JSON (sorted keys, compact) so both sides hash identical bytes. */
object Canonical {
    fun of(value: Any?): String = when (value) {
        null -> "null"
        is String -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        is Boolean -> value.toString()
        is Int -> value.toString()
        is Long -> value.toString()
        is Double -> if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        is Map<*, *> -> value.entries.sortedBy { it.key.toString() }
            .joinToString(",", "{", "}") { "\"${it.key}\":${of(it.value)}" }
        is List<*> -> value.joinToString(",", "[", "]") { of(it) }
        else -> "\"$value\""
    }
    fun bytes(value: Any?): ByteArray = of(value).toByteArray(Charsets.UTF_8)
}

interface Signer {
    val nodeId: String
    fun sign(data: ByteArray): String
}

/** Deterministic NON-cryptographic stand-in (SHA-256) for tests/CI. Not for production. */
class DevSigner(override val nodeId: String) : Signer {
    override fun sign(data: ByteArray): String = digest(nodeId, data)
    companion object {
        fun digest(nodeId: String, data: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(nodeId.toByteArray(Charsets.UTF_8)); md.update('|'.code.toByte()); md.update(data)
            return md.digest().joinToString("") { "%02x".format(it) }
        }
        fun verify(nodeId: String, data: ByteArray, sigHex: String): Boolean =
            digest(nodeId, data) == sigHex
    }
}

data class SignedEnvelope(
    val sender: String,
    val msgId: String,
    val lamport: Int,
    val payload: Map<String, Any?>,
    var sig: String = "",
    val type: String = "sos",
    val version: Int = 1,
    var ttl: Int = 4,        // relay metadata — NOT part of the signed content
) {
    fun signedContent(): Map<String, Any?> = mapOf(
        "version" to version, "sender" to sender, "msg_id" to msgId,
        "lamport" to lamport, "type" to type, "payload" to payload,
    )

    fun verify(): Boolean = DevSigner.verify(sender, Canonical.bytes(signedContent()), sig)

    companion object {
        fun create(
            signer: Signer, payload: Map<String, Any?>, msgId: String, lamport: Int,
            type: String = "sos", ttl: Int = 4,
        ): SignedEnvelope {
            val env = SignedEnvelope(signer.nodeId, msgId, lamport, payload, "", type, 1, ttl)
            env.sig = signer.sign(Canonical.bytes(env.signedContent()))
            return env
        }
    }
}

/** A mesh node: Lamport clock + dedup + multi-hop flooding. */
class MeshNode(val nodeId: String) {
    var clock: Int = 0
    val seen: MutableSet<String> = mutableSetOf()
    val inbox: MutableList<Map<String, Any?>> = mutableListOf()

    /** Returns an envelope to re-broadcast (ttl-1), or null (dropped/duplicate/invalid). */
    fun receive(env: SignedEnvelope): SignedEnvelope? {
        if (!env.verify()) return null                       // tamper/forgery -> drop
        if (env.msgId in seen) return null                   // duplicate -> drop
        seen.add(env.msgId)
        clock = maxOf(clock, env.lamport) + 1                // Lamport update
        inbox.add(env.payload)
        if (env.ttl > 0) return env.copy(ttl = env.ttl - 1)  // sig still valid (ttl not signed)
        return null
    }
}
