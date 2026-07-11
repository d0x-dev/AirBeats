package com.darkxvenom.airbeats.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import com.darkxvenom.airbeats.viewmodels.BackupRestoreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import com.darkxvenom.airbeats.ui.component.NamePreferenceManager
import com.darkxvenom.airbeats.utils.GoogleAuthManager
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    navController: NavController,
    backupRestoreViewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val namePrefManager = remember { NamePreferenceManager(context) }
    
    val googleAuthLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val name = account.displayName ?: "Google User"
                
                coroutineScope.launch {
                    // Try to restore from Google Drive
                    val restored = backupRestoreViewModel.restoreFromDrive(context, account)
                    if (!restored) {
                        // If no backup exists, do an initial backup
                        namePrefManager.saveUserName(name)
                        backupRestoreViewModel.backupToDrive(context, account)
                    }
                    
                    namePrefManager.setUserNameSet(true)
                    
                    withContext(Dispatchers.Main) {
                        if (restored) {
                            // If restored, restart app to load DB
                            val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                            intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            context.startActivity(intent)
                        } else {
                            navController.navigate("home") {
                                popUpTo("onboarding") { inclusive = true }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val onGoogleSignInClick: () -> Unit = {
        val googleAuthManager = GoogleAuthManager(context)
        googleAuthLauncher.launch(googleAuthManager.getSignInClient().signInIntent)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "stringAnim")
    val stringWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "wave"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F2027),
                        Color(0xFF203A43),
                        Color(0xFF2C5364)
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val startY = height * 0.4f

            val path = Path()
            path.moveTo(width * 0.1f, startY)
            
            // Draw a wobbly string between two imaginary people
            val control1X = width * 0.3f
            val control1Y = startY + kotlin.math.sin(stringWave) * 100f
            val control2X = width * 0.7f
            val control2Y = startY + kotlin.math.sin(stringWave + 1f) * 100f
            val endX = width * 0.9f
            
            path.cubicTo(control1X, control1Y, control2X, control2Y, endX, startY)
            
            drawPath(
                path = path,
                color = Color.Cyan.copy(alpha = 0.8f),
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Draw simple stick figures holding the string
            drawCircle(Color.White, radius = 15f, center = Offset(width * 0.08f, startY - 30f))
            drawLine(Color.White, Offset(width * 0.08f, startY - 15f), Offset(width * 0.08f, startY + 40f), strokeWidth = 5f)
            drawLine(Color.White, Offset(width * 0.08f, startY), Offset(width * 0.1f, startY), strokeWidth = 5f)
            
            drawCircle(Color.White, radius = 15f, center = Offset(width * 0.92f, startY - 30f))
            drawLine(Color.White, Offset(width * 0.92f, startY - 15f), Offset(width * 0.92f, startY + 40f), strokeWidth = 5f)
            drawLine(Color.White, Offset(width * 0.92f, startY), Offset(width * 0.9f, startY), strokeWidth = 5f)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Welcome to AirBeats",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Sync your music across devices seamlessly",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            Button(
                onClick = onGoogleSignInClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.search), // Ideally a Google icon, fallback to search
                    contentDescription = "Google",
                    tint = Color.Black,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Continue with Google",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { navController.navigate("guest_profile_setup") },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Continue as Guest",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
