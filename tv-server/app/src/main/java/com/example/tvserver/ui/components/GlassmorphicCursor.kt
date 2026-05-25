package com.example.tvserver.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.tvserver.ui.utils.appleShadow

@Composable
fun GlassmorphicCursor(
    sizeDp: Int = 32,
    cursorColor: Color = Color.White,
    opacity: Float = 0.6f,
    isPressed: Boolean = false
) {
    val scaleFactor by animateFloatAsState(
        targetValue = if (isPressed) 0.78f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 1500f),
        label = "cursorScale"
    )

    Canvas(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scaleFactor
                scaleY = scaleFactor
            }
            .size(sizeDp.dp)
            .appleShadow(elevation = 8.dp, alpha = 0.15f)
    ) {
        val radius = size.minDimension / 2f
        val centerOffset = Offset(size.width / 2f, size.height / 2f)

        val glassBrush = Brush.radialGradient(
            colors = listOf(
                cursorColor.copy(alpha = (if (isPressed) 1.5f else 1.2f) * opacity),
                cursorColor.copy(alpha = opacity * 0.4f),
                cursorColor.copy(alpha = opacity * 0.1f)
            ),
            center = centerOffset,
            radius = radius
        )

        // Draw radial glass fill
        drawCircle(
            brush = glassBrush,
            radius = radius,
            center = centerOffset
        )

        // Draw bright inner border
        drawCircle(
            color = Color.White.copy(alpha = if (isPressed) 0.85f else 0.55f),
            radius = radius - 1f,
            center = centerOffset,
            style = Stroke(width = 1.dp.toPx())
        )

        // Draw soft dark outer rim
        drawCircle(
            color = Color.Black.copy(alpha = 0.12f),
            radius = radius,
            center = centerOffset,
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
