package com.example.medicalsymptomprescreener.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Persistent safety disclaimer banner displayed at the top of every screen.
 *
 * Present on [InputScreen], [TriageScreen], [FacilitiesScreen], and [GuidanceScreen]
 * to ensure users are always reminded that this app provides triage **guidance** only,
 * not medical diagnosis or treatment.
 *
 * The disclaimer cannot be dismissed or hidden — it is always composed as the first
 * element of each screen's column.
 *
 * @param modifier Optional [Modifier] for overriding background or padding on specific screens.
 */
@Composable
fun DisclaimerCard(modifier: Modifier = Modifier) {
    Text(
        text = "⚠️ This is not a medical diagnosis. Always consult a qualified healthcare professional. For emergencies, call 911.",
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFFFFEBEE))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.Medium,
        color = Color(0xFFB71C1C),
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
    )
}
