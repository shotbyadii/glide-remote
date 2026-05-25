package com.example.tvserver.ui.utils

import android.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.appleShadow(
    elevation: Dp = 12.dp,
    alpha: Float = 0.05f
): Modifier = this.drawBehind {
    val elevationPx = elevation.toPx()
    val shadowOffsetPx = (elevation / 3).toPx()
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val nativePaint = paint.asFrameworkPaint()
        nativePaint.color = Color.TRANSPARENT
        nativePaint.setShadowLayer(
            elevationPx,
            0.0f,
            shadowOffsetPx,
            Color.argb((255 * alpha).toInt(), 0, 0, 0)
        )
        canvas.drawRect(
            0.0f,
            0.0f,
            size.width,
            size.height,
            paint
        )
    }
}
