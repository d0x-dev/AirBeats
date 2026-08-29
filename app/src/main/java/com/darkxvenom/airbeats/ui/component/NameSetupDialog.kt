package com.darkxvenom.airbeats.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.darkxvenom.airbeats.R

@Composable
fun NameSetupDialog(
    onNameConfirmed: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val trimmedName = nameInput.text.trim()
    val isNameValid = trimmedName.isNotEmpty() && trimmedName.length <= 16

    val isDark = isSystemInDarkTheme()
    val dialogBgColor = if (isDark) Color(0xFF181820) else Color(0xFFFFFFFF)
    val textColor = if (isDark) Color(0xFFEEEEF2) else Color(0xFF1A1A24)
    val subTextColor = if (isDark) Color(0xFFA0A0B2) else Color(0xFF6B6B80)
    val inputBgColor = if (isDark) Color(0xFF242430) else Color(0xFFF2F2F8)
    val primaryColor = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        try {
            focusRequester.requestFocus()
        } catch (_: Exception) {}
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .imePadding(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp)),
                colors = CardDefaults.cardColors(containerColor = dialogBgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Logo Icon Badge
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        primaryColor.copy(alpha = 0.25f),
                                        primaryColor.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(1.5.dp, primaryColor.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.airbeats_monochrome),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Title
                    Text(
                        text = "Welcome to AirBeats!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Subtitle
                    Text(
                        text = "What should we call you? Enter your name or nickname to personalize your experience.",
                        fontSize = 14.sp,
                        color = subTextColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // High Contrast Text Field
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = {
                            if (it.text.length <= 16) {
                                nameInput = it
                            }
                        },
                        label = {
                            Text(
                                text = "Your Name",
                                color = if (nameInput.text.isNotEmpty()) primaryColor else subTextColor,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        placeholder = {
                            Text(
                                text = "e.g. Alex, Venom...",
                                color = subTextColor.copy(alpha = 0.7f)
                            )
                        },
                        singleLine = true,
                        textStyle = TextStyle(
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = inputBgColor,
                            unfocusedContainerColor = inputBgColor,
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = primaryColor.copy(alpha = 0.4f),
                            cursorColor = primaryColor
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (isNameValid) {
                                    keyboardController?.hide()
                                    onNameConfirmed(trimmedName)
                                }
                            }
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Character counter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${nameInput.text.length} / 16",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Get Started Button
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            onNameConfirmed(trimmedName)
                        },
                        enabled = isNameValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            contentColor = Color.White,
                            disabledContainerColor = primaryColor.copy(alpha = 0.35f),
                            disabledContentColor = Color.White.copy(alpha = 0.6f)
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 1.dp
                        )
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
