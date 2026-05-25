package com.example.mobileclient.ui.utils

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import com.example.mobileclient.data.HapticManager

fun Modifier.hapticClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val context = LocalContext.current
    this.clickable(
        enabled = enabled,
        onClick = {
            HapticManager.performTapHaptic(context)
            onClick()
        }
    )
}
