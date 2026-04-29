package com.example.medicalsymptomprescreener.ui.facilities

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.medicalsymptomprescreener.domain.model.CareType
import com.example.medicalsymptomprescreener.ui.components.DisclaimerCard
import com.example.medicalsymptomprescreener.ui.components.FacilityCard
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.LocationServices

/**
 * Displays nearby medical facilities matching the triage-recommended [CareType].
 *
 * Requests `ACCESS_FINE_LOCATION` permission on launch. Once granted, calls
 * `FusedLocationProviderClient.lastLocation` and passes the result to [FacilitiesViewModel].
 *
 * State rendering:
 * - [FacilitiesUiState.Loading] → spinner
 * - [FacilitiesUiState.Success] → [LazyColumn] of [FacilityCard] items
 * - [FacilitiesUiState.Empty] / [FacilitiesUiState.Error] → [RuralFallbackCard]
 *   with 911 dial button and Google Maps search link
 * - [FacilitiesUiState.SkipSearch] → no-op (router in [MainActivity] should have
 *   sent the user to [GuidanceScreen] for TELEHEALTH/HOME_CARE)
 *
 * @param careType The recommended care type. Determines which Places API types to search.
 * @param onBack Navigation callback for the back arrow.
 * @param viewModel [FacilitiesViewModel] injected by Hilt.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun FacilitiesScreen(
    careType: CareType,
    onBack: () -> Unit,
    viewModel: FacilitiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val locationPermission = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermission.status.isGranted) {
        if (locationPermission.status.isGranted) {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            fusedClient.lastLocation.addOnSuccessListener { location ->
                viewModel.loadFacilities(location, careType)
            }.addOnFailureListener {
                viewModel.loadFacilities(null, careType)
            }
        } else {
            locationPermission.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nearby Facilities") },
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
        ) {
            DisclaimerCard()

            when (val state = uiState) {
                is FacilitiesUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is FacilitiesUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item { Spacer(Modifier.height(8.dp)) }
                        items(state.facilities) { facility ->
                            FacilityCard(facility = facility)
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }
                }
                is FacilitiesUiState.Empty, is FacilitiesUiState.Error -> {
                    RuralFallbackCard(
                        message = if (state is FacilitiesUiState.Error) state.message
                        else "No nearby facilities found within 10km."
                    )
                }
                is FacilitiesUiState.SkipSearch -> {
                    // Should not reach here — router sends to GuidanceScreen instead
                }
            }
        }
    }
}

/**
 * Fallback UI shown when no facilities are found or a Places API error occurs.
 *
 * Provides a 911 dial button and a Google Maps search link to ensure users in rural areas
 * or with API failures always have an actionable next step.
 *
 * @param message Contextual message explaining why the fallback is shown.
 */
@Composable
private fun RuralFallbackCard(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(Modifier.height(16.dp))
        Text("Options:", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        val context = LocalContext.current

        Button(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:911")))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📞 Call 911 for Emergencies", color = Color.White)
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = {
                val uri = Uri.parse("https://www.google.com/maps/search/urgent+care+near+me")
                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔍 Search on Google Maps")
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Consider a telehealth virtual visit if your symptoms are not an emergency.",
            textAlign = TextAlign.Center,
            fontSize = 13.sp,
            color = Color.Gray
        )
    }
}
