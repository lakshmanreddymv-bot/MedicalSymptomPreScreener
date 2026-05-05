package com.example.medicalsymptomprescreener

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.ui.facilities.FacilitiesScreen
import com.example.medicalsymptomprescreener.ui.guidance.GuidanceScreen
import com.example.medicalsymptomprescreener.ui.history.HistoryScreen
import com.example.medicalsymptomprescreener.ui.input.InputScreen
import com.example.medicalsymptomprescreener.ui.settings.SettingsScreen
import com.example.medicalsymptomprescreener.ui.theme.MedicalSymptomPreScreenerTheme
import com.example.medicalsymptomprescreener.ui.triage.SharedTriageViewModel
import com.example.medicalsymptomprescreener.ui.triage.TriageScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single Activity entry point for the Medical Symptom Pre-Screener.
 *
 * Hosts the full [NavHost] with six destinations:
 * - `input`                  → [InputScreen]: symptom entry via text or voice
 * - `triage`                 → [TriageScreen]: validated triage result display
 * - `facilities/{careType}`  → [FacilitiesScreen]: nearby facility search
 * - `guidance/{careType}`    → [GuidanceScreen]: telehealth/home care guidance
 * - `history`                → [HistoryScreen]: saved assessment history
 * - `settings`               → [SettingsScreen]: language preference (EN / ES) toggle
 *
 * [SharedTriageViewModel] is scoped to the [NavHost] composable (effectively the Activity)
 * so that it is shared across all destinations without re-creation on navigation.
 *
 * The routing logic for TELEHEALTH/HOME_CARE → [GuidanceScreen] vs.
 * EMERGENCY_ROOM/URGENT_CARE/etc. → [FacilitiesScreen] lives here so that
 * neither [TriageScreen] nor [SharedTriageViewModel] need to know about navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedicalSymptomPreScreenerTheme {
                val navController = rememberNavController()

                // SharedTriageViewModel scoped to the NavHost — survives screen navigation
                val sharedVm: SharedTriageViewModel = hiltViewModel()
                val triageResult by sharedVm.triageResult.collectAsState()

                NavHost(navController = navController, startDestination = "input") {

                    composable("input") {
                        InputScreen(
                            onTriageResult = { result, symptomText ->
                                sharedVm.setResult(symptomText, result)
                                navController.navigate("triage")
                            },
                            onOpenSettings = { navController.navigate("settings") }
                        )
                    }

                    composable("triage") {
                        TriageScreen(
                            sharedViewModel = sharedVm,
                            onFindFacilities = {
                                val careType = triageResult?.recommendedCareType ?: CareType.URGENT_CARE
                                // Route TELEHEALTH/HOME_CARE to GuidanceScreen — no Places API call
                                if (careType == CareType.TELEHEALTH || careType == CareType.HOME_CARE) {
                                    navController.navigate("guidance/${careType.name}")
                                } else {
                                    navController.navigate("facilities/${careType.name}")
                                }
                            },
                            onNewAssessment = {
                                navController.popBackStack("input", inclusive = false)
                            }
                        )
                    }

                    composable("facilities/{careType}") { backStackEntry ->
                        val careType = runCatching {
                            CareType.valueOf(backStackEntry.arguments?.getString("careType") ?: "URGENT_CARE")
                        }.getOrDefault(CareType.URGENT_CARE)
                        FacilitiesScreen(
                            careType = careType,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable("guidance/{careType}") { backStackEntry ->
                        val careType = runCatching {
                            CareType.valueOf(backStackEntry.arguments?.getString("careType") ?: "TELEHEALTH")
                        }.getOrDefault(CareType.TELEHEALTH)
                        GuidanceScreen(
                            careType = careType,
                            onBack = { navController.popBackStack() },
                            onNewAssessment = {
                                sharedVm.clear()
                                navController.popBackStack("input", inclusive = false)
                            }
                        )
                    }

                    composable("history") {
                        HistoryScreen(onBack = { navController.popBackStack() })
                    }

                    composable("settings") {
                        SettingsScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}
