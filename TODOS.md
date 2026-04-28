# TODOS — MedicalSymptomPreScreener

## Post-v1 Enhancements

### TODO: Gemini API response caching
**What:** LRU cache (5 entries, 5-min TTL) in GeminiTriageApiImpl, keyed on symptoms.take(200).hashCode()
**Why:** Portfolio demo with multiple reviewers hitting identical test symptoms burns API quota. Also prevents redundant TTS "Call 911" triggers on repeated submissions.
**Pros:** Reduces Gemini API cost, improves demo latency, demonstrates thoughtful API design
**Cons:** Slight complexity in GeminiTriageApiImpl; stale results if symptoms are exactly repeated
**Context:** Emergency keyword path (Layer 1) always bypasses cache — only Gemini call is cached
**Depends on:** Core app built and working

### TODO: Process death UX improvement
**What:** When SharedTriageViewModel.triageResult is null (process death), TriageScreen shows blank. Add a "Session expired — start a new assessment" card with a button.
**Why:** Currently shows blank screen which is technically safe but confusing. Caught by /qa FM7 verification.
**Pros:** Better UX, makes the empty state explicit
**Cons:** Minor — process death is rare in a portfolio demo
**Context:** TriageScreen.kt:69 — `triageResult ?: return` exits composable on null. Replace with a composed empty state.
**Depends on:** Core app working

### TODO: Manual integration test before portfolio demo
**What:** Run actual app with real API keys. Test "chest pain" → EMERGENCY, mild headache → SELF_CARE, Gemini offline.
**Why:** All tests mock the Gemini API. Real API behavior (latency, JSON format) needs manual verification.
**Context:** Test keys are in local.properties. Build debug APK, install on device or emulator.
**Depends on:** GitHub Actions CI set up (to catch regressions)

### TODO: GitHub Actions CI workflow
**What:** Build + unit test on every push. `actions/checkout → setup-java → gradle test → gradle assembleDebug`
**Why:** Portfolio reviewers check if CI exists. A green CI badge on the README is a strong signal.
**Pros:** Proves clean build in a fresh environment; catches test regressions automatically
**Cons:** API keys must be set as GitHub secrets (GEMINI_API_KEY, MAPS_API_KEY) — tests that mock the API don't need them, but a build check does
**Context:** No signing required for debug APK. See `.github/workflows/ci.yml`
**Depends on:** Core app built, unit tests written
