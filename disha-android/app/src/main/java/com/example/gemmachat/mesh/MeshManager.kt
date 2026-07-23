package com.example.gemmachat.mesh

import android.content.Context
import com.example.gemmachat.core.SignedEnvelope
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import com.google.gson.Gson
import java.util.UUID

/**
 * Offline mesh transport over Google Nearby Connections (Bluetooth + Wi-Fi Direct, no internet).
 * Wraps each SOS in a Ed25519-signable [SignedEnvelope] (see core/Mesh.kt) — verify-before-trust,
 * Lamport-clock dedup, and controlled-flooding multi-hop relay.
 */
class MeshManager(
    context: Context,
    val localName: String,
    private val onStatus: (String) -> Unit,
    private val onPeersChanged: (Int) -> Unit,
    private val onReceived: (env: SignedEnvelope, verified: Boolean, hops: Int) -> Unit,
) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val signer = MeshIdentity.loadOrCreateSigner(context, localName)
    private val gson = Gson()
    private var clock = 0
    // Bounded dedup cache: a long relay session could otherwise grow this set without limit.
    // LRU-evicting the oldest ids past the cap keeps memory flat; re-seeing an id older than the
    // cap at worst re-delivers one message, which the UI already tolerates.
    private val seen: MutableSet<String> = java.util.Collections.synchronizedSet(
        java.util.Collections.newSetFromMap(
            object : LinkedHashMap<String, Boolean>(256, 0.75f, false) {
                override fun removeEldestEntry(eldest: Map.Entry<String, Boolean>): Boolean =
                    size > MAX_SEEN
            },
        ),
    )
    private val connected = mutableSetOf<String>()

    companion object {
        const val SERVICE_ID = "com.example.gemmachat.DISHA_MESH"
        private const val MAX_SEEN = 2_000
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            payload.asBytes()?.let { handleIncoming(String(it, Charsets.UTF_8)) }
        }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {}
    }

    private val lifecycle = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloadCallback)   // auto-accept for the demo
            onStatus("Connecting to ${info.endpointName}…")
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connected.add(endpointId)
                onPeersChanged(connected.size)
                onStatus("Connected — ${connected.size} peer(s)")
            }
        }
        override fun onDisconnected(endpointId: String) {
            connected.remove(endpointId)
            onPeersChanged(connected.size)
        }
    }

    private val discovery = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            client.requestConnection(localName, endpointId, lifecycle)
        }
        override fun onEndpointLost(endpointId: String) {}
    }

    fun start() {
        val strategy = Strategy.P2P_CLUSTER
        client.startAdvertising(
            localName, SERVICE_ID, lifecycle,
            AdvertisingOptions.Builder().setStrategy(strategy).build())
            .addOnSuccessListener { onStatus("Advertising as $localName") }
            .addOnFailureListener { onStatus("Advertise failed: ${it.message}") }
        client.startDiscovery(
            SERVICE_ID, discovery,
            DiscoveryOptions.Builder().setStrategy(strategy).build())
            .addOnFailureListener { onStatus("Discovery failed: ${it.message}") }
    }

    fun stop() {
        try {
            client.stopAdvertising(); client.stopDiscovery(); client.stopAllEndpoints()
        } catch (_: Exception) {
        }
        connected.clear()
    }

    fun peers(): Int = connected.size

    /** Compose + broadcast a signed SOS. Returns the envelope so the UI can show it locally. */
    fun sendSos(text: String, lat: Double?, lon: Double?): SignedEnvelope {
        clock += 1
        val payload = mapOf(
            "text" to text,
            "lat" to (lat?.toString() ?: ""),
            "lon" to (lon?.toString() ?: ""),
        )
        val env = SignedEnvelope.create(signer, payload, UUID.randomUUID().toString(), clock, ttl = 4)
        seen.add(env.msgId)
        broadcast(env)
        if (connected.isEmpty()) onStatus("No peers yet — SOS queued")
        else onStatus("SOS broadcast to ${connected.size} peer(s)")
        return env
    }

    /** Compose + broadcast a signed community report (type = "community"). Same trust as SOS. */
    fun sendReport(kind: String, note: String, district: String, lat: Double?, lon: Double?): SignedEnvelope {
        clock += 1
        val payload = mapOf(
            "kind" to kind,
            "note" to note,
            "district" to district,
            "lat" to (lat?.toString() ?: ""),
            "lon" to (lon?.toString() ?: ""),
        )
        val env = SignedEnvelope.create(
            signer, payload, UUID.randomUUID().toString(), clock, ttl = 4, type = "community")
        seen.add(env.msgId)
        broadcast(env)
        onStatus(if (connected.isEmpty()) "No peers yet — report queued"
        else "Report shared with ${connected.size} peer(s)")
        return env
    }

    /**
     * Broadcast a signed family "presence" beacon (type = "presence"). Only a hashed family tag and
     * an encrypted member name go on the air, so a stranger in range learns nothing.
     */
    fun sendPresence(tag: String, sealedName: String, lat: Double?, lon: Double?): SignedEnvelope {
        clock += 1
        val payload = mapOf(
            "tag" to tag,
            "who" to sealedName,
            "lat" to (lat?.toString() ?: ""),
            "lon" to (lon?.toString() ?: ""),
        )
        val env = SignedEnvelope.create(
            signer, payload, UUID.randomUUID().toString(), clock, ttl = 3, type = "presence")
        seen.add(env.msgId)
        broadcast(env)
        return env
    }

    private fun broadcast(env: SignedEnvelope) {
        if (connected.isEmpty()) return
        val bytes = gson.toJson(envToMap(env)).toByteArray(Charsets.UTF_8)
        client.sendPayload(connected.toList(), Payload.fromBytes(bytes))
            .addOnFailureListener {
                // The sender must never believe an SOS went out when the transfer actually
                // failed (peer walked out of range, radio dropped) — surface it immediately.
                onStatus("⚠ SOS failed to send: ${it.message ?: "connection lost"}")
            }
    }

    private fun envToMap(env: SignedEnvelope): Map<String, Any?> {
        val m = env.signedContent().toMutableMap()
        m["sig"] = env.sig
        m["ttl"] = env.ttl
        return m
    }

    private fun handleIncoming(json: String) {
        try {
            @Suppress("UNCHECKED_CAST")
            val d = gson.fromJson(json, Map::class.java) as Map<String, Any?>
            @Suppress("UNCHECKED_CAST")
            val payload = (d["payload"] as? Map<String, Any?>) ?: emptyMap()
            val env = SignedEnvelope(
                sender = d["sender"] as String,
                msgId = d["msg_id"] as String,
                lamport = (d["lamport"] as Double).toInt(),
                payload = payload,
                sig = d["sig"] as String,
                type = (d["type"] as? String) ?: "sos",
                version = (d["version"] as Double).toInt(),
                ttl = (d["ttl"] as Double).toInt(),
                scheme = (d["scheme"] as? String) ?: "dev-sha256",
                senderKey = (d["sender_key"] as? String) ?: "",
            )
            if (env.msgId in seen) return             // dedup
            seen.add(env.msgId)
            val ok = env.isProductionTrusted()
            clock = maxOf(clock, env.lamport) + 1     // Lamport update
            val hops = 4 - env.ttl
            onReceived(env, ok, hops)
            if (ok && env.ttl > 0) broadcast(env.copy(ttl = env.ttl - 1))   // multi-hop relay
        } catch (e: Exception) {
            onStatus("Recv error: ${e.message}")
        }
    }
}
