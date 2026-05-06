package com.example.medicalsymptomprescreener

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Static source-scan accessibility tests for MedicalSymptomPreScreener.
 *
 * These tests parse Kotlin UI source files to enforce WCAG 2.1 AA accessibility
 * requirements without requiring a running device or Robolectric:
 *
 * 1. **No null content descriptions on interactive Icon elements** — Icons inside
 *    [Button], [FilledIconButton], [IconButton] must have a meaningful contentDescription.
 *    TalkBack reads the contentDescription for screen-reader users; null means the
 *    control is invisible to accessibility services.
 *
 * 2. **911 emergency button regression guard** — The Call icon on the EMERGENCY
 *    result screen previously had `contentDescription = null` (TriageScreen line 113).
 *    This is the most safety-critical UI element in the app. The fix must not regress.
 *
 * 3. **Bilingual content descriptions** — Emergency-related UI must provide
 *    Spanish descriptions for Spanish-language users.
 *
 * 4. **No hardcoded English-only content descriptions on emergency UI** — Descriptions
 *    that ignore `isSpanish` on the 911 button violate the HIPAA language access standard.
 *
 * Test strategy: scan the `src/main` source tree using [File] I/O.
 * The working directory during Android unit tests is the `app/` module directory.
 *
 * DO NOT TOUCH: 3-layer safety architecture, Spanish ML Kit.
 */
class AccessibilityTest {

    private val sourceRoot = File("src/main/java/com/example/medicalsymptomprescreener")

    // ── 911 emergency button — regression guard ───────────────────────────────

    @Test
    fun `TriageScreen Call-icon contentDescription is not null after fix`() {
        val triageScreen = sourceRoot
            .walk()
            .filter { it.name == "TriageScreen.kt" }
            .firstOrNull()
            ?: error("TriageScreen.kt not found under $sourceRoot")

        val src = triageScreen.readText()

        // The old null contentDescription on the Call icon must be gone
        assertFalse(
            "TriageScreen: Icons.Default.Call must not have contentDescription = null. " +
            "Screen-reader users cannot identify the emergency button.",
            src.contains("Icons.Default.Call") &&
            src.lines()
                .dropWhile { !it.contains("Icons.Default.Call") }
                .take(3)
                .any { it.trim() == "contentDescription = null," }
        )
    }

    @Test
    fun `TriageScreen 911 button has bilingual contentDescription`() {
        val triageScreen = sourceRoot
            .walk()
            .filter { it.name == "TriageScreen.kt" }
            .firstOrNull()
            ?: error("TriageScreen.kt not found")

        val src = triageScreen.readText()
        val callIconBlock = extractBlockAfterMarker(src, "Icons.Default.Call", linesAfter = 5)

        assertTrue(
            "Call icon must have a bilingual contentDescription (isSpanish) expression",
            callIconBlock.contains("isSpanish") && callIconBlock.contains("contentDescription")
        )
    }

    @Test
    fun `TriageScreen 911 contentDescription includes Spanish Llamar al 911`() {
        val triageScreen = sourceRoot
            .walk()
            .filter { it.name == "TriageScreen.kt" }
            .firstOrNull()
            ?: error("TriageScreen.kt not found")

        val src = triageScreen.readText()

        assertTrue(
            "Emergency icon must include Spanish description 'Llamar al 911' for Spanish-language users",
            src.contains("Llamar al 911")
        )
    }

    @Test
    fun `TriageScreen 911 contentDescription includes English Call 911`() {
        val triageScreen = sourceRoot
            .walk()
            .filter { it.name == "TriageScreen.kt" }
            .firstOrNull()
            ?: error("TriageScreen.kt not found")

        val src = triageScreen.readText()

        assertTrue(
            "Emergency icon must include English description 'Call 911' for English users",
            src.contains("Call 911")
        )
    }

    // ── MicButton bilingual contentDescription ───────────────────────────────

    @Test
    fun `MicButton contentDescription responds to isListening state`() {
        val micButton = sourceRoot
            .walk()
            .filter { it.name == "MicButton.kt" }
            .firstOrNull()
            ?: error("MicButton.kt not found")

        val src = micButton.readText()

        assertTrue(
            "MicButton must describe its current state to TalkBack users",
            src.contains("isListening")
        )
        assertTrue(
            "MicButton must have a non-null contentDescription",
            src.contains("contentDescription") &&
            !src.lines()
                .filter { it.contains("contentDescription") }
                .all { it.trim() == "contentDescription = null," }
        )
    }

    @Test
    fun `MicButton contentDescription describes stop action when listening`() {
        val micButton = sourceRoot
            .walk()
            .filter { it.name == "MicButton.kt" }
            .firstOrNull()
            ?: error("MicButton.kt not found")

        val src = micButton.readText()

        assertTrue(
            "MicButton must tell screen-reader users they can stop listening",
            src.contains("Stop listening", ignoreCase = true) ||
            src.contains("Detener", ignoreCase = true)  // Spanish equivalent
        )
    }

    // ── No null contentDescriptions on interactive icons ─────────────────────

    @Test
    fun `no interactive Icon has contentDescription null in UI source files`() {
        // Interactive context markers — icons inside these elements MUST have descriptions
        val interactiveMarkers = listOf("IconButton", "FilledIconButton", "OutlinedIconButton")

        val violations = mutableListOf<String>()

        sourceRoot.walk()
            .filter { it.extension == "kt" && it.path.contains("/ui/") }
            .forEach { file ->
                val lines = file.readLines()
                lines.forEachIndexed { idx, line ->
                    // Find null contentDescriptions
                    if (line.trim() == "contentDescription = null,") {
                        // Check surrounding 20 lines for interactive context
                        val window = lines
                            .subList(maxOf(0, idx - 20), minOf(lines.size, idx + 5))
                            .joinToString("\n")
                        val inInteractiveContext = interactiveMarkers.any { window.contains(it) }
                        if (inInteractiveContext) {
                            violations.add("${file.name}:${idx + 1} — contentDescription = null inside interactive context")
                        }
                    }
                }
            }

        assertTrue(
            "Found null contentDescriptions in interactive UI elements — TalkBack users cannot operate these:\n" +
            violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun `navigation back icons all have contentDescriptions`() {
        // Back navigation icons must always have contentDescriptions
        val violations = mutableListOf<String>()

        sourceRoot.walk()
            .filter { it.extension == "kt" && it.path.contains("/ui/") }
            .forEach { file ->
                val content = file.readText()
                // Find ArrowBack icon usages
                val arrowBackPattern = Regex("""ArrowBack.*contentDescription\s*=\s*null""", RegexOption.DOT_MATCHES_ALL)
                if (arrowBackPattern.containsMatchIn(content)) {
                    violations.add("${file.name} — ArrowBack icon has null contentDescription")
                }
            }

        assertTrue(
            "Navigation back icons must have contentDescriptions for TalkBack users:\n" +
            violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    // ── Emergency-specific accessibility requirements ────────────────────────

    @Test
    fun `TriageScreen has at least one 911 reference in source`() {
        val triageScreen = sourceRoot
            .walk()
            .filter { it.name == "TriageScreen.kt" }
            .firstOrNull()
            ?: error("TriageScreen.kt not found")

        val src = triageScreen.readText()

        assertTrue(
            "TriageScreen must contain 911 emergency reference for EMERGENCY results",
            src.contains("911")
        )
    }

    @Test
    fun `TriageScreen Call icon uses Icons Default Call — not a custom icon`() {
        val triageScreen = sourceRoot
            .walk()
            .filter { it.name == "TriageScreen.kt" }
            .firstOrNull()
            ?: error("TriageScreen.kt not found")

        val src = triageScreen.readText()

        assertTrue(
            "Emergency call button must use the standard Call icon recognizable to all users",
            src.contains("Icons.Default.Call")
        )
    }

    // ── FacilityCard interactive icons ───────────────────────────────────────

    @Test
    fun `FacilityCard Phone icon has contentDescription`() {
        val facilityCard = sourceRoot
            .walk()
            .filter { it.name == "FacilityCard.kt" }
            .firstOrNull()
            ?: error("FacilityCard.kt not found")

        val src = facilityCard.readText()
        val phoneIconBlock = extractBlockAfterMarker(src, "Icons.Default.Phone", linesAfter = 3)

        assertFalse(
            "FacilityCard phone icon must not have null contentDescription",
            phoneIconBlock.contains("contentDescription = null")
        )
    }

    @Test
    fun `FacilityCard Directions icon has contentDescription`() {
        val facilityCard = sourceRoot
            .walk()
            .filter { it.name == "FacilityCard.kt" }
            .firstOrNull()
            ?: error("FacilityCard.kt not found")

        val src = facilityCard.readText()
        val directionsBlock = extractBlockAfterMarker(src, "Icons.Default.Directions", linesAfter = 3)

        assertFalse(
            "FacilityCard directions icon must not have null contentDescription",
            directionsBlock.contains("contentDescription = null")
        )
    }

    // ── Source structure sanity checks ────────────────────────────────────────

    @Test
    fun `all UI screen files have at least one contentDescription`() {
        val screensWithNoDesc = sourceRoot.walk()
            .filter { it.extension == "kt" && it.name.endsWith("Screen.kt") }
            .filter { !it.readText().contains("contentDescription") }
            .map { it.name }
            .toList()

        assertTrue(
            "These Screen files have NO contentDescription at all — likely missing accessibility:\n" +
            screensWithNoDesc.joinToString("\n"),
            screensWithNoDesc.isEmpty()
        )
    }

    @Test
    fun `TriageScreen source file exists and is non-empty`() {
        val triageScreen = sourceRoot
            .walk()
            .filter { it.name == "TriageScreen.kt" }
            .firstOrNull()
            ?: error("TriageScreen.kt not found — accessibility contract cannot be verified")

        assertTrue("TriageScreen.kt must not be empty", triageScreen.length() > 0)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns the block of text starting from the line containing [marker] plus the
     * next [linesAfter] lines. Used for checking attributes near a specific widget.
     */
    private fun extractBlockAfterMarker(src: String, marker: String, linesAfter: Int): String {
        val lines = src.lines()
        val markerIdx = lines.indexOfFirst { it.contains(marker) }
        if (markerIdx < 0) return ""
        return lines.subList(markerIdx, minOf(lines.size, markerIdx + linesAfter + 1))
            .joinToString("\n")
    }
}
