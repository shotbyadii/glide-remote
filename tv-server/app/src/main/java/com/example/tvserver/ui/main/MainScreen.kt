package com.example.tvserver.ui.main

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.tvserver.*
import com.example.tvserver.theme.AppTheme
import com.example.tvserver.ui.utils.appleShadow
import java.net.NetworkInterface
import java.util.Collections
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var isServerRunning by remember { mutableStateOf(isServiceRunning(context, RemoteService::class.java)) }
    var isOverlayRunning by remember { mutableStateOf(isServiceRunning(context, CursorOverlayService::class.java)) }
    var isAccessibilityActive by remember { mutableStateOf(GtvAccessibilityService.isServiceRunning()) }
    var isAdbModeActive by remember { mutableStateOf(CursorOverlayService.isAdbMode) }

    var sizeSliderVal by remember { mutableFloatStateOf(CursorOverlayService.cursorSize.toFloat()) }
    var opacitySliderVal by remember { mutableFloatStateOf(CursorOverlayService.cursorOpacity) }

    val xHistory = remember { mutableStateListOf<Float>() }
    val yHistory = remember { mutableStateListOf<Float>() }
    var lastDx by remember { mutableFloatStateOf(0f) }
    var lastDy by remember { mutableFloatStateOf(0f) }
    var packetCount by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        val originalCallback = RemoteServiceCallbacks.onPositionUpdated
        RemoteServiceCallbacks.onPositionUpdated = { dx, dy ->
            originalCallback?.invoke(dx, dy)
            lastDx = dx
            lastDy = dy
            packetCount++
            xHistory.add(dx)
            if (xHistory.size > 80) {
                xHistory.removeAt(0)
            }
            yHistory.add(dy)
            if (yHistory.size > 80) {
                yHistory.removeAt(0)
            }
        }
        onDispose {
            RemoteServiceCallbacks.onPositionUpdated = originalCallback
        }
    }

    val localIp = remember { getWifiIpAddress() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AppTheme.colors.background
    ) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // LEFT COLUMN: Time-Based Rolling Input Oscilloscope Panel
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
                    .appleShadow(elevation = 12.dp, alpha = 0.15f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.card)
                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "[ REAL-TIME INPUT ROLLING SCOPE (TOP: X / BOT: Y) ]",
                        style = AppTheme.typography.caption.copy(
                            color = AppTheme.colors.foreground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (packetCount > 0) AppTheme.colors.primary else AppTheme.colors.mutedForeground)
                    )
                }

                // Visual Oscilloscope Canvas — capture colors before entering draw scope
                val scopeBorderColor = AppTheme.colors.border
                val scopeFgColor = AppTheme.colors.foreground
                val scopeMutedColor = AppTheme.colors.mutedForeground
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(AppTheme.colors.background)
                        .border(1.dp, AppTheme.colors.border, RoundedCornerShape(4.dp))
                ) {
                    val w = size.width
                    val h = size.height
                    val halfH = h / 2f
                    val baseX = halfH / 2f
                    val baseY = halfH + (halfH / 2f)

                    // Draw center and baseline grid lines
                    drawLine(
                        color = scopeBorderColor.copy(alpha = 0.2f),
                        start = Offset(0f, baseX),
                        end = Offset(w, baseX),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = scopeBorderColor.copy(alpha = 0.2f),
                        start = Offset(0f, baseY),
                        end = Offset(w, baseY),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawLine(
                        color = scopeBorderColor.copy(alpha = 0.4f),
                        start = Offset(0f, halfH),
                        end = Offset(w, halfH),
                        strokeWidth = 1.5.dp.toPx()
                    )

                    // Draw vertical grids
                    val divisions = 8
                    for (i in 1 until divisions) {
                        val xPos = w * (i.toFloat() / divisions)
                        drawLine(
                            color = scopeBorderColor.copy(alpha = 0.1f),
                            start = Offset(xPos, 0f),
                            end = Offset(xPos, h),
                            strokeWidth = 0.8.dp.toPx()
                        )
                    }

                    // Plot X Movements (Top sub-chart)
                    if (xHistory.size > 1) {
                        val points = xHistory.toList()
                        for (idx in 0 until points.size - 1) {
                            val val1 = points[idx]
                            val val2 = points[idx + 1]

                            val startX = (idx.toFloat() / 80f) * w
                            val startY = baseX - ((val1 / 15f) * (halfH / 2f))
                            val endX = ((idx + 1).toFloat() / 80f) * w
                            val endY = baseX - ((val2 / 15f) * (halfH / 2f))

                            val alpha = (idx.toFloat() / points.size) * 0.8f
                            drawCircle(
                                color = scopeFgColor.copy(alpha = alpha),
                                radius = 1.5.dp.toPx(),
                                center = Offset(endX, endY)
                            )
                            drawLine(
                                color = scopeFgColor.copy(alpha = alpha),
                                start = Offset(startX, startY.coerceIn(0f, halfH)),
                                end = Offset(endX, endY.coerceIn(0f, halfH)),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }

                    // Plot Y Movements (Bottom sub-chart)
                    if (yHistory.size > 1) {
                        val points = yHistory.toList()
                        for (idx in 0 until points.size - 1) {
                            val val1 = points[idx]
                            val val2 = points[idx + 1]

                            val startX = (idx.toFloat() / 80f) * w
                            val startY = baseY - ((val1 / 15f) * (halfH / 2f))
                            val endX = ((idx + 1).toFloat() / 80f) * w
                            val endY = baseY - ((val2 / 15f) * (halfH / 2f))

                            val alpha = (idx.toFloat() / points.size) * 0.8f
                            drawCircle(
                                color = scopeMutedColor.copy(alpha = alpha),
                                radius = 1.5.dp.toPx(),
                                center = Offset(endX, endY)
                            )
                            drawLine(
                                color = scopeMutedColor.copy(alpha = alpha),
                                start = Offset(startX, startY.coerceIn(halfH, h)),
                                end = Offset(endX, endY.coerceIn(halfH, h)),
                                strokeWidth = 1.5.dp.toPx()
                            )
                        }
                    }
                }

                // Telemetry text printouts
                Column {
                    Text(
                        text = String.format("LATEST INPUT: dX=%+.2f, dY=%+.2f", lastDx, lastDy),
                        style = AppTheme.typography.caption.copy(
                            color = AppTheme.colors.mutedForeground,
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = String.format("ACCEL/FRIC: A=%.2f, F=%.2f", CursorOverlayService.acceleration, CursorOverlayService.momentumFriction),
                        style = AppTheme.typography.caption.copy(
                            color = AppTheme.colors.mutedForeground,
                            fontSize = 9.sp
                        )
                    )
                    Text(
                        text = String.format("TELEMETRY PKTS: %d", packetCount),
                        style = AppTheme.typography.caption.copy(
                            color = AppTheme.colors.mutedForeground,
                            fontSize = 9.sp
                        )
                    )
                }
            }

            // RIGHT COLUMN: System Info & Sliders & Lab Button
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // TV Status Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleShadow(elevation = 8.dp, alpha = 0.15f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.colors.card)
                        .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TV HYBRID REMOTE SERVER",
                        style = AppTheme.typography.caption.copy(
                            color = AppTheme.colors.foreground,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Server IP: $localIp",
                            style = AppTheme.typography.body.copy(
                                color = AppTheme.colors.mutedForeground,
                                fontSize = 13.sp
                            )
                        )
                        val serverStatusText = if (isServerRunning) "RUNNING" else "STOPPED"
                        val serverStatusColor = if (isServerRunning) AppTheme.colors.foreground else AppTheme.colors.mutedForeground
                        Text(
                            text = serverStatusText,
                            style = AppTheme.typography.body.copy(
                                color = serverStatusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val serverInteractionSource = remember { MutableInteractionSource() }
                        val isServerBtnFocused by serverInteractionSource.collectIsFocusedAsState()
                        val isServerBtnHovered by serverInteractionSource.collectIsHoveredAsState()
                        val isServerBtnActive = isServerBtnFocused || isServerBtnHovered
                        val serverBtnScale by animateFloatAsState(if (isServerBtnActive) 1.05f else 1.0f)
                        val serverBtnBg by animateColorAsState(if (isServerBtnActive) AppTheme.colors.primary else AppTheme.colors.card)
                        val serverBtnBorder by animateColorAsState(if (isServerBtnActive) AppTheme.colors.primary else AppTheme.colors.border)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .scale(serverBtnScale)
                                .clip(RoundedCornerShape(8.dp))
                                .background(serverBtnBg)
                                .border(1.dp, serverBtnBorder, RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = serverInteractionSource,
                                    indication = null,
                                    onClick = {
                                        if (isServerRunning) {
                                            context.stopService(Intent(context, RemoteService::class.java))
                                            context.stopService(Intent(context, CursorOverlayService::class.java))
                                        } else {
                                            context.startService(Intent(context, RemoteService::class.java))
                                            if (Settings.canDrawOverlays(context)) {
                                                context.startService(Intent(context, CursorOverlayService::class.java))
                                            } else {
                                                val intent = Intent(
                                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    Uri.parse("package:" + context.packageName)
                                                ).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            }
                                        }
                                        isServerRunning = !isServerRunning
                                        isOverlayRunning = isServerRunning
                                    }
                                )
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = if (isServerRunning) "STOP SERVER" else "START SERVER",
                                style = AppTheme.typography.caption.copy(
                                    color = if (isServerBtnActive) AppTheme.colors.primaryForeground else AppTheme.colors.foreground,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        val accessInteractionSource = remember { MutableInteractionSource() }
                        val isAccessBtnFocused by accessInteractionSource.collectIsFocusedAsState()
                        val isAccessBtnHovered by accessInteractionSource.collectIsHoveredAsState()
                        val isAccessBtnActive = isAccessBtnFocused || isAccessBtnHovered
                        val accessBtnScale by animateFloatAsState(if (isAccessBtnActive) 1.05f else 1.0f)
                        val accessBtnBg by animateColorAsState(if (isAccessBtnActive) AppTheme.colors.primary else AppTheme.colors.card)
                        val accessBtnBorder by animateColorAsState(if (isAccessBtnActive) AppTheme.colors.primary else AppTheme.colors.border)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .scale(accessBtnScale)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accessBtnBg)
                                .border(1.dp, accessBtnBorder, RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = accessInteractionSource,
                                    indication = null,
                                    onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        try {
                                            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e2: Exception) {
                                            android.widget.Toast.makeText(
                                                context,
                                                "Could not open Settings. Please open them manually via TV Settings -> System -> Accessibility.",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                                }
                                )
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "ACCESSIBILITY",
                                style = AppTheme.typography.caption.copy(
                                    color = if (isAccessBtnActive) AppTheme.colors.primaryForeground else AppTheme.colors.foreground,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                // Cursor Parameters Sliders Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleShadow(elevation = 8.dp, alpha = 0.15f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.colors.card)
                        .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "OVERLAY CURSOR CONFIG",
                        style = AppTheme.typography.caption.copy(
                            color = AppTheme.colors.foreground,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    // Cursor Size
                    StepController(
                        label = "Cursor Size",
                        valueText = "${sizeSliderVal.roundToInt()} px",
                        onDecrement = {
                            sizeSliderVal = (sizeSliderVal - 4f).coerceIn(24f, 64f)
                            CursorOverlayService.cursorSize = sizeSliderVal.roundToInt()
                        },
                        onIncrement = {
                            sizeSliderVal = (sizeSliderVal + 4f).coerceIn(24f, 64f)
                            CursorOverlayService.cursorSize = sizeSliderVal.roundToInt()
                        }
                    )

                    // Cursor Opacity
                    StepController(
                        label = "Cursor Opacity",
                        valueText = "${(opacitySliderVal * 100).roundToInt()}%",
                        onDecrement = {
                            opacitySliderVal = (opacitySliderVal - 0.1f).coerceIn(0.2f, 1.0f)
                            opacitySliderVal = Math.round(opacitySliderVal * 10f) / 10f
                            CursorOverlayService.cursorOpacity = opacitySliderVal
                        },
                        onIncrement = {
                            opacitySliderVal = (opacitySliderVal + 0.1f).coerceIn(0.2f, 1.0f)
                            opacitySliderVal = Math.round(opacitySliderVal * 10f) / 10f
                            CursorOverlayService.cursorOpacity = opacitySliderVal
                        }
                    )
                }

                // ADB mode Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appleShadow(elevation = 8.dp, alpha = 0.15f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.colors.card)
                        .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "POINTER FALLBACK METHOD",
                            style = AppTheme.typography.caption.copy(
                                color = AppTheme.colors.foreground,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = if (isAdbModeActive) "Using ADB Shell coordinate tap injections." else "Using Accessibility Node coordinate actions.",
                            style = AppTheme.typography.body.copy(
                                color = AppTheme.colors.mutedForeground,
                                fontSize = 10.sp
                            )
                        )
                    }

                    val adbInteractionSource = remember { MutableInteractionSource() }
                    val isAdbBtnFocused by adbInteractionSource.collectIsFocusedAsState()
                    val isAdbBtnHovered by adbInteractionSource.collectIsHoveredAsState()
                    val isAdbBtnActive = isAdbBtnFocused || isAdbBtnHovered
                    val adbScale by animateFloatAsState(if (isAdbBtnActive) 1.05f else 1.0f)
                    val adbBg by animateColorAsState(if (isAdbModeActive) AppTheme.colors.primary else Color.Transparent)

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .scale(adbScale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(adbBg)
                            .border(1.dp, AppTheme.colors.border, RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = adbInteractionSource,
                                indication = null,
                                onClick = {
                                    isAdbModeActive = !isAdbModeActive
                                    CursorOverlayService.isAdbMode = isAdbModeActive
                                }
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isAdbModeActive) "ADB MODE" else "ACCESSIBILITY",
                            style = AppTheme.typography.caption.copy(
                                color = if (isAdbModeActive) AppTheme.colors.primaryForeground else AppTheme.colors.foreground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        )
                    }
                }

                // Keyboard Lab Button Card (The requested button)
                val labInteractionSource = remember { MutableInteractionSource() }
                val isLabFocused by labInteractionSource.collectIsFocusedAsState()
                val isLabHovered by labInteractionSource.collectIsHoveredAsState()
                val isLabActive = isLabFocused || isLabHovered
                val labScale by animateFloatAsState(if (isLabActive) 1.05f else 1.0f)
                val labBg by animateColorAsState(if (isLabActive) AppTheme.colors.primary else AppTheme.colors.card)
                val labBorderColor by animateColorAsState(if (isLabActive) AppTheme.colors.primary else AppTheme.colors.border)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(labScale)
                        .appleShadow(elevation = 8.dp, alpha = 0.15f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(labBg)
                        .border(1.dp, labBorderColor, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = labInteractionSource,
                            indication = null,
                            onClick = { onItemClick(KeyboardTest) }
                        )
                        .padding(vertical = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⌨ OPEN MOUSE & KEYBOARD TEST LAB",
                            style = AppTheme.typography.body.copy(
                                color = if (isLabActive) AppTheme.colors.primaryForeground else AppTheme.colors.foreground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepController(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AppTheme.typography.body.copy(
                color = AppTheme.colors.mutedForeground,
                fontSize = 13.sp
            )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val decInteractionSource = remember { MutableInteractionSource() }
            val isDecFocused by decInteractionSource.collectIsFocusedAsState()
            val isDecHovered by decInteractionSource.collectIsHoveredAsState()
            val isDecActive = isDecFocused || isDecHovered
            val decScale by animateFloatAsState(if (isDecActive) 1.1f else 1.0f)
            val decBg by animateColorAsState(if (isDecActive) AppTheme.colors.primary else Color.Transparent)
            val decTextColor = if (isDecActive) AppTheme.colors.primaryForeground else AppTheme.colors.foreground

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(decScale)
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(decBg)
                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = decInteractionSource,
                        indication = null,
                        onClick = onDecrement
                    )
            ) {
                Text(
                    text = "-",
                    style = AppTheme.typography.body.copy(
                        color = decTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }

            Text(
                text = valueText,
                style = AppTheme.typography.body.copy(
                    color = AppTheme.colors.foreground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                ),
                modifier = Modifier.width(60.dp),
                textAlign = TextAlign.Center
            )

            val incInteractionSource = remember { MutableInteractionSource() }
            val isIncFocused by incInteractionSource.collectIsFocusedAsState()
            val isIncHovered by incInteractionSource.collectIsHoveredAsState()
            val isIncActive = isIncFocused || isIncHovered
            val incScale by animateFloatAsState(if (isIncActive) 1.1f else 1.0f)
            val incBg by animateColorAsState(if (isIncActive) AppTheme.colors.primary else Color.Transparent)
            val incTextColor = if (isIncActive) AppTheme.colors.primaryForeground else AppTheme.colors.foreground

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(incScale)
                    .size(28.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(incBg)
                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(4.dp))
                    .clickable(
                        interactionSource = incInteractionSource,
                        indication = null,
                        onClick = onIncrement
                    )
            ) {
                Text(
                    text = "+",
                    style = AppTheme.typography.body.copy(
                        color = incTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                )
            }
        }
    }
}

private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Int.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}

private fun getWifiIpAddress(): String {
    try {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (networkInterface in interfaces) {
            val addresses = Collections.list(networkInterface.inetAddresses)
            for (address in addresses) {
                if (!address.isLoopbackAddress) {
                    val sAddr = address.hostAddress
                    if (sAddr != null) {
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4) return sAddr
                    }
                }
            }
        }
    } catch (e: Exception) {
        // Log error
    }
    return "127.0.0.1"
}
