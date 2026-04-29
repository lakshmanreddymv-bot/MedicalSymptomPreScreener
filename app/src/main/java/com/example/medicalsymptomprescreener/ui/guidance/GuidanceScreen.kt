package com.example.medicalsymptomprescreener.ui.guidance

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.ui.components.DisclaimerCard

/**
 * Guidance screen for [CareType.TELEHEALTH] and [CareType.HOME_CARE] triage results.
 *
 * No Google Places API call is made — these care types do not require finding a
 * physical location. The router in [MainActivity] sends users here instead of
 * [FacilitiesScreen] when the triage result is TELEHEALTH or HOME_CARE.
 *
 * Displays contextual guidance:
 * - [CareType.TELEHEALTH] → [TelehealthGuidance]: virtual visit options and tips
 * - [CareType.HOME_CARE] → [HomeCareGuidance]: rest, OTC remedies, monitoring advice
 *
 * Both paths include a 911 escalation reminder at the bottom and a "Start New Assessment" button.
 *
 * @param careType Either [CareType.TELEHEALTH] or [CareType.HOME_CARE].
 * @param onBack Navigation callback for the back arrow.
 * @param onNewAssessment Navigation callback to return to [InputScreen].
 */
// Shown when recommendedCareType is TELEHEALTH or HOME_CARE.
// No Places API call is made — no physical facility to find.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuidanceScreen(
    careType: CareType,
    onBack: () -> Unit,
    onNewAssessment: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (careType == CareType.TELEHEALTH) "Telehealth Options" else "Home Care Guidance") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DisclaimerCard()
            Spacer(Modifier.height(24.dp))

            if (careType == CareType.TELEHEALTH) {
                TelehealthGuidance()
            } else {
                HomeCareGuidance()
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNewAssessment,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Text("Start New Assessment")
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Guidance content for [CareType.TELEHEALTH] results.
 * Suggests checking insurance portals and common telehealth services.
 * Always includes a 911 escalation reminder at the bottom.
 */
@Composable
private fun TelehealthGuidance() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("💻 A virtual visit may be right for you", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        GuidancePoint("Check your insurance's telehealth app or member portal")
        GuidancePoint("Common services: Teladoc, MDLive, or your insurance's own telehealth")
        GuidancePoint("Have your symptoms ready to describe clearly")
        GuidancePoint("If symptoms worsen significantly, reassess and consider in-person care")
        Spacer(Modifier.height(16.dp))
        Text(
            "If you believe this may be an emergency at any point, call 911 immediately.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color(0xFFB71C1C)
        )
    }
}

/**
 * Guidance content for [CareType.HOME_CARE] results.
 * Suggests rest, hydration, OTC remedies, and symptom monitoring.
 * Always includes a 911 escalation reminder at the bottom.
 */
@Composable
private fun HomeCareGuidance() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Text("🏠 Home care is appropriate", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        GuidancePoint("Rest and stay hydrated")
        GuidancePoint("Over-the-counter remedies may help (follow package instructions)")
        GuidancePoint("Monitor your symptoms — if they worsen, reassess")
        GuidancePoint("Return to this app if symptoms change significantly")
        GuidancePoint("See a doctor if symptoms persist beyond 48-72 hours")
        Spacer(Modifier.height(16.dp))
        Text(
            "If you believe this may be an emergency at any point, call 911 immediately.",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = androidx.compose.ui.graphics.Color(0xFFB71C1C)
        )
    }
}

/**
 * A single bullet-point guidance item.
 * @param text The guidance text to display.
 */
@Composable
private fun GuidancePoint(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
