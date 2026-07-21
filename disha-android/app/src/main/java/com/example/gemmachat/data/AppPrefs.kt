package com.example.gemmachat.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small persisted app settings shared across screens: interface language and which offline
 * region packs are installed / active. Backed by SharedPreferences, exposed as StateFlows so
 * Compose recomposes when they change.
 */
class AppPrefs(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("disha_prefs", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(sp.getString(KEY_LANG, "en") ?: "en")
    val language: StateFlow<String> = _language.asStateFlow()
    val isBangla: Boolean get() = _language.value == "bn"

    private val _installedRegions = MutableStateFlow(
        sp.getStringSet(KEY_INSTALLED, setOf(DEFAULT_REGION))?.toSet() ?: setOf(DEFAULT_REGION),
    )
    val installedRegions: StateFlow<Set<String>> = _installedRegions.asStateFlow()

    private val _activeRegion = MutableStateFlow(sp.getString(KEY_ACTIVE, DEFAULT_REGION) ?: DEFAULT_REGION)
    val activeRegion: StateFlow<String> = _activeRegion.asStateFlow()

    // First-run coaching balloon on Home — shown once, then dismissed for good.
    private val _coachSeen = MutableStateFlow(sp.getBoolean(KEY_COACH_SEEN, false))
    val coachSeen: StateFlow<Boolean> = _coachSeen.asStateFlow()

    fun markCoachSeen() {
        _coachSeen.value = true
        sp.edit().putBoolean(KEY_COACH_SEEN, true).apply()
    }

    fun setLanguage(lang: String) {
        _language.value = lang
        sp.edit().putString(KEY_LANG, lang).apply()
    }

    fun installRegion(id: String) {
        val next = _installedRegions.value + id
        _installedRegions.value = next
        sp.edit().putStringSet(KEY_INSTALLED, next).apply()
        setActiveRegion(id)
    }

    fun setActiveRegion(id: String) {
        _activeRegion.value = id
        sp.edit().putString(KEY_ACTIVE, id).apply()
    }

    companion object {
        const val DEFAULT_REGION = "chattogram"
        private const val KEY_LANG = "language"
        private const val KEY_INSTALLED = "installed_regions"
        private const val KEY_ACTIVE = "active_region"
        private const val KEY_COACH_SEEN = "coach_seen"
    }
}
