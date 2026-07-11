package com.darkxvenom.airbeats.ui.screens.onboarding

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import com.darkxvenom.airbeats.utils.DriveResult
import com.darkxvenom.airbeats.utils.GoogleAuthManager
import com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun OnboardingScreen(
    navController: NavController,
    backupRestoreViewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val namePrefManager = remember { NamePreferenceManager(context) }
    
    // Store user data in case we need to retry after getting permissions
    var currentUserEmail by remember { mutableStateOf<String?>(null) }
    var currentUserName by remember { mutableStateOf<String?>(null) }

    val drivePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val email = currentUserEmail
            val name = currentUserName
            if (email != null && name != null) {
                // Retry backup/restore process
                coroutineScope.launch {
                    val restoredResult = backupRestoreViewModel.restoreFromDrive(context, email)
                    if (restoredResult is DriveResult.Success && restoredResult.data) {
                        withContext(Dispatchers.Main) {
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            context.startActivity(intent)
                        }
                    } else if (restoredResult is DriveResult.Error || (restoredResult is DriveResult.Success && !restoredResult.data)) {
                        namePrefManager.saveUserName(name)
                        backupRestoreViewModel.backupToDrive(context, email)
                        withContext(Dispatchers.Main) {
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    }
                }
            }
        } else {
            Toast.makeText(context, "Google Drive permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    val fallbackGoogleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val email = account.email ?: return@rememberLauncherForActivityResult
                val name = account.displayName ?: "Google User"
                
                currentUserEmail = email
                currentUserName = name

                coroutineScope.launch {
                    val restoredResult = backupRestoreViewModel.restoreFromDrive(context, email)
                    
                    if (restoredResult is DriveResult.NeedsPermission) {
                        drivePermissionLauncher.launch(restoredResult.intent)
                    } else if (restoredResult is DriveResult.Success && restoredResult.data) {
                        withContext(Dispatchers.Main) {
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            context.startActivity(intent)
                        }
                    } else {
                        namePrefManager.saveUserName(name)
                        backupRestoreViewModel.backupToDrive(context, email)
                        withContext(Dispatchers.Main) {
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val onGoogleSignInClick: () -> Unit = {
        coroutineScope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(GoogleAuthManager.WEB_CLIENT_ID)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                
                if (credential is androidx.credentials.CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val email = googleIdTokenCredential.id
                    val name = googleIdTokenCredential.displayName ?: "Google User"
                    
                    currentUserEmail = email
                    currentUserName = name

                    val restoredResult = backupRestoreViewModel.restoreFromDrive(context, email)
                    
                    if (restoredResult is DriveResult.NeedsPermission) {
                        drivePermissionLauncher.launch(restoredResult.intent)
                    } else if (restoredResult is DriveResult.Success && restoredResult.data) {
                        withContext(Dispatchers.Main) {
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            context.startActivity(intent)
                        }
                    } else {
                        // Error or false (not restored) -> Do initial backup
                        namePrefManager.saveUserName(name)
                        backupRestoreViewModel.backupToDrive(context, email)
                        withContext(Dispatchers.Main) {
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    }
                }
            } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                // Fallback to legacy GoogleSignInClient if no credentials available
                val googleAuthManager = GoogleAuthManager(context)
                fallbackGoogleAuthLauncher.launch(googleAuthManager.getSignInClient().signInIntent)
            } catch (e: Exception) {
                if (e.message?.contains("NoCredential") == true) {
                    val googleAuthManager = GoogleAuthManager(context)
                    fallbackGoogleAuthLauncher.launch(googleAuthManager.getSignInClient().signInIntent)
                } else {
                    e.printStackTrace()
                    Toast.makeText(context, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
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
                .androidx.compose.ui.graphics.graphicsLayer { alpha = 0.99f }
                .androidx.compose.ui.draw.drawWithContent {
                    val colors = listOf(Color.Transparent, Color.Black, Color.Black)
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = colors,
                            startY = size.height * 0.4f,
                            endY = size.height * 0.8f
                        ),
                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA259FF)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
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

            Spacer(modifier = Modifier.height(16.dp))

            // Secondary Dark Button (Continue as Guest)
            Button(
                onClick = { navController.navigate("guest_profile_setup") },
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
