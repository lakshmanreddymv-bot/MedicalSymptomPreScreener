package com.example.medicalsymptomprescreener.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Settings screen providing the language toggle (English / Spanish).
 *
 * Follows the same Compose + Scaffold + TopAppBar pattern as [InputScreen],
 * [HistoryScreen], and other screens in the app.
 *
 * **Feature flag:** The Spanish switch is OFF by default. The user must explicitly
 * enable it. When enabled, [SettingsViewModel.setSpanishEnabled] persists the
 * preference and eagerly triggers ML Kit model download.
 *
 * **Navigation:** Accessed via the "settings" NavGraph route added in [MainActivity].
 * A back arrow navigates to the previous screen.
 *
 * @param onBack Navigation callback — pops this screen off the back stack.
 * @param viewModel [SettingsViewModel] injected by Hilt. Overridable for previews.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isSpanishEnabled by viewModel.isSpanishEnabled.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            // Section header
            Text(
                text = "Language",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Language preference card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {

                    // English row (always shown — tapping disables Spanish)
                    LanguageRow(
                        flag = "🇺🇸",
                        label = "English",
                        sublabel = "Default",
                        isSelected = !isSpanishEnabled,
                        showSwitch = false,
                        onToggle = { viewModel.setSpanishEnabled(false) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                    // Spanish row — the feature flag toggle
                    LanguageRow(
                        flag = "🇪🇸",
                        label = "Español",
                        sublabel = "Powered by Google ML Kit • On-device",
                        isSelected = isSpanishEnabled,
                        showSwitch = true,
                        onToggle = { viewModel.setSpanishEnabled(it) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Informational note
            Text(
                text = "AI triage responses will be translated on-device. Emergency alerts (Call 911) work in both languages. A one-time model download (~20 MB) is required when Spanish is first enabled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

/**
 * A single language option row inside the language preference card.
 *
 * @param flag      Flag emoji for the locale.
 * @param label     Language name (e.g. "English", "Español").
 * @param sublabel  Secondary descriptive text (e.g. "Default", "Powered by Google ML Kit").
 * @param isSelected Whether this language is currently active.
 * @param showSwitch Whether to render a [Switch] toggle (Spanish row only).
 * @param onToggle  Callback invoked with the new toggle state (or `true`/`false` for row tap).
 */
@Composable
private fun LanguageRow(
    flag: String,
    label: String,
    sublabel: String,
    isSelected: Boolean,
    showSwitch: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = flag, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = sublabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showSwitch) {
            Switch(
                checked = isSelected,
                onCheckedChange = onToggle
            )
        }
    }
}
