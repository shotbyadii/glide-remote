package com.example.tvserver.ui.keyboard

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tvserver.theme.AppTheme
import com.example.tvserver.ui.utils.appleShadow

@Composable
fun KeyboardTestScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var typedText by remember { mutableStateOf("") }
    var popupText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MOUSE & KEYBOARD TEST LAB",
                    style = AppTheme.typography.title.copy(
                        color = AppTheme.colors.foreground,
                        fontSize = 24.sp
                    )
                )
                Text(
                    text = "Test coordinate precision, on-screen keyboard hover/clicks, and Android System IME popups.",
                    style = AppTheme.typography.body.copy(
                        color = AppTheme.colors.mutedForeground,
                        fontSize = 11.sp
                    )
                )
            }

            val backInteractionSource = remember { MutableInteractionSource() }
            val isBackFocused by backInteractionSource.collectIsFocusedAsState()
            val isBackHovered by backInteractionSource.collectIsHoveredAsState()
            val isBackActive = isBackFocused || isBackHovered

            val backBg by animateColorAsState(
                if (isBackActive) AppTheme.colors.primary else AppTheme.colors.card,
                label = "backBg"
            )
            val backTextColor = if (isBackActive) AppTheme.colors.primaryForeground else AppTheme.colors.foreground

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(backBg)
                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = backInteractionSource,
                        indication = null,
                        onClick = onBack
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "◀ DASHBOARD",
                    style = AppTheme.typography.caption.copy(
                        color = backTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Left Column: System Keyboard Test
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .appleShadow(elevation = 8.dp, alpha = 0.15f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.card)
                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "SYSTEM IME KEYBOARD TEST",
                    style = AppTheme.typography.caption.copy(
                        color = AppTheme.colors.foreground,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "Click the field below to request focus and invoke Android's native soft keyboard dialog.",
                    style = AppTheme.typography.body.copy(
                        color = AppTheme.colors.mutedForeground,
                        fontSize = 10.sp
                    )
                )
                OutlinedTextField(
                    value = popupText,
                    onValueChange = { popupText = it },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AppTheme.colors.foreground,
                        unfocusedTextColor = AppTheme.colors.foreground,
                        focusedContainerColor = AppTheme.colors.background,
                        unfocusedContainerColor = AppTheme.colors.background,
                        focusedBorderColor = AppTheme.colors.primary,
                        unfocusedBorderColor = AppTheme.colors.border
                    ),
                    placeholder = { Text("Click to type...") }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Input echo: " + if (popupText.isEmpty()) "[Empty]" else popupText,
                    style = AppTheme.typography.body.copy(
                        color = AppTheme.colors.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }

            // Right Column: On-Screen Virtual Keyboard Test
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .appleShadow(elevation = 8.dp, alpha = 0.15f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppTheme.colors.card)
                    .border(1.dp, AppTheme.colors.border, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "VIRTUAL UI ECHO",
                    style = AppTheme.typography.caption.copy(
                        color = AppTheme.colors.foreground,
                        fontWeight = FontWeight.Bold
                    )
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppTheme.colors.background)
                        .border(1.dp, AppTheme.colors.border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = if (typedText.isEmpty()) "[Virtual Input Echo]" else typedText,
                        style = AppTheme.typography.body.copy(
                            color = if (typedText.isEmpty()) AppTheme.colors.mutedForeground else AppTheme.colors.foreground,
                            fontSize = 14.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val row1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
                val row2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
                val row3 = listOf("Z", "X", "C", "V", "B", "N", "M")

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row1.forEach { char ->
                            KeyButton(
                                char = char,
                                onClick = { typedText += char },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row2.forEach { char ->
                            KeyButton(
                                char = char,
                                onClick = { typedText += char },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row3.forEach { char ->
                            KeyButton(
                                char = char,
                                onClick = { typedText += char },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ControlKeyButton(
                            text = "SPACE",
                            onClick = { typedText += " " },
                            modifier = Modifier.weight(2f)
                        )
                        ControlKeyButton(
                            text = "⌫",
                            onClick = {
                                if (typedText.isNotEmpty()) {
                                    typedText = typedText.dropLast(1)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ControlKeyButton(
                            text = "CLEAR",
                            onClick = { typedText = "" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KeyButton(
    char: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isActive = isFocused || isHovered

    val scale by animateFloatAsState(if (isActive) 1.15f else 1.0f, label = "keyScale")
    val bg by animateColorAsState(
        if (isActive) AppTheme.colors.primary else AppTheme.colors.background,
        label = "keyBg"
    )
    val borderCol by animateColorAsState(
        if (isActive) AppTheme.colors.primary else AppTheme.colors.border,
        label = "keyBorder"
    )
    val textColor = if (isActive) AppTheme.colors.primaryForeground else AppTheme.colors.foreground

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text = char,
            style = AppTheme.typography.body.copy(
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun ControlKeyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isActive = isFocused || isHovered

    val scale by animateFloatAsState(if (isActive) 1.08f else 1.0f, label = "ctrlScale")
    val bg by animateColorAsState(
        if (isActive) AppTheme.colors.primary else AppTheme.colors.background,
        label = "ctrlBg"
    )
    val borderCol by animateColorAsState(
        if (isActive) AppTheme.colors.primary else AppTheme.colors.border,
        label = "ctrlBorder"
    )
    val textColor = if (isActive) AppTheme.colors.primaryForeground else AppTheme.colors.foreground

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .height(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderCol, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text = text,
            style = AppTheme.typography.caption.copy(
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
