package com.darkxvenom.airbeats.ui.screens.onboarding

import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.darkxvenom.airbeats.R
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class SyncState {
    IDLE,
    CHECKING,
    RESTORING,
    RESTORED,
    NEW_USER
}

@Composable
fun OnboardingScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val namePrefManager = remember { NamePreferenceManager(context) }
    val backupViewModel: com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel = androidx.hilt.navigation.compose.hiltViewModel()
    
    var syncState by remember { mutableStateOf(SyncState.IDLE) }
    var currentUserName by remember { mutableStateOf("") }
    var currentUserEmail by remember { mutableStateOf("") }
    var featureStep by remember { mutableStateOf(0) }

    fun continueToHome() {
        coroutineScope.launch {
            if (currentUserName.isNotBlank()) namePrefManager.saveUserName(currentUserName)
            if (currentUserEmail.isNotBlank()) namePrefManager.saveAccountEmail(currentUserEmail)
            syncState = SyncState.IDLE
            navController.navigate("home") {
                popUpTo("onboarding") {
                    inclusive = true
                }
            }
        }
    }

    fun showSignInError(message: String) {
        syncState = SyncState.IDLE
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun requestGoogleSignIn(filterByAuthorizedAccounts: Boolean) {
        coroutineScope.launch {
            try {
                val credentialOption: androidx.credentials.CredentialOption = if (filterByAuthorizedAccounts) {
                    GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(true)
                        .setServerClientId(GoogleAuthManager.WEB_CLIENT_ID)
                        .setAutoSelectEnabled(false)
                        .build()
                } else {
                    com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption.Builder(GoogleAuthManager.WEB_CLIENT_ID)
                        .build()
                }

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(credentialOption)
                    .build()

                val credential = credentialManager.getCredential(context, request).credential
                
                // Only change state after user has actually selected an account
                syncState = SyncState.CHECKING
                
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val name = googleCredential.displayName
                    ?: googleCredential.givenName
                    ?: googleCredential.id.substringBefore("@")
                val email = googleCredential.id
                
                currentUserName = name
                currentUserEmail = email

                val backupClient = com.darkxvenom.airbeats.utils.CloudBackupClient()
                val backupExists = backupClient.checkBackupExists(email)
                
                if (backupExists) {
                    syncState = SyncState.RESTORING
                    val result = backupViewModel.restoreFromDrive(context, email)
                    if (result is com.darkxvenom.airbeats.utils.DriveResult.Success) {
                        syncState = SyncState.RESTORED
                        delay(2500) // Show restored message for a bit
                        
                        // Save name and email only when we are done
                        namePrefManager.saveUserName(name)
                        namePrefManager.saveAccountEmail(email)
                        
                        // Restart app to apply changes
                        context.stopService(android.content.Intent(context, com.darkxvenom.airbeats.playback.MusicService::class.java))
                        context.startActivity(android.content.Intent(context, com.darkxvenom.airbeats.MainActivity::class.java).apply {
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        })
                        Runtime.getRuntime().exit(0)
                        return@launch
                    } else {
                        Toast.makeText(context, "Failed to restore backup. Continuing...", Toast.LENGTH_SHORT).show()
                        syncState = SyncState.NEW_USER
                    }
                } else {
                    syncState = SyncState.NEW_USER
                    // Upload initial backup in background (name/email will be saved when user clicks Get Started)
                    backupViewModel.backupToDrive(context, email, name)
                }
            } catch (e: NoCredentialException) {
                if (filterByAuthorizedAccounts) {
                    requestGoogleSignIn(filterByAuthorizedAccounts = false)
                } else {
                    showSignInError("No Google account found on this device.")
                }
            } catch (e: GoogleIdTokenParsingException) {
                e.printStackTrace()
                showSignInError("Google sign in failed. Please try again.")
            } catch (e: GetCredentialException) {
                e.printStackTrace()
                showSignInError(e.message ?: "Google sign in was cancelled.")
            } catch (e: Exception) {
                e.printStackTrace()
                showSignInError("Sign in failed: ${e.message}")
            }
        }
    }

    val onGoogleSignInClick: () -> Unit = {
        requestGoogleSignIn(filterByAuthorizedAccounts = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.hero_bg),
            contentDescription = "Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Blurred Background Masked
        Image(
            painter = painterResource(id = R.drawable.hero_bg),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 24.dp)
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

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0x800D0D1A), Color(0xFF0D0D1A), Color(0xFF0D0D1A)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        // Main Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Crossfade(targetState = syncState, label = "OnboardingState") { state ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    when (state) {
                        SyncState.IDLE -> {
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
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 48.dp)
                            )
                            Button(
                                onClick = onGoogleSignInClick,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA259FF)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Icon(painter = painterResource(id = R.drawable.google), contentDescription = "Google", tint = Color.Unspecified, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Continue With Google", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { navController.navigate("guest_profile_setup") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF232336)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text("Continue as Guest", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(48.dp))
                            Text("By signing up or logging in, I accept the AirBeats\nTerms of Service and Privacy Policy", color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }

                        SyncState.CHECKING -> {
                            CircularProgressIndicator(color = Color(0xFFA259FF), modifier = Modifier.padding(bottom = 24.dp))
                            Text(
                                text = "Looking for any backup in cloud...",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text("Hold on a moment.", color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 120.dp))
                        }

                        SyncState.RESTORING -> {
                            CircularProgressIndicator(color = Color(0xFFA259FF), modifier = Modifier.padding(bottom = 24.dp))
                            Text(
                                text = "Restoring your backup...",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                            Text("Hold on a moment.", color = Color.LightGray, fontSize = 14.sp, modifier = Modifier.padding(top = 8.dp, bottom = 120.dp))
                        }

                        SyncState.RESTORED -> {
                            Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Success", tint = Color(0xFFA259FF), modifier = Modifier.size(64.dp).padding(bottom = 16.dp))
                            Text(
                                text = "Hi $currentUserName,",
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = "Welcome again back to Airbeats",
                                color = Color.LightGray,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 120.dp)
                            )
                        }

                        SyncState.NEW_USER -> {
                            val featureTitles = listOf("Discover Music", "Create Playlists", "Offline Mode")
                            val featureDesc = listOf("Find millions of songs matching your mood.", "Curate your perfect listening experience.", "Download your favorites and listen anywhere.")
                            
                            Text(
                                text = if (featureStep == 0) "Hi $currentUserName, welcome!" else featureTitles[featureStep - 1],
                                color = Color.White,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Text(
                                text = if (featureStep == 0) "Looking like you are new to Airbeats" else featureDesc[featureStep - 1],
                                color = Color.LightGray,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 48.dp)
                            )
                            Button(
                                onClick = {
                                    if (featureStep < featureTitles.size) featureStep++ else continueToHome()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA259FF)),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth().height(56.dp)
                            ) {
                                Text(if (featureStep == featureTitles.size) "Get Started" else "Next", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }
                }
            }
        }
    }
}
