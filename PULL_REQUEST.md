# feat: Spanish language support via Google ML Kit Translation

## Summary

Adds full Spanish language support to the Medical Symptom Pre-Screener using the
**Google ML Kit Translation API** (on-device, no API key required after first download).
The feature is controlled by a **feature flag that is OFF by default** — users explicitly
enable it in the new Settings screen.

---

## What Was Implemented

### 1. ML Kit + DataStore dependencies (`gradle/libs.versions.toml`, `app/build.gradle.kts`)
- `com.google.mlkit:translate:17.0.3` — on-device English → Spanish model
- `androidx.datastore:datastore-preferences:1.1.1` — persistent language preference

### 2. `TranslationRepository` interface (domain layer)
**File:** `domain/repository/TranslationRepository.kt`

Contract for on-device translation:
- `ensureModelDownloaded()` — downloads the Spanish ML Kit model (~20 MB, one-time, cached)
- `translate(text)` — translates English → Spanish; **always falls back to English on error**
- `close()` — releases native ML Kit resources

### 3. `TranslationRepositoryImpl` (data layer)
**File:** `data/repository/TranslationRepositoryImpl.kt`

ML Kit implementation using coroutine-bridged callbacks (`suspendCancellableCoroutine`).
Annotated `@Singleton` so one `Translator` instance is shared across the app.

### 4. `LanguagePreferenceDataStore` (data layer)
**File:** `data/local/LanguagePreferenceDataStore.kt`

Persists the EN/ES preference using **DataStore Preferences** (the modern replacement for
SharedPreferences). Key: `spanish_enabled` (Boolean, default = `false`).
Exposes a reactive `Flow<Boolean>` and a one-shot `isSpanishEnabledOnce()` helper.

### 5. Hilt DI bindings (`di/AppModule.kt`)
`TranslationRepository → TranslationRepositoryImpl` bound in `BindingsModule` following
the existing `@Binds @Singleton` pattern. `LanguagePreferenceDataStore` is a concrete
`@Singleton` injected directly (no interface needed).

### 6. `SharedTriageViewModel` — translation routing
**File:** `ui/triage/SharedTriageViewModel.kt`

After the three-layer safety pipeline produces a `TriageResult`, when Spanish is enabled:
- **Translates** `reasoning` and all `followUpQuestions` via `TranslationRepository`
- **Skips translation entirely for `EMERGENCY`** — instant display is critical
- **Saves the original English result** to Room history regardless of language setting
- Exposes `isSpanishEnabled: StateFlow<Boolean>` for the UI layer

**The 3-layer safety architecture is completely unchanged** — translation happens downstream
of `EmergencySymptomMatcher → Gemini → TriageRuleEngine`.

### 7. `SettingsViewModel` + `SettingsScreen`
**Files:** `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsScreen.kt`

New Settings screen reachable via the ⚙️ gear icon in the Input screen's top bar:
- Flag/country emoji row for English 🇺🇸 (always shown, default)
- Toggle Switch row for Español 🇪🇸 (ML Kit on-device, feature flag)
- Enabling Spanish **eagerly triggers model download** so translation is instant on next use
- Download failure is silent — `translate()` already falls back to English

### 8. `TriageScreen` — bilingual emergency alerts
**File:** `ui/triage/TriageScreen.kt`

Emergency strings are conditionally selected based on `isSpanishEnabled`:

| English | Spanish |
|---------|---------|
| "Call 911 Now" | "Llamar al 911 Ahora" |
| "🚨 EMERGENCY" | "🚨 EMERGENCIA" |
| TTS: "This appears to be an emergency. Call 911 immediately." | TTS: "Esto parece una emergencia. Llame al 911 inmediatamente." |
| "Questions to ask your doctor:" | "Preguntas para hacerle a su médico:" |
| "Find Urgent Care Nearby" | "Encontrar Atención Urgente Cercana" |
| "Start New Assessment" | "Iniciar Nueva Evaluación" |

### 9. `res/values-es/strings.xml`
**File:** `app/src/main/res/values-es/strings.xml`

Full Spanish string resource file (60+ strings) covering all screens: Input, Triage,
Settings, History, Facilities, Guidance, and the Disclaimer. Android resolves these
automatically for users whose device locale is set to Spanish.

### 10. Navigation — `MainActivity.kt`
Added `"settings"` NavGraph route. Settings is reached via the `⚙️` icon on `InputScreen`
and navigates back via the back arrow in the `TopAppBar`.

---

## How to Enable the Spanish Feature Flag

### For users (runtime toggle):
1. Open the app → tap the **⚙️ gear icon** (top-right of the main screen)
2. Tap the **Español toggle switch** → ON
3. A one-time ~20 MB model download begins automatically
4. Submit any triage assessment — the AI reasoning appears in Spanish

### For developers (programmatic):
```kotlin
// Inject LanguagePreferenceDataStore and call:
languagePreferenceDataStore.setSpanishEnabled(true)
```

The flag is stored in DataStore under key `"spanish_enabled"` and persists across
app restarts. Default is `false` (English).

---

## How to Test

### Manual test path:
1. Build and run on a device/emulator
2. Tap ⚙️ → enable Español toggle
3. Return to main screen → enter any symptom → tap "Obtener Consejo de Triaje"
4. Verify: AI reasoning and follow-up questions appear in Spanish
5. Verify: urgency label, action buttons, and TTS phrase are in Spanish
6. **Emergency test:** Enter "chest pain" → verify "Llamar al 911 Ahora" button appears
   and TTS reads the Spanish emergency phrase
7. Tap ⚙️ → disable Español → re-run assessment → verify English is restored

### Verifying the feature flag default:
Fresh install (or clear app data) → verify the app starts in English with no toggle active.

### Model download test:
Enable Spanish while on airplane mode → submit triage → verify app shows English fallback
without crashing, then re-enable WiFi and retry → Spanish appears.

---

## Test Results

```
Total test suites:  11 files
Total tests:       108
Failures:            0
Errors:              0
```

### Pre-existing test files (all green, zero modifications):
| Test Class | Tests | Result |
|---|---|---|
| `EmergencySymptomMatcherTest` | 25 | ✅ PASS |
| `ExampleUnitTest` | 1 | ✅ PASS |
| `FacilitiesFailureModeTest` | 7 | ✅ PASS |
| `GeminiCacheTest` | 6 | ✅ PASS |
| `TriageRuleEngineTest` | 16 | ✅ PASS |
| `TriageSymptomUseCaseTest` | 12 | ✅ PASS |
| `UrgencyLevelOrderingTest` | 6 | ✅ PASS |
| **Subtotal** | **73** | **✅ All pass** |

### New test files:
| Test Class | Tests | What Is Covered |
|---|---|---|
| `TranslationRepositoryImplTest` | 10 | translate() happy path, fallback, empty input, long text, ensureModelDownloaded success/failure, close() |
| `LanguagePreferenceDataStoreTest` | 6 | Default=false, setSpanishEnabled, flow emission, multi-toggle |
| `SettingsViewModelTest` | 7 | Default flag OFF, setSpanishEnabled persists, model download triggered, download failure handled, StateFlow reflects DataStore |
| `SharedTriageViewModelTranslationTest` | 12 | Spanish OFF=no translate, Spanish ON=reasoning+questions translated, EMERGENCY skips translation, fallback to English, history saved with original, clear() |
| **Subtotal** | **35** | |

---

## Architecture Compliance

| Rule | Status |
|---|---|
| Feature flag OFF by default | ✅ `spanish_enabled = false` |
| 3-layer safety architecture untouched | ✅ Translation is post-pipeline only |
| Emergency alerts in both languages | ✅ Call 911 button, TTS, urgency label |
| No existing tests modified | ✅ 73 original tests unchanged |
| Clean Architecture (domain interface, data impl) | ✅ `TranslationRepository` interface in domain |
| Hilt DI follows existing patterns (`@Binds @Singleton`) | ✅ |
| EMERGENCY translation skipped for instant display | ✅ |
| History saved in original English | ✅ |
| Graceful fallback to English on any error | ✅ `translate()` never throws |
