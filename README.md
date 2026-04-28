# AI Medical Symptom Pre-Screener

> **⚠️ IMPORTANT SAFETY DISCLAIMER**
> This application is NOT a medical diagnosis tool. It provides general triage guidance only.
> **Always consult a qualified healthcare professional.**
> **For emergencies, call 911 immediately.**

[![CI](https://github.com/lakshmanreddymv-bot/MedicalSymptomPreScreener/actions/workflows/ci.yml/badge.svg)](https://github.com/lakshmanreddymv-bot/MedicalSymptomPreScreener/actions/workflows/ci.yml)
[![Android](https://img.shields.io/badge/Android-26%2B-green)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue)](https://kotlinlang.org)
[![Tests](https://img.shields.io/badge/Tests-67%20passing-brightgreen)](app/src/test)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM%20%2B%20Hilt-orange)](https://developer.android.com/topic/architecture)

**Project 4 in a portfolio of real-world AI Android apps.**
Built to demonstrate safety-critical AI architecture for Big Tech (Google, Meta, Apple) interviews.

---

## The Problem

- **40% of ER visits are unnecessary**, costing $32B+ annually in the US
- Rural areas have zero access to doctors — the nearest hospital may be 2+ hours away
- People don't know when to seek urgent vs non-urgent care
- Every minute of delay in real emergencies costs lives

## The Solution

Voice or text symptom input → Gemini 2.0 Flash AI triage → urgency level → nearest facility via Maps.

**Safety first. Never replaces a doctor. Only guides timing.**

---

## What Makes This Different: Three-Layer Defense-in-Depth

Most AI triage apps call an AI and trust the result. This app doesn't.

The 2026 Mount Sinai study found that state-of-the-art AI models undertriage **52% of emergencies**
via "paradoxical safety explanations" — the AI recognizes danger in its reasoning but still advises
waiting. This app builds the explicit fix.

```
TRIAGE EXECUTION FLOW
══════════════════════════════════════════════════════════════════
                                                                  
  Layer 1: EmergencySymptomMatcher (offline, ~5ms)               
  ┌─────────────────────────────────────────────────────────┐    
  │ Synonym groups + natural language variants               │    
  │ "my heart feels weird" → EMERGENCY (no API needed)      │    
  │ TemporalDetector: "suddenly dizzy" → URGENT minimum     │    
  │ Runs on FULL untruncated string                          │    
  └──────────────────┬──────────────────────────────────────┘    
                     │ No match? Continue to Layer 2             
                     ▼                                           
  Pre-check: Network available?                                   
  NO → safetyFallbackResult (URGENT + "call 911 if emergency")   
                     │                                           
                     ▼                                           
  Gemini 2.0 Flash call (advisory, temperature=0.1)              
                     │                                           
                     ▼                                           
  Layer 2: TriageRuleEngine (can ONLY escalate, never de-escalate)
  ┌─────────────────────────────────────────────────────────┐    
  │ Check A: Hedging language in Gemini reasoning?          │    
  │   "might", "could", "cannot rule out" → floor to URGENT │    
  │ Check B: Anatomical risk terms in Gemini reasoning?     │    
  │   "cardiac", "respiratory", "brain" → floor to URGENT   │    
  │   ← Mount Sinai "paradoxical safety explanation" fix    │    
  └──────────────────┬──────────────────────────────────────┘    
                     │                                           
                     ▼                                           
  EMERGENCY / URGENT / NON_URGENT / SELF_CARE                    
══════════════════════════════════════════════════════════════════
```

**Iron rule:** EMERGENCY is a floor. Deterministic code is authoritative. Gemini is advisory.

---

## Architecture

```
┌─────────────────────────────────────────────┐
│           UI Layer (Jetpack Compose)         │
│  InputScreen  TriageScreen  FacilitiesScreen │
│  GuidanceScreen  HistoryScreen               │
│  SharedTriageViewModel (NavGraph-scoped)     │
└─────────────────────┬───────────────────────┘
                      │
┌─────────────────────▼───────────────────────┐
│          Domain Layer (Pure Kotlin)          │
│  TriageSymptomUseCase                        │
│  EmergencySymptomMatcher ← Layer 1           │
│  TriageRuleEngine ← Layer 2                  │
│  NetworkMonitor (interface)                  │
│  FindNearbyFacilitiesUseCase                 │
│  GetSymptomHistoryUseCase · SaveSymptomUseCase│
└─────────────────────┬───────────────────────┘
                      │
┌─────────────────────▼───────────────────────┐
│           Data Layer                         │
│  GeminiTriageApiImpl (Retrofit)              │
│  GooglePlacesApiImpl (Retrofit, new v1 API)  │
│  ConnectivityNetworkMonitor                  │
│  SpeechRecognitionManager (@ActivityRetained)│
│  TextToSpeechManager                         │
│  Room: SymptomDao · SymptomDatabase          │
└─────────────────────────────────────────────┘
```

**Clean Architecture + MVVM + Hilt.** Zero Android dependencies in the domain layer.

---

## Safety Guardrails

### Keyword + Synonym Detection (Layer 1)

The safety layer runs on the **full, untruncated** symptom string before any AI call:

```kotlin
val EMERGENCY_GROUPS = listOf(
    // Cardiac — natural language variants
    setOf("chest pain", "chest tightness", "heart attack",
          "my heart feels weird", "heart feels strange"),
    // Respiratory
    setOf("can't breathe", "difficulty breathing", "short of breath"),
    // Neurological
    setOf("stroke", "seizure", "passed out", "loss of consciousness"),
    // Mental health emergency
    setOf("suicidal", "overdose", "took too many pills"),
    // Allergic
    setOf("anaphylaxis", "throat closing", "throat is closing"),
    // ... 7 groups total, 50+ synonyms
)
```

**Emergency keyword → EMERGENCY immediately. No AI call. No network needed.**

### Anatomical Risk Term Check (Layer 2, the Mount Sinai fix)

```kotlin
val ANATOMICAL_RISK_TERMS = setOf(
    "heart", "cardiac", "airway", "respiratory", "brain",
    "neurological", "spine", "aorta", "pulmonary"
)

// If Gemini writes "cardiac" in its reasoning but sets urgency=NON_URGENT
// → this check catches it and floors to URGENT
```

### All Failure Modes Handled

| Failure | Detection | Response |
|---------|-----------|----------|
| Gemini API down | catch(IOException) | URGENT fallback + "call 911 if emergency" |
| Truncated JSON | catch(EOFException) | URGENT fallback |
| No internet | NetworkCapabilities check | URGENT fallback (Layer 1 still ran) |
| Maps API fails | empty/error result | RuralFallbackCard with 911 + search links |
| Mic denied | Accompanist permission | Text input shown, mic hidden |
| TELEHEALTH result | shouldSkipFacilitySearch | GuidanceScreen (no Places API call) |

---

## Gemini Prompt Engineering

Single-turn calls only (no multi-turn — prevents instruction drift):

```json
{
  "system_instruction": "You are a medical triage assistant. You do NOT diagnose conditions.
    You do NOT suggest medications. You ALWAYS recommend professional medical consultation.",
  "generationConfig": {
    "temperature": 0.1,
    "responseMimeType": "application/json"
  }
}
```

`temperature: 0.1` — medical triage must be consistent, not creative.
`responseMimeType: "application/json"` — eliminates parsing failures from prose wrappers.

---

## Real-World Use Cases

| Scenario | Expected Result |
|---|---|
| Parent: "My child has had a fever for 3 days" | NON_URGENT → Primary Care |
| "Chest pain and I can't breathe" | EMERGENCY → 911 (Layer 1 keyword match) |
| Rural patient, nearest hospital 2hrs away | URGENT → closest urgent_care_center shown |
| "Mild headache and tired" | SELF_CARE → home rest + pharmacy |
| "Throat is closing" | EMERGENCY → 911 (synonym group match) |
| AI writes "cardiac" in reasoning, sets NON_URGENT | URGENT (Layer 2 anatomical fix) |

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 |
| Architecture | Clean Architecture + MVVM + Hilt 2.59.1 |
| AI/Triage | Gemini 2.0 Flash (v1beta) via Retrofit |
| Maps | Google Maps Compose + Places API v1 |
| Voice Input | Android SpeechRecognizer (built-in) |
| TTS | Android TextToSpeech (built-in) |
| Database | Room 2.7.1 |
| Permissions | Accompanist Permissions |
| Networking | Retrofit 2.11.0 + OkHttp 4.12.0 |
| Testing | JUnit4 + Mockito-Kotlin + Coroutines-Test |

---

## Test Coverage

```
EmergencySymptomMatcher:   25 tests (synonym groups, natural language, temporal detection)
TriageRuleEngine:          16 tests (hedging language, anatomical terms, escalation rules)
UrgencyLevelOrdering:       6 tests (safety-critical enum invariant)
TriageSymptomUseCase:      12 tests (Gemini down, offline, full-string Layer 1 invariant)
FacilitiesFailureMode:      7 tests (TELEHEALTH/HOME_CARE skip logic)
─────────────────────────────────────
Total:                     67 tests, 0 failures
```

The `UrgencyLevelOrderingTest` is particularly important — it enforces the enum ordinal
ordering that the RuleEngine depends on. If anyone reorders the enum (e.g., for readability),
this test fails immediately with a clear message before any production code is affected.

---

## Setup

### Prerequisites
- Android Studio Ladybug or later
- Android SDK 36
- Gemini API key ([get one free](https://aistudio.google.com/app/apikey))
- Google Maps API key ([enable Maps + Places API](https://console.cloud.google.com/))

### Configuration

Add to `local.properties` (never commit this file):
```properties
gemini.api.key=YOUR_GEMINI_API_KEY
maps.api.key=YOUR_MAPS_API_KEY
```

### Build

```bash
./gradlew assembleDebug        # build APK
./gradlew test                 # run all 67 unit tests
./gradlew installDebug         # install on connected device
```

---

## Architecture Decisions (Key Ones)

**Why `SharedTriageViewModel` scoped to NavGraph?**
`TriageResult` contains a `List<String>` — too complex to serialize as a nav argument.
NavGraph-scoped ViewModel survives rotation without serialization. On process death,
`TriageScreen` guards with `triageResult ?: return` (blank screen, no crash).

**Why `@ActivityRetainedScoped` for SpeechRecognitionManager?**
`SpeechRecognizer` must be created on the main thread. `@Singleton` scoping can
instantiate it during app startup off the main thread, causing silent failures.
ActivityRetained scope ensures main-thread creation and correct lifecycle.

**Why the new Places API endpoint?**
`maps.googleapis.com/maps/api/place/nearbysearch` is deprecated.
`places.googleapis.com/v1/places:searchNearby` is the current endpoint.
Type mapping: URGENT → `["hospital", "urgent_care_center"]` (not `"doctor"` —
the `doctor` type is sparsely indexed and returns near-empty results in most areas).

**Why `temperature: 0.1` for Gemini?**
Medical triage must be deterministic. High temperature means the same symptoms
could get different urgency levels on consecutive calls. Low temperature makes
the model consistent.

---

## Ethical Considerations

This app provides triage **guidance**, not medical **diagnosis**. Every screen shows
the disclaimer. No specific medications or treatments are ever suggested. The app
always recommends professional consultation and makes 911 prominent throughout.

The safety guardrails are designed conservatively — when uncertain, the system
escalates (never de-escalates). False positives (unnecessary urgency) are preferable
to false negatives (missed emergencies).

---

## Portfolio

| Project | Description | Tech |
|---|---|---|
| [MySampleApplication-AI](https://github.com/lakshmanreddymv-bot/MySampleApplication-AI) | AI Natural Language Search | Gemini API + Clean Architecture |
| [FakeProductDetector](https://github.com/lakshmanreddymv-bot/FakeProductDetector) | Counterfeit detection ($500B problem) | Gemini Vision + Claude Haiku |
| [EnterpriseDocumentRedactor](https://github.com/lakshmanreddymv-bot/EnterpriseDocumentRedactor) | GDPR/HIPAA PII redaction, fully offline | ML Kit OCR + on-device AI |
| **MedicalSymptomPreScreener** | **Safety-critical AI triage** | **Gemini + Three-layer safety** |

---

## License

MIT License — see [LICENSE](LICENSE)

## Author

**Lakshmana Reddy** — Android Tech Lead, 12 years experience
Building AI-powered Android apps | Pleasanton, California
[GitHub](https://github.com/lakshmanreddymv-bot) | lakshmanreddymv@gmail.com
