# TODOS — MedicalSymptomPreScreener

## Post-v1 Enhancements

### ~~TODO: Gemini API response caching~~ DONE
LRU cache (5 entries, 5-min TTL) added to GeminiTriageApiImpl. Keyed on symptoms.take(200).hashCode().
Layer 1 (EmergencySymptomMatcher) always bypasses this method entirely — only the Gemini call is cached.
6 cache tests added in GeminiCacheTest.kt (hit, miss, distinct keys, LRU eviction, TTL expiry, 200-char key).

### ~~TODO: Process death UX improvement~~ DONE
SessionExpiredCard composable added to TriageScreen.kt. Shows "Session Expired" card with "Start New Assessment" button instead of blank screen when triageResult is null after process death.

### TODO: Manual integration test before portfolio demo
**What:** Run actual app with real API keys. Test "chest pain" → EMERGENCY, mild headache → SELF_CARE, Gemini offline.
**Why:** All tests mock the Gemini API. Real API behavior (latency, JSON format) needs manual verification.
**Context:** Test keys are in local.properties. Build debug APK, install on device or emulator.
**Depends on:** GitHub Actions CI set up (to catch regressions)

### ~~TODO: GitHub Actions CI workflow~~ DONE
`.github/workflows/ci.yml` created. Runs `./gradlew test` + `./gradlew assembleDebug` on every push/PR to main. Uses `local.defaults.properties` PLACEHOLDER values — no GitHub secrets needed since unit tests mock the Gemini API. CI badge added to README.md.
