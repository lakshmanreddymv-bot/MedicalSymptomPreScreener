# Changelog

## [1.0.0.0] - 2026-04-28

### Added
- Three-layer defense-in-depth safety architecture
  - Layer 1: EmergencySymptomMatcher — offline keyword + synonym detection on full untruncated string, 7 emergency groups, 50+ synonyms, natural language variants ("my heart feels weird")
  - Layer 1b: TemporalDetector — sudden onset + amber term → URGENT minimum floor
  - Layer 2: TriageRuleEngine — post-AI validation, hedging language detection, anatomical risk term check (Mount Sinai 2026 "paradoxical safety explanation" fix)
- Gemini 2.0 Flash triage integration — advisory only, temperature 0.1, responseMimeType JSON, single-turn (no instruction drift)
- NetworkMonitor — NET_CAPABILITY_VALIDATED check before every Gemini call, graceful offline fallback
- SharedTriageViewModel — NavGraph-scoped, process-death safe, no serialization
- SpeechRecognitionManager — @ActivityRetainedScoped, all 9 error codes mapped, partial results for live transcript
- TextToSpeechManager — auto-trigger on EMERGENCY ("Call 911 now"), opt-in for other urgency levels
- Google Maps + Places API v1 integration — new endpoint (searchNearby), urgency-based type mapping
- GuidanceScreen for TELEHEALTH/HOME_CARE — skips Places API call entirely
- RuralFallbackCard — 911 dial button + Google Maps deep link when no facilities found
- Room persistence — SymptomEntity, SymptomDao, swipe-to-delete HistoryScreen
- RECORD_AUDIO permission gate in InputScreen (Accompanist) — text input always visible
- DisclaimerCard — visible on every screen, safety disclaimer always present
- 67 unit tests: EmergencySymptomMatcher (25), TriageRuleEngine (16), UrgencyLevelOrdering (6), TriageSymptomUseCase (12), FacilitiesFailureMode (7)
- UrgencyLevelOrderingTest — safety-critical enum invariant guard
- All 8 failure modes verified: Gemini down, Maps fail, mic denied, offline, emergency keywords, anatomical escalation, process death, TELEHEALTH routing
- Clean Architecture: domain layer has zero Android dependencies
- Hilt dependency injection with two Retrofit instances (Gemini + Places)
