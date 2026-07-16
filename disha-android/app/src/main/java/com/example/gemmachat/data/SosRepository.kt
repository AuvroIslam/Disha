package com.example.gemmachat.data

import com.example.gemmachat.core.SosReport
import com.example.gemmachat.core.TriageResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** One real field report the app has actually handled (triaged locally or relayed over mesh). */
data class SosEntry(
    val report: SosReport,
    val triage: TriageResult,
    val source: String,               // "triage" | "mesh_sent" | "mesh_recv"
    val verified: Boolean = true,
    val hops: Int = 0,
    val ts: Long = System.currentTimeMillis(),
)

/**
 * Live, shared store of the SOS reports this device has genuinely handled. Triage adds cases it
 * assesses; Mesh adds SOS it sends or receives. The Coordinator Summary reads only from here, so
 * the briefing reflects the real situation on this device rather than any bundled sample data.
 */
class SosRepository {

    private val _entries = MutableStateFlow<List<SosEntry>>(emptyList())
    val entries: StateFlow<List<SosEntry>> = _entries

    private var lastBriefedCount = 0

    fun add(entry: SosEntry) {
        _entries.value = _entries.value + entry
    }

    fun clear() {
        _entries.value = emptyList()
        lastBriefedCount = 0
    }

    /** How many reports arrived since the last briefing was generated. */
    fun newSince(): Int = (_entries.value.size - lastBriefedCount).coerceAtLeast(0)

    fun markBriefed() {
        lastBriefedCount = _entries.value.size
    }

    fun reports(): List<SosReport> = _entries.value.map { it.report }
    fun triageResults(): List<TriageResult> = _entries.value.map { it.triage }
}
