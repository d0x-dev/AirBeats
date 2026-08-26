package com.darkxvenom.airbeats.ui.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.constants.SpotifyCookieKey
import androidx.compose.material3.IconButton
import com.darkxvenom.airbeats.ui.component.PreferenceEntry
import com.darkxvenom.airbeats.ui.component.SettingsGeneralCategory
import com.darkxvenom.airbeats.utils.dataStore
import com.darkxvenom.airbeats.viewmodels.AccountViewModel
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpotifyAccountScreen(
    navController: NavController,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val hasSpotifyCookie by context.dataStore.data.map { it.contains(SpotifyCookieKey) }.collectAsState(initial = false)
    val user by viewModel.spotifyUser.collectAsState()
    val isLoading by viewModel.isSpotifyLoading.collectAsState()

    LaunchedEffect(hasSpotifyCookie) {
        if (hasSpotifyCookie) {
            viewModel.loadSpotifyData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spotify Account") },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading && user == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (user != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (user!!.images.isNotEmpty()) {
                            AsyncImage(
                                model = user!!.images.first().url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.person),
                                contentDescription = null,
                                modifier = Modifier.size(120.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = user!!.displayName ?: "Spotify User",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                SettingsGeneralCategory(
                    title = "Account Details",
                    items = listOf(
                        {
                            PreferenceEntry(
                                title = { Text("Email") },
                                description = user!!.email ?: "Hidden",
                                icon = { Icon(painterResource(R.drawable.email), contentDescription = null) }
                            )
                        },
                        {
                            PreferenceEntry(
                                title = { Text("Subscription") },
                                description = user!!.product?.uppercase() ?: "Unknown",
                                icon = { Icon(painterResource(R.drawable.star), contentDescription = null) }
                            )
                        },
                        {
                            PreferenceEntry(
                                title = { Text("Country") },
                                description = user!!.country ?: "Unknown",
                                icon = { Icon(painterResource(R.drawable.language), contentDescription = null) }
                            )
                        },
                        {
                            PreferenceEntry(
                                title = { Text("User ID") },
                                description = user!!.id,
                                icon = { Icon(painterResource(R.drawable.info), contentDescription = null) }
                            )
                        }
                    )
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Not logged in to Spotify.")
            }
        }
    }
}
