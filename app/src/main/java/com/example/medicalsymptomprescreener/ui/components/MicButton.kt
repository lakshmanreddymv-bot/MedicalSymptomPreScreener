package com.example.medicalsymptomprescreener.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Animated microphone button for voice symptom input.
 *
 * When [isListening] is `true`:
 * - Button color changes from blue to red
 * - Icon changes from [Mic] to [MicOff]
 * - A pulse animation scales the button 1.0x → 1.15x → 1.0x at 600 ms intervals
 *   to provide clear visual feedback that recording is active
 *
 * Only shown in [InputScreen] when `RECORD_AUDIO` permission is granted.
 *
 * @param isListening Whether voice recognition is currently active.
 * @param onClick Toggles listening on/off. Caller handles start/stop logic in [InputViewModel].
 * @param modifier Optional [Modifier] for positioning within the parent layout.
 */
@Composable
fun MicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale = if (isListening) {
        val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
        val pulse by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        pulse
    } else 1f

    FilledIconButton(
        onClick = onClick,
        modifier = modifier
            .size(80.dp)
            .scale(scale),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = if (isListening) Color(0xFFE53935) else Color(0xFF1976D2)
        )
    ) {
        Icon(
            imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = if (isListening) "Stop listening" else "Start voice input",
            modifier = Modifier.size(36.dp),
            tint = Color.White
        )
    }
}
