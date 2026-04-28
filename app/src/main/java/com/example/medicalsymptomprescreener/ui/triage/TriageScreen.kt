package com.example.medicalsymptomprescreener.ui.triage

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.medicalsymptomprescreener.data.tts.TextToSpeechManager
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.domain.model.UrgencyLevel
import com.example.medicalsymptomprescreener.ui.components.DisclaimerCard
import android.content.Intent
import android.net.Uri

@Composable
fun TriageScreen(
    sharedViewModel: SharedTriageViewModel,
    onFindFacilities: () -> Unit,
    onNewAssessment: () -> Unit
) {
    val triageResult by sharedViewModel.triageResult.collectAsState()
    val context = LocalContext.current
    val ttsManager = remember { TextToSpeechManager(context) }

    DisposableEffect(Unit) {
        ttsManager.init()
        onDispose { ttsManager.destroy() }
    }

    // Auto-trigger TTS on EMERGENCY only
    LaunchedEffect(triageResult) {
        if (triageResult?.urgencyLevel == UrgencyLevel.EMERGENCY) {
            ttsManager.speak("This appears to be an emergency. Call 911 immediately.")
        }
    }

    val result = triageResult ?: return

    val (bgColor, textColor) = when (result.urgencyLevel) {
        UrgencyLevel.EMERGENCY -> Color(0xFFB71C1C) to Color.White
        UrgencyLevel.URGENT -> Color(0xFFE65100) to Color.White
        UrgencyLevel.NON_URGENT -> Color(0xFFF9A825) to Color(0xFF212121)
        UrgencyLevel.SELF_CARE -> Color(0xFF2E7D32) to Color.White
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // EMERGENCY: prominent 911 button at the very top
        if (result.urgencyLevel == UrgencyLevel.EMERGENCY) {
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:911")))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(72.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = null,
                    tint = Color(0xFFB71C1C),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "Call 911 Now",
                    color = Color(0xFFB71C1C),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Urgency level label
        Text(
            text = when (result.urgencyLevel) {
                UrgencyLevel.EMERGENCY -> "🚨 EMERGENCY"
                UrgencyLevel.URGENT -> "⚠️ URGENT"
                UrgencyLevel.NON_URGENT -> "ℹ️ NON-URGENT"
                UrgencyLevel.SELF_CARE -> "✅ SELF-CARE"
            },
            color = textColor,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(Modifier.height(8.dp))
        Text(
            text = result.timeframe,
            color = textColor.copy(alpha = 0.85f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))

        // Reasoning card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(
                    color = textColor.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Text(
                text = result.reasoning,
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 22.sp
            )
        }

        // TTS button (opt-in for non-emergency levels)
        if (result.urgencyLevel != UrgencyLevel.EMERGENCY) {
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                FilledIconButton(
                    onClick = { ttsManager.speak(result.reasoning) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = textColor.copy(alpha = 0.2f)
                    )
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "Read aloud", tint = textColor)
                }
            }
        }

        // Follow-up questions (display-only — no multi-turn)
        if (result.followUpQuestions.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
            ) {
                Text(
                    "Questions to ask your doctor:",
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                result.followUpQuestions.forEach { q ->
                    Text("• $q", color = textColor, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Find facilities button (skip for EMERGENCY — user should call 911)
        if (result.urgencyLevel != UrgencyLevel.EMERGENCY) {
            Button(
                onClick = onFindFacilities,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = textColor.copy(alpha = 0.15f),
                    contentColor = textColor
                )
            ) {
                Text(
                    text = when (result.urgencyLevel) {
                        UrgencyLevel.URGENT -> "Find Urgent Care Nearby"
                        UrgencyLevel.NON_URGENT -> "Find Primary Care Nearby"
                        UrgencyLevel.SELF_CARE -> "Find Pharmacy Nearby"
                        else -> "Find Nearby Facility"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        // New assessment
        Button(
            onClick = {
                sharedViewModel.clear()
                onNewAssessment()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Text("Start New Assessment", color = textColor)
        }

        Spacer(Modifier.height(16.dp))

        // Disclaimer always at bottom
        DisclaimerCard(
            modifier = Modifier.background(
                if (result.urgencyLevel == UrgencyLevel.EMERGENCY) Color(0xFF7B0000)
                else Color(0xFFFFEBEE)
            )
        )

        Spacer(Modifier.height(24.dp))
    }
}
