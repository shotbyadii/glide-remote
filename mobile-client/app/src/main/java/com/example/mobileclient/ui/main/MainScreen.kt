package com.example.mobileclient.ui.main

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import com.example.mobileclient.Settings
import com.example.mobileclient.data.ProfileManager
import com.example.mobileclient.data.PhysicsProfile
import com.example.mobileclient.data.RemoteClient
import com.example.mobileclient.data.SensorEngine
import com.example.mobileclient.data.HapticManager
import com.example.mobileclient.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 1. Sockets & Sensors Initialization
    val remoteClient = remember { RemoteClient(context) }
    val sensorEngine = remember {
        SensorEngine(context) { dx, dy ->
            remoteClient.sendMotion(dx, dy, 2.toByte())
        }
    }

    // 2. State management
    var isConnected by remember { mutableStateOf(false) }
    var currentTab by remember { mutableIntStateOf(0) } // 0 = Pad, 1 = 3D Mouse, 2 = TV Controls
    var isAirMouseToggled by remember { mutableStateOf(false) }

    // Physics values
    val lastProfile = remember { ProfileManager.loadLastValues(context) }
    var trackpadSensitivity by remember { mutableFloatStateOf(lastProfile.trackpadSensitivity) }
    var pointerSensitivity by remember { mutableFloatStateOf(lastProfile.sensitivity) }
    var pointerAcceleration by remember { mutableFloatStateOf(lastProfile.acceleration) }
    var pointerMomentum by remember { mutableFloatStateOf(lastProfile.friction) }

    // Dialog & UI expansion
    var showSaveDialog by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    var savedProfiles by remember { mutableStateOf(ProfileManager.listProfiles(context)) }
    var showProfilePicker by remember { mutableStateOf(false) }
    var showManualIpDialog by remember { mutableStateOf(false) }
    var manualIpText by remember { mutableStateOf("") }
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var isGyroAdvancedExpanded by remember { mutableStateOf(false) }
    var isCursorEnabled by remember { mutableStateOf(true) }

    // TV Control values
    var tvVolume by remember { mutableFloatStateOf(7.0f) }
    var tvPictureMode by remember { mutableStateOf("STANDARD") }

    val connectedDeviceName by remember { mutableStateOf("Google TV") }

    // Auto-save helper
    fun autoSave() {
        ProfileManager.saveLastValues(
            context,
            pointerSensitivity,
            pointerAcceleration,
            pointerMomentum,
            trackpadSensitivity
        )
    }

    // Connect lifecycle callbacks
    DisposableEffect(Unit) {
        remoteClient.onConnectionStateChanged = { state ->
            isConnected = state
            if (state && isAirMouseToggled) {
                sensorEngine.start()
            } else {
                sensorEngine.stop()
            }
        }
        remoteClient.startDiscovery()
        onDispose {
            remoteClient.disconnect()
            sensorEngine.stop()
        }
    }

    // Pulse animation for connection indicator
    val infiniteTransition = rememberInfiniteTransition("pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Layout Scaffold
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppTheme.colors.background,
        topBar = {
            // Elegant topbar with connection status and direct Gear/Settings button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Connection indicator panel (tap to open manual IP configuration)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AppTheme.colors.card)
                        .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                        .clickable {
                            HapticManager.performTapHaptic(context)
                            manualIpText = ProfileManager.loadLastIp(context)
                            showManualIpDialog = true
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .alpha(if (isConnected) 1.0f else pulseAlpha)
                            .background(if (isConnected) Color(0xFF10B981) else Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isConnected) connectedDeviceName else "Searching TV...",
                            color = AppTheme.colors.foreground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isConnected) {
                            Text(
                                text = remoteClient.tvIpAddress ?: "Connected",
                                color = AppTheme.colors.mutedForeground,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Settings icon button
                IconButton(
                    onClick = {
                        HapticManager.performTapHaptic(context)
                        onItemClick(Settings)
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(AppTheme.colors.card)
                        .border(1.dp, AppTheme.colors.border, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = AppTheme.colors.foreground
                    )
                }
            }
        },
        bottomBar = {
            // Three Tabs at the bottom
            NavigationBar(
                containerColor = AppTheme.colors.background,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                NavigationBarItem(
                    selected = (currentTab == 0),
                    onClick = {
                        HapticManager.performTapHaptic(context)
                        currentTab = 0
                        sensorEngine.stop()
                        isAirMouseToggled = false
                    },
                    label = { Text("Precision Pad", color = AppTheme.colors.foreground) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(if (currentTab == 0) AppTheme.colors.primary else Color.Transparent, CircleShape)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = AppTheme.colors.card
                    )
                )

                NavigationBarItem(
                    selected = (currentTab == 1),
                    onClick = {
                        HapticManager.performTapHaptic(context)
                        currentTab = 1
                    },
                    label = { Text("Air Mouse", color = AppTheme.colors.foreground) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(if (currentTab == 1) AppTheme.colors.primary else Color.Transparent, CircleShape)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = AppTheme.colors.card
                    )
                )

                NavigationBarItem(
                    selected = (currentTab == 2),
                    onClick = {
                        HapticManager.performTapHaptic(context)
                        currentTab = 2
                        sensorEngine.stop()
                        isAirMouseToggled = false
                    },
                    label = { Text("TV Dashboard", color = AppTheme.colors.foreground) },
                    icon = {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(if (currentTab == 2) AppTheme.colors.primary else Color.Transparent, CircleShape)
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = AppTheme.colors.card
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> {
                    // TAB 0: PRECISION PAD
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Advanced expansion button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.colors.card)
                                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                                .clickable {
                                    HapticManager.performTapHaptic(context)
                                    isAdvancedExpanded = !isAdvancedExpanded
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Trackpad Settings",
                                color = AppTheme.colors.foreground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isAdvancedExpanded) "Collapse ▲" else "Expand ▼",
                                color = AppTheme.colors.primary,
                                fontSize = 12.sp
                            )
                        }

                        if (isAdvancedExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.card),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = "Touch Sensitivity: ${"%.1f".format(trackpadSensitivity)}",
                                        color = AppTheme.colors.foreground,
                                        fontSize = 13.sp
                                    )
                                    Slider(
                                        value = trackpadSensitivity,
                                        onValueChange = {
                                            trackpadSensitivity = it
                                            autoSave()
                                        },
                                        valueRange = 0.5f..5.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = AppTheme.colors.primary,
                                            activeTrackColor = AppTheme.colors.primary
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Cyber grid touchpad
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .background(AppTheme.colors.background)
                                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(24.dp))
                                .cyberGrid()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onTap = {
                                            remoteClient.sendClick(true)
                                            remoteClient.sendClick(false)
                                            HapticManager.performTapHaptic(context)
                                        }
                                    )
                                }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val dx = dragAmount.x * trackpadSensitivity
                                            val dy = dragAmount.y * trackpadSensitivity
                                            remoteClient.sendMotion(dx, dy, 1.toByte()) // Touchpad motion
                                            HapticManager.performHoverHaptic(context)
                                        }
                                    )
                                }
                        ) {
                            Text(
                                text = "PRECISION TRACKPAD\nSlide to aim • Tap to click",
                                color = AppTheme.colors.mutedForeground,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // System key rows (Back, Menu, Home)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    HapticManager.performTapHaptic(context)
                                    remoteClient.sendSystemKey("BACK")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.card),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(20.dp))
                            ) {
                                Text("Back", color = AppTheme.colors.foreground)
                            }
                            Button(
                                onClick = {
                                    HapticManager.performTapHaptic(context)
                                    remoteClient.sendSystemKey("MENU")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.card),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(20.dp))
                            ) {
                                Text("Menu", color = AppTheme.colors.foreground)
                            }
                            Button(
                                onClick = {
                                    HapticManager.performTapHaptic(context)
                                    remoteClient.sendSystemKey("HOME")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.card),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(20.dp))
                            ) {
                                Text("Home", color = AppTheme.colors.foreground)
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: AIR MOUSE (3D GYRO MOUSE)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Air Mouse Main Toggle Button
                        Button(
                            onClick = {
                                HapticManager.performTapHaptic(context)
                                isAirMouseToggled = !isAirMouseToggled
                                sensorEngine.isAimingActive = isAirMouseToggled
                                if (isAirMouseToggled) {
                                    sensorEngine.start()
                                } else {
                                    sensorEngine.stop()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAirMouseToggled) Color(0xFF10B981) else Color(0xFFEF4444)
                            ),
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = if (isAirMouseToggled) "AIR MOUSE: ACTIVE" else "AIR MOUSE: INACTIVE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Clutch hold area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(AppTheme.colors.card)
                                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(20.dp))
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            sensorEngine.isAimingActive = true
                                            HapticManager.performTapHaptic(context)
                                            try {
                                                awaitRelease()
                                            } finally {
                                                sensorEngine.isAimingActive = isAirMouseToggled
                                                HapticManager.performHoverHaptic(context)
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CLUTCH AREA\nPress and hold to engage gyro",
                                color = AppTheme.colors.primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Reset / Profile Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    HapticManager.performTapHaptic(context)
                                    sensorEngine.resetCalibration()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.card),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(20.dp))
                            ) {
                                Text("Recalibrate", color = AppTheme.colors.foreground)
                            }

                            Button(
                                onClick = {
                                    HapticManager.performTapHaptic(context)
                                    showProfilePicker = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.card),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(20.dp))
                            ) {
                                Text("Profiles", color = AppTheme.colors.foreground)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Advanced Gyro physics parameters
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.colors.card)
                                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                                .clickable {
                                    HapticManager.performTapHaptic(context)
                                    isGyroAdvancedExpanded = !isGyroAdvancedExpanded
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Advanced Physics Profiles",
                                color = AppTheme.colors.foreground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isGyroAdvancedExpanded) "Collapse ▲" else "Expand ▼",
                                color = AppTheme.colors.primary,
                                fontSize = 12.sp
                            )
                        }

                        if (isGyroAdvancedExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = AppTheme.colors.card),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // Sensitivity Slider
                                    Text(
                                        text = "Gyro Sensitivity: ${"%.1f".format(pointerSensitivity)}",
                                        color = AppTheme.colors.foreground,
                                        fontSize = 13.sp
                                    )
                                    Slider(
                                        value = pointerSensitivity,
                                        onValueChange = {
                                            pointerSensitivity = it
                                            sensorEngine.sensitivityX = it
                                            sensorEngine.sensitivityY = it
                                            autoSave()
                                        },
                                        valueRange = 5.0f..100.0f
                                    )

                                    // Acceleration Slider
                                    Text(
                                        text = "Pointer Acceleration: ${"%.1f".format(pointerAcceleration)}",
                                        color = AppTheme.colors.foreground,
                                        fontSize = 13.sp
                                    )
                                    Slider(
                                        value = pointerAcceleration,
                                        onValueChange = {
                                            pointerAcceleration = it
                                            remoteClient.sendAcceleration(it)
                                            autoSave()
                                        },
                                        valueRange = 0.0f..5.0f
                                    )

                                    // Friction / Momentum Slider
                                    Text(
                                        text = "Pointer Friction: ${"%.1f".format(pointerMomentum)}",
                                        color = AppTheme.colors.foreground,
                                        fontSize = 13.sp
                                    )
                                    Slider(
                                        value = pointerMomentum,
                                        onValueChange = {
                                            pointerMomentum = it
                                            remoteClient.sendFriction(it)
                                            autoSave()
                                        },
                                        valueRange = 0.0f..5.0f
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = {
                                            HapticManager.performTapHaptic(context)
                                            showSaveDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Save Custom Physics Profile", color = AppTheme.colors.foreground)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: SMART TV CONTROLS DASHBOARD
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // System Cursor Switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(AppTheme.colors.card)
                                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Show TV Cursor Overlay",
                                color = AppTheme.colors.foreground,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(
                                checked = isCursorEnabled,
                                onCheckedChange = {
                                    HapticManager.performTapHaptic(context)
                                    isCursorEnabled = it
                                    if (isCursorEnabled) {
                                        remoteClient.sendLaunchSetting("ENABLE_CURSOR")
                                    } else {
                                        remoteClient.sendLaunchSetting("DISABLE_CURSOR")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // TV volume setting card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.card),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Smart TV Volume: ${tvVolume.toInt()}",
                                    color = AppTheme.colors.foreground,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = tvVolume,
                                    onValueChange = {
                                        tvVolume = it
                                        remoteClient.sendVolume(it.toInt())
                                    },
                                    valueRange = 0.0f..100.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AppTheme.colors.primary,
                                        activeTrackColor = AppTheme.colors.primary
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Picture Modes Row
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AppTheme.colors.card),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "TV Picture Mode",
                                    color = AppTheme.colors.foreground,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val modes = listOf("STANDARD", "VIVID", "CINEMA")
                                    modes.forEach { mode ->
                                        Button(
                                            onClick = {
                                                HapticManager.performTapHaptic(context)
                                                tvPictureMode = mode
                                                remoteClient.sendPictureMode(mode)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (tvPictureMode == mode) AppTheme.colors.primary else AppTheme.colors.card
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 4.dp)
                                                .border(1.dp, AppTheme.colors.border, RoundedCornerShape(20.dp))
                                        ) {
                                            Text(mode, fontSize = 11.sp, color = AppTheme.colors.foreground)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Launch system TV configurations
                        Button(
                            onClick = {
                                HapticManager.performTapHaptic(context)
                                remoteClient.sendLaunchSetting("TV_DASHBOARD")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Launch GTV Dashboard Overlay", color = AppTheme.colors.foreground)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    HapticManager.performTapHaptic(context)
                                    remoteClient.sendLaunchSetting("PICTURE")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.card),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 4.dp)
                                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(20.dp))
                            ) {
                                Text("TV Picture Settings", fontSize = 11.sp, color = AppTheme.colors.foreground)
                            }

                            Button(
                                onClick = {
                                    HapticManager.performTapHaptic(context)
                                    remoteClient.sendLaunchSetting("SOUND")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.card),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 4.dp)
                            ) {
                                Text("TV Sound Settings", fontSize = 11.sp, color = AppTheme.colors.foreground)
                            }
                        }
                    }
                }
            }
        }
    }



    // DIALOGS & POPUPS

    // 1. IP Manual Dialog Configuration
    if (showManualIpDialog) {
        AlertDialog(
            onDismissRequest = { showManualIpDialog = false },
            title = { Text("Configure Google TV IP", color = AppTheme.colors.foreground) },
            text = {
                Column {
                    Text(
                        text = "Enter the server's IP address displayed on your GTV Server Dashboard.",
                        color = AppTheme.colors.mutedForeground,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = manualIpText,
                        onValueChange = { manualIpText = it },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = AppTheme.colors.foreground,
                            unfocusedTextColor = AppTheme.colors.foreground,
                            focusedContainerColor = AppTheme.colors.background,
                            unfocusedContainerColor = AppTheme.colors.background
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        HapticManager.performTapHaptic(context)
                        if (manualIpText.isNotBlank()) {
                            val ip = manualIpText.trim()
                            ProfileManager.saveLastIp(context, ip)
                            remoteClient.connectToTv(ip)
                            showManualIpDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary)
                ) {
                    Text("Connect", color = AppTheme.colors.foreground)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        HapticManager.performTapHaptic(context)
                        showManualIpDialog = false
                    }
                ) {
                    Text("Cancel", color = AppTheme.colors.foreground)
                }
            },
            containerColor = AppTheme.colors.card
        )
    }

    // 2. Profile Picker Dialog
    if (showProfilePicker) {
        AlertDialog(
            onDismissRequest = { showProfilePicker = false },
            title = { Text("Select Physics Profile", color = AppTheme.colors.foreground) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (savedProfiles.isEmpty()) {
                        Text("No custom profiles saved yet.", color = AppTheme.colors.mutedForeground)
                    } else {
                        savedProfiles.forEach { name ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        HapticManager.performTapHaptic(context)
                                        val p = ProfileManager.loadProfile(context, name)
                                        if (p != null) {
                                            pointerSensitivity = p.sensitivity
                                            pointerAcceleration = p.acceleration
                                            pointerMomentum = p.friction
                                            trackpadSensitivity = p.trackpadSensitivity
                                            sensorEngine.sensitivityX = p.sensitivity
                                            sensorEngine.sensitivityY = p.sensitivity
                                            remoteClient.sendAcceleration(p.acceleration)
                                            remoteClient.sendFriction(p.friction)
                                            autoSave()
                                        }
                                        showProfilePicker = false
                                    }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, color = AppTheme.colors.foreground, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = {
                                        HapticManager.performTapHaptic(context)
                                        ProfileManager.deleteProfile(context, name)
                                        savedProfiles = ProfileManager.listProfiles(context)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Delete", fontSize = 10.sp, color = Color.White)
                                }
                            }
                            HorizontalDivider(color = AppTheme.colors.border)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        HapticManager.performTapHaptic(context)
                        showProfilePicker = false
                    }
                ) {
                    Text("Close", color = AppTheme.colors.foreground)
                }
            },
            containerColor = AppTheme.colors.card
        )
    }

    // 3. Save Custom Physics Profile Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Physics Profile", color = AppTheme.colors.foreground) },
            text = {
                Column {
                    Text("Enter a unique name for this physics configuration.", color = AppTheme.colors.mutedForeground)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it.take(20) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = AppTheme.colors.foreground,
                            unfocusedTextColor = AppTheme.colors.foreground,
                            focusedContainerColor = AppTheme.colors.background,
                            unfocusedContainerColor = AppTheme.colors.background
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        HapticManager.performTapHaptic(context)
                        if (profileName.isNotBlank()) {
                            val profile = PhysicsProfile(
                                profileName.trim(),
                                pointerSensitivity,
                                pointerAcceleration,
                                pointerMomentum,
                                trackpadSensitivity
                            )
                            ProfileManager.saveProfile(context, profile)
                            savedProfiles = ProfileManager.listProfiles(context)
                            profileName = ""
                            showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.primary)
                ) {
                    Text("Save", color = AppTheme.colors.foreground)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        HapticManager.performTapHaptic(context)
                        showSaveDialog = false
                    }
                ) {
                    Text("Cancel", color = AppTheme.colors.foreground)
                }
            },
            containerColor = AppTheme.colors.card
        )
    }
}

// Custom Modifier extension to draw grid lines on precision touchpad
fun Modifier.cyberGrid(): Modifier = drawBehind {
    val d = density
    val gridSpacing = d * 24.0f
    val strokeWidth = d * 1.0f
    val gridColor = Color(0x0AFFFFFF) // Super subtle grid color
    val w = size.width
    val h = size.height
    var x = 0.0f
    while (x < w) {
        drawLine(gridColor, Offset(x, 0.0f), Offset(x, h), strokeWidth)
        x += gridSpacing
    }
    var y = 0.0f
    while (y < h) {
        drawLine(gridColor, Offset(0.0f, y), Offset(w, y), strokeWidth)
        y += gridSpacing
    }
}
