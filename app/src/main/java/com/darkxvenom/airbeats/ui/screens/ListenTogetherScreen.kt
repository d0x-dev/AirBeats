package com.darkxvenom.airbeats.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.darkxvenom.airbeats.LocalPlayerAwareWindowInsets
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.utils.ListenTogetherClient
import com.darkxvenom.airbeats.utils.ListenTogetherPlaybackState
import com.darkxvenom.airbeats.utils.ListenTogetherSession
import com.darkxvenom.airbeats.utils.ListenTogetherStore
import com.darkxvenom.airbeats.utils.ListenTogetherSync
import com.darkxvenom.airbeats.utils.joinByBullet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenTogetherScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val playerConnection = LocalPlayerConnection.current
    val mediaMetadata by playerConnection?.mediaMetadata?.collectAsState(initial = null)
        ?: remember { mutableStateOf(null) }
    val isPlaying by playerConnection?.isPlaying?.collectAsState(initial = false)
        ?: remember { mutableStateOf(false) }
    val currentPosition by playerConnection?.currentPosition?.collectAsState(initial = 0L)
        ?: remember { mutableLongStateOf(0L) }
    val scope = rememberCoroutineScope()

    var displayName by remember { mutableStateOf(ListenTogetherStore.defaultName()) }
    var joinCode by remember { mutableStateOf("") }
    var session by remember { mutableStateOf<ListenTogetherSession?>(null) }
    var isHost by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val syncedSession by ListenTogetherSync.session.collectAsState()
    val syncedIsHost by ListenTogetherSync.isHost.collectAsState()
    val latestMediaMetadata by rememberUpdatedState(mediaMetadata)
    val latestIsPlaying by rememberUpdatedState(isPlaying)
    val latestCurrentPosition by rememberUpdatedState(currentPosition)
    val sessionCreatedMessage = stringResource(R.string.session_created)
    val joinedSessionMessage = stringResource(R.string.joined_session)
    val leftSessionMessage = stringResource(R.string.left_session)
    val listenTogetherCodeLabel = stringResource(R.string.listen_together_code)

    LaunchedEffect(syncedSession, syncedIsHost) {
        syncedSession?.let {
            session = it
            isHost = syncedIsHost
        }
    }

    LaunchedEffect(Unit) {
        val savedSession = ListenTogetherStore.load(context) ?: return@LaunchedEffect
        displayName = savedSession.displayName
        isHost = savedSession.isHost
        runCatching {
            ListenTogetherClient
                .getSession(savedSession.code)
                .copy(participantId = savedSession.participantId)
        }.onSuccess {
            session = it
        }.onFailure {
            ListenTogetherStore.clear(context)
            message = it.message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.listen_together)) },
                navigationIcon = {
                    IconButton(onClick = navController::navigateUp) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(
                            WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom
                        )
                    )
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.listen_together_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            CurrentSessionCard(
                session = session,
                isHost = isHost,
                songTitle = mediaMetadata?.title ?: session?.state?.title,
                subtitle = mediaMetadata?.artists?.joinToString { it.name }
                    ?: session?.state?.artists?.joinToString(),
            )

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.display_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    enabled = !isLoading && mediaMetadata != null,
                    onClick = {
                        val metadata = mediaMetadata ?: return@Button
                        isLoading = true
                        scope.launch {
                            try {
                                session =
                                    ListenTogetherClient.createSession(
                                        displayName = displayName,
                                        state =
                                            ListenTogetherPlaybackState(
                                                songId = metadata.id,
                                                title = metadata.title,
                                                artists = metadata.artists.map { it.name },
                                                thumbnailUrl = metadata.thumbnailUrl,
                                                positionMs = currentPosition,
                                                isPlaying = isPlaying,
                                            )
                                )
                                isHost = true
                                session?.let {
                                    ListenTogetherSync.adoptSession(context, it, displayName, true)
                                }
                                message = sessionCreatedMessage
                            } catch (e: Exception) {
                                message = e.message
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.create_session))
                }

                OutlinedButton(
                    enabled = session?.joinUrl?.isNotBlank() == true,
                    onClick = {
                        val shareText =
                            "${session?.joinUrl}\n$listenTogetherCodeLabel: ${session?.code}"
                        context.startActivity(
                            Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                },
                                null
                            )
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.share))
                }
            }

            OutlinedTextField(
                value = joinCode,
                onValueChange = { joinCode = it.uppercase() },
                label = { Text(stringResource(R.string.session_code)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                enabled = !isLoading && joinCode.isNotBlank(),
                onClick = {
                    isLoading = true
                    scope.launch {
                        try {
                            session = ListenTogetherClient.joinSession(joinCode, displayName)
                            isHost = false
                            session?.let {
                                ListenTogetherSync.adoptSession(context, it, displayName, false)
                            }
                            message = joinedSessionMessage
                        } catch (e: Exception) {
                            message = e.message
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.join_session))
            }

            session?.let { activeSession ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(activeSession.code))
                            message = context.getString(R.string.session_code_copied)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.copy_session_code))
                    }
                    OutlinedButton(
                        onClick = {
                            ListenTogetherSync.leaveSession()
                            session = null
                            isHost = false
                            message = leftSessionMessage
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.leave_session))
                    }
                }

                ParticipantsSection(activeSession)
            }

            message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun ParticipantsSection(session: ListenTogetherSession) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.session_users),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        session.participantList.forEach { participant ->
            Text(
                text = if (participant.isHost) {
                    stringResource(R.string.created_by_name, participant.name)
                } else {
                    participant.name
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
private fun CurrentSessionCard(
    session: ListenTogetherSession?,
    isHost: Boolean,
    songTitle: String?,
    subtitle: String?,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = if (session == null) {
                    stringResource(R.string.no_active_session)
                } else if (isHost) {
                    stringResource(R.string.hosting_session, session.code)
                } else {
                    stringResource(R.string.joined_session_with_code, session.code)
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = songTitle ?: stringResource(R.string.play_song_first),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = joinByBullet(it, session?.participants?.let { count -> "$count listeners" }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            session?.joinUrl?.takeIf { it.isNotBlank() }?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
