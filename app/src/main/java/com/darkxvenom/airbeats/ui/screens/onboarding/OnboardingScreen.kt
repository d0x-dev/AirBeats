package com.darkxvenom.airbeats.ui.screens.onboarding

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.darkxvenom.airbeats.R
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.NavController
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import com.darkxvenom.airbeats.utils.GoogleAuthManager
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val namePrefManager = remember { NamePreferenceManager(context) }
    val backupViewModel: com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    var isLoading by remember { mutableStateOf(false) }

    fun continueToHome(name: String) {
        coroutineScope.launch {
            namePrefManager.saveUserName(name)
            isLoading = false
            navController.navigate("home") {
                popUpTo("onboarding") {
                    inclusive = true
                }
            }
        }
    }

    fun showSignInError(message: String) {
        isLoading = false
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun requestGoogleSignIn(filterByAuthorizedAccounts: Boolean) {
        isLoading = true
        coroutineScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(filterByAuthorizedAccounts)
                    .setServerClientId(GoogleAuthManager.WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credential = credentialManager.getCredential(context, request).credential
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val name = googleCredential.displayName
                    ?: googleCredential.givenName
                    ?: googleCredential.id.substringBefore("@")
                val email = googleCredential.id

                val backupClient = com.darkxvenom.airbeats.utils.CloudBackupClient()
                val backupExists = backupClient.checkBackupExists(email)
                
                if (backupExists) {
                    Toast.makeText(context, "Restoring cloud backup...", Toast.LENGTH_SHORT).show()
                    val result = backupViewModel.restoreFromDrive(context, email)
                    if (result is com.darkxvenom.airbeats.utils.DriveResult.Success) {
                        namePrefManager.saveUserName(name)
                        namePrefManager.saveAccountEmail(email)
                        Toast.makeText(context, "Restore complete!", Toast.LENGTH_SHORT).show()
                        // Restart app to apply changes
                        context.stopService(android.content.Intent(context, com.darkxvenom.airbeats.playback.MusicService::class.java))
                        context.startActivity(android.content.Intent(context, com.darkxvenom.airbeats.MainActivity::class.java).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        })
                        Runtime.getRuntime().exit(0)
                        return@launch
                    }
                } else {
                    Toast.makeText(context, "Creating cloud folder...", Toast.LENGTH_SHORT).show()
                    namePrefManager.saveUserName(name)
                    namePrefManager.saveAccountEmail(email)
                    backupViewModel.backupToDrive(context, email)
                }

                continueToHome(name)
            } catch (e: NoCredentialException) {
                if (filterByAuthorizedAccounts) {
                    requestGoogleSignIn(filterByAuthorizedAccounts = false)
                } else {
                    showSignInError("No Google account found on this device.")
                }
            } catch (e: GoogleIdTokenParsingException) {
                isLoading = false
                e.printStackTrace()
                showSignInError("Google sign in failed. Please try again.")
            } catch (e: GetCredentialException) {
                isLoading = false
                e.printStackTrace()
                showSignInError(e.message ?: "Google sign in was cancelled.")
            } catch (e: Exception) {
                isLoading = false
                e.printStackTrace()
                showSignInError("Sign in failed: ${e.message}")
            }
        }
    }

    val onGoogleSignInClick: () -> Unit = {
        isLoading = true
        requestGoogleSignIn(filterByAuthorizedAccounts = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Unblurred Background Image
        Image(
            painter = painterResource(id = R.drawable.hero_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Blurred Background Image (Masked to show only at the bottom)
        Image(
            painter = painterResource(id = R.drawable.hero_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 24.dp)
                .graphicsLayer { alpha = 0.99f }
                .drawWithContent {
                    val colors = listOf(Color.Transparent, Color.Black, Color.Black)
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = colors,
                            startY = this.size.height * 0.4f,
                            endY = this.size.height * 0.8f
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        )

        // Gradient overlay fading to dark purple/black at bottom
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x800D0D1A),
                            Color(0xFF0D0D1A),
                            Color(0xFF0D0D1A)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Let get started",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Sign up or log in to see what's happening\nnear you",
                color = Color.LightGray,
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Primary Purple Button (Continue with Google)
            Button(
                onClick = onGoogleSignInClick,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA259FF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.google),
                        contentDescription = "Google",
                        tint = Color.Unspecified, // Keep original colors
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Continue With Google",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Dark Button (Continue as Guest)
            Button(
                onClick = { navController.navigate("guest_profile_setup") },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232336)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Continue as Guest",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "By signing up or logging in, I accept the AirBeats\nTerms of Service and Privacy Policy",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
