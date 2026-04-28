package com.example.medicalsymptomprescreener.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.medicalsymptomprescreener.domain.model.NearbyFacility

@Composable
fun FacilityCard(facility: NearbyFacility, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = facility.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Text(text = facility.address, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                facility.isOpen?.let {
                    Text(
                        text = if (it) "Open now" else "Closed",
                        color = if (it) Color(0xFF2E7D32) else Color.Gray,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.width(8.dp))
                }
                facility.rating?.let {
                    Text(text = "★ ${"%.1f".format(it)}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = "${"%.1f".format(facility.distanceKm)} km",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            Row(modifier = Modifier.padding(top = 8.dp)) {
                facility.phone?.let { phone ->
                    IconButton(onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    }) {
                        Icon(Icons.Default.Phone, contentDescription = "Call facility")
                    }
                }
                IconButton(onClick = {
                    val uri = Uri.parse("https://www.google.com/maps/place/?q=place_id:${facility.mapsPlaceId}")
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                }) {
                    Icon(Icons.Default.Directions, contentDescription = "Get directions")
                }
            }
        }
    }
}
