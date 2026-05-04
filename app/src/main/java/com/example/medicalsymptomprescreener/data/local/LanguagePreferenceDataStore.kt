package com.example.medicalsymptomprescreener.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** DataStore singleton extension on [Context] — one instance per process. */
private val Context.languageDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "language_preferences"
)

/**
 * Persists the user's language preference (English / Spanish) using DataStore Preferences.
 *
 * **Feature flag behavior:** Spanish support is OFF by default ([isSpanishEnabled] defaults
 * to `false`). The user explicitly enables it in the Settings screen. This satisfies the
 * "feature flag OFF by default" requirement.
 *
 * **DataStore vs. SharedPreferences:** DataStore is the Android-recommended replacement —
 * it is coroutine-safe, has no ANR risk, and provides a typed API.
 *
 * The stored key is a Boolean (`true` = Spanish, `false` = English) rather than a String
 * enum so that adding future languages requires only a new key, with no migration needed.
 *
 * S: Single Responsibility — persists and exposes the language preference only.
 * D: Dependency Inversion — injected as a concrete class; callers depend on it directly
 *    (DataStore is already an abstraction over the underlying store).
 */
@Singleton
class LanguagePreferenceDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.languageDataStore

    companion object {
        /** Preferences key: `true` = Spanish enabled, `false` = English (default). */
        val SPANISH_ENABLED_KEY = booleanPreferencesKey("spanish_enabled")
    }

    /**
     * Cold [Flow] that emits whenever the language preference changes.
     * Emits `false` (English) on first subscription until the user explicitly enables Spanish.
     */
    val isSpanishEnabled: Flow<Boolean> =
        dataStore.data.map { prefs -> prefs[SPANISH_ENABLED_KEY] ?: false }

    /**
     * Persists the language preference to DataStore.
     *
     * @param enabled `true` to enable Spanish, `false` to revert to English.
     */
    suspend fun setSpanishEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[SPANISH_ENABLED_KEY] = enabled
        }
    }

    /**
     * One-shot suspend read of the current language preference.
     * Uses [Flow.first] which cancels the collection after the first emission.
     * Safe to call from a coroutine context.
     *
     * Note: [isSpanishEnabled] is the preferred reactive API for observing changes;
     * this helper is provided for one-shot reads (e.g. inside ViewModel init blocks).
     */
    suspend fun isSpanishEnabledOnce(): Boolean = isSpanishEnabled.first()
}
