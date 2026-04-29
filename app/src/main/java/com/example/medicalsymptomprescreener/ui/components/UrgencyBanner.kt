package com.example.medicalsymptomprescreener.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel

/**
 * Color-coded urgency level banner displayed prominently at the top of [TriageScreen].
 *
 * Colors are chosen for maximum visibility and accessibility:
 * - EMERGENCY → dark red (#B71C1C) — maximum urgency signal
 * - URGENT → deep orange (#E65100)
 * - NON_URGENT → amber (#F9A825)
 * - SELF_CARE → dark green (#2E7D32)
 *
 * @param urgencyLevel The [UrgencyLevel] from the triage result to display.
 * @param modifier Optional [Modifier] for layout customization.
 */
@Composable
fun UrgencyBanner(urgencyLevel: UrgencyLevel, modifier: Modifier = Modifier) {
    val (label, color) = when (urgencyLevel) {
        UrgencyLevel.EMERGENCY -> "🚨 EMERGENCY — Call 911 Now" to Color(0xFFB71C1C)
        UrgencyLevel.URGENT -> "🟠 URGENT — Seek care within 24 hours" to Color(0xFFE65100)
        UrgencyLevel.NON_URGENT -> "🟡 NON-URGENT — Monitor at home" to Color(0xFFF9A825)
        UrgencyLevel.SELF_CARE -> "🟢 SELF-CARE — Rest and OTC remedies" to Color(0xFF2E7D32)
    }
    Text(
        text = label,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        textAlign = TextAlign.Center
    )
}
