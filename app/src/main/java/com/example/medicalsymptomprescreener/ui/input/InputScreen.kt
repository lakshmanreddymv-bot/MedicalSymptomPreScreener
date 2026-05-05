package com.example.medicalsymptomprescreener.ui.input

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.medicalsymptomprescreener.data.speech.RecognitionState
import com.example.medicalsymptomprescreener.domain.model.TriageResult
import com.example.medicalsymptomprescreener.ui.components.DisclaimerCard
import com.example.medicalsymptomprescreener.ui.components.MicButton
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/** Body area filter options displayed as horizontal chips. */
private val BODY_PARTS = listOf("Head", "Chest", "Abdomen", "Limbs", "General")

/**
 * Primary entry screen for the AI Medical Pre-Screener.
 *
 * Provides two symptom input methods:
 * 1. **Voice input** — gated by `RECORD_AUDIO` permission via Accompanist. The
 *    [OutlinedTextField] is always visible regardless of permission state so users
 *    always have a text fallback.
 * 2. **Text input** — always accessible, never hidden.
 *
 * Permission handling stays in this composable — [InputViewModel] never touches
 * Android permission APIs (Clean Architecture boundary).
 *
 * Speech recognition results flow: [SpeechRecognitionManager] → [InputViewModel.speechState]
 * → [LaunchedEffect] in this screen → [InputViewModel.updateSymptomText].
 *
 * On submit, calls [onTriageResult] with the [TriageResult] and the raw symptom text,
 * which [MainActivity] passes to [SharedTriageViewModel] before navigating to [TriageScreen].
 *
 * @param onTriageResult Callback invoked with the triage result and symptom text on success.
 * @param viewModel [InputViewModel] injected by Hilt. Overridable for previews.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun InputScreen(
    onTriageResult: (TriageResult, String) -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: InputViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val symptomText by viewModel.symptomText.collectAsState()
    val selectedBodyPart by viewModel.selectedBodyPart.collectAsState()
    val speechState by viewModel.speechState.collectAsState()

    // Sync speech results into ViewModel text field
    LaunchedEffect(speechState) {
        when (val s = speechState) {
            is RecognitionState.Partial -> viewModel.updateSymptomText(s.transcript)
            is RecognitionState.Result -> viewModel.onSpeechResult(s.transcript)
            is RecognitionState.Error -> viewModel.onSpeechError(s.message)
            else -> Unit
        }
    }

    // Destroy recognizer when leaving this screen
    DisposableEffect(Unit) {
        onDispose { viewModel.stopListening() }
    }

    // Mic permission — ViewModel never touches this
    val micPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Medical Pre-Screener", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
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
            // Always-visible disclaimer
            DisclaimerCard()

            Spacer(Modifier.height(24.dp))

            // Mic button — gated by permission state
            when {
                micPermission.status.isGranted -> {
                    MicButton(
                        isListening = speechState is RecognitionState.Listening ||
                                speechState is RecognitionState.Partial,
                        onClick = {
                            if (speechState is RecognitionState.Listening ||
                                speechState is RecognitionState.Partial
                            ) viewModel.stopListening()
                            else viewModel.startListening()
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when (speechState) {
                            is RecognitionState.Listening -> "Listening…"
                            is RecognitionState.Partial -> "Recognizing…"
                            else -> "Tap mic to speak"
                        },
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
                micPermission.status.shouldShowRationale -> {
                    Button(onClick = { micPermission.launchPermissionRequest() }) {
                        Text("Allow microphone for voice input")
                    }
                }
                else -> {
                    Button(
                        onClick = { micPermission.launchPermissionRequest() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                    ) {
                        Text("Enable microphone in Settings")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Text input — always visible regardless of permission
            OutlinedTextField(
                value = symptomText,
                onValueChange = viewModel::updateSymptomText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(120.dp),
                label = { Text("Describe your symptoms") },
                placeholder = { Text("e.g. I have a headache and feel nauseous") },
                maxLines = 6
            )

            Spacer(Modifier.height(16.dp))

            // Body part selector
            Text("Body area (optional)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BODY_PARTS) { part ->
                    FilterChip(
                        selected = selectedBodyPart == part,
                        onClick = { viewModel.selectBodyPart(if (selectedBodyPart == part) null else part) },
                        label = { Text(part) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Submit button
            when (uiState) {
                is InputUiState.Processing -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Analyzing symptoms…", color = Color.Gray)
                }
                else -> {
                    Button(
                        onClick = {
                            viewModel.submitSymptoms { result ->
                                onTriageResult(result, symptomText)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(52.dp),
                        enabled = symptomText.isNotBlank() && uiState !is InputUiState.Processing
                    ) {
                        Text("Get Triage Advice", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (uiState is InputUiState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (uiState as InputUiState.Error).message,
                    color = Color(0xFFB71C1C),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text(
                "For emergencies always call 911",
                color = Color(0xFFB71C1C),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
