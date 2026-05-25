package com.example.mobileclient.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mobileclient.data.HapticManager
import com.example.mobileclient.data.ProfileManager
import com.example.mobileclient.ui.utils.hapticClickable
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var tapEnabled by remember { mutableStateOf(ProfileManager.isHapticTapEnabled(context)) }
    var hoverEnabled by remember { mutableStateOf(ProfileManager.isHapticHoverEnabled(context)) }
    var tapIntensity by remember { mutableFloatStateOf(ProfileManager.getHapticTapIntensity(context)) }
    var hoverIntensity by remember { mutableFloatStateOf(ProfileManager.getHapticHoverIntensity(context)) }

    // Save configuration updates
    fun updateSettings() {
        ProfileManager.saveHapticSettings(context, tapEnabled, hoverEnabled, tapIntensity, hoverIntensity)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tactile Preferences",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121214)
                )
            )
        },
        containerColor = Color(0xFF121214),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Tap Haptics Section Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2C2C32), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Button Tap Haptics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Sharp tactile click when pressing buttons",
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                        Switch(
                            checked = tapEnabled,
                            onCheckedChange = {
                                tapEnabled = it
                                updateSettings()
                                if (it) HapticManager.performTapHaptic(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.4f),
                                uncheckedThumbColor = Color(0xFF767680),
                                uncheckedTrackColor = Color(0xFF2C2C32)
                            )
                        )
                    }

                    if (tapEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tap Intensity",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${(tapIntensity * 100).roundToInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Slider(
                                value = tapIntensity,
                                onValueChange = {
                                    tapIntensity = it
                                    updateSettings()
                                },
                                onValueChangeFinished = {
                                    HapticManager.performTapHaptic(context)
                                },
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF10B981),
                                    activeTrackColor = Color(0xFF10B981),
                                    inactiveTrackColor = Color(0xFF2C2C32)
                                )
                            )
                        }

                        // Test button
                        Button(
                            onClick = { HapticManager.performTapHaptic(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Text("Test Click Feedback", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Hover Haptics Section Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF2C2C32), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hover & Scroll Haptics",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Low-intensity ticks when sliding over components",
                                fontSize = 12.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                        Switch(
                            checked = hoverEnabled,
                            onCheckedChange = {
                                hoverEnabled = it
                                updateSettings()
                                if (it) HapticManager.performHoverHaptic(context)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF3B82F6),
                                checkedTrackColor = Color(0xFF3B82F6).copy(alpha = 0.4f),
                                uncheckedThumbColor = Color(0xFF767680),
                                uncheckedTrackColor = Color(0xFF2C2C32)
                            )
                        )
                    }

                    if (hoverEnabled) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Hover Intensity",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "${(hoverIntensity * 100).roundToInt()}%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3B82F6)
                                )
                            }
                            Slider(
                                value = hoverIntensity,
                                onValueChange = {
                                    hoverIntensity = it
                                    updateSettings()
                                },
                                onValueChangeFinished = {
                                    HapticManager.performHoverHaptic(context)
                                },
                                valueRange = 0.1f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF3B82F6),
                                    activeTrackColor = Color(0xFF3B82F6),
                                    inactiveTrackColor = Color(0xFF2C2C32)
                                )
                            )
                        }

                        // Sliding playground
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Sliding Haptic Playground",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            // A row of hover ticks where dragging across them fires haptics
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF121214))
                                    .border(1.dp, Color(0xFF2C2C32), RoundedCornerShape(8.dp)),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (i in 1..6) {
                                    Box(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(36.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1A1A1E))
                                            .border(1.dp, Color(0xFF2C2C32), RoundedCornerShape(4.dp))
                                            .hapticClickable {
                                                // click is tap haptic, but moving onto it can be tested
                                                HapticManager.performHoverHaptic(context)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "T$i",
                                            color = Color(0xFF8E8E93),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Tap inside the cells above to feel low-intensity hover ticks.",
                                fontSize = 10.sp,
                                color = Color(0xFF8E8E93)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
