package com.darkxvenom.airbeats.ui.component

import android.content.Context
import android.content.ContextWrapper
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.mediarouter.app.MediaRouteButton
import com.darkxvenom.airbeats.LocalPlayerConnection
import com.darkxvenom.airbeats.R
import com.google.android.gms.cast.framework.CastButtonFactory
import timber.log.Timber

/**
 * MediaRouteButton opens its chooser with a FragmentManager.  Compose can hand
 * AndroidView a themed ContextWrapper (especially inside a bottom sheet), which
 * is not itself a FragmentActivity and crashes when the chooser is tapped.
 */
fun Context.fragmentActivityOrNull(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.fragmentActivityOrNull()
    else -> null
}

fun createCastRouteButton(context: Context): MediaRouteButton? {
    val activity = context.fragmentActivityOrNull() ?: return null
    return MediaRouteButton(activity).apply {
        runCatching { CastButtonFactory.setUpMediaRouteButton(activity, this) }
            .onFailure { Timber.w(it, "CastButtonFactory setup failed") }
    }
}

@Composable
fun PlayerCastMenuRow(
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val playerConnection = LocalPlayerConnection.current
    val isCasting by (playerConnection?.service?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) })
    val castDeviceName by (playerConnection?.service?.castDeviceName?.collectAsState() ?: remember { mutableStateOf(null) })

    val buttonRef = remember { mutableStateOf<MediaRouteButton?>(null) }

    Surface(
        onClick = {
            runCatching {
                buttonRef.value?.performClick()
            }.onFailure { e ->
                Timber.w(e, "Failed to launch MediaRouteButton chooser")
            }
        },
        shape = RoundedCornerShape(14.dp),
        color = if (isCasting) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Hidden MediaRouteButton that handles Google Cast dialog and pairing
            AndroidView(
                factory = { ctx ->
                    createCastRouteButton(ctx)?.also { buttonRef.value = it }
                        ?: FrameLayout(ctx)
                },
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            ) {
                Icon(
                    painter = painterResource(if (isCasting) R.drawable.ic_cast_connected else R.drawable.ic_cast),
                    contentDescription = stringResource(R.string.ui_a11y_chromecast),
                    tint = if (isCasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(24.dp),
                )

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCasting) "Chromecast Connected" else "Google Cast / Chromecast",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCasting) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCasting) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (isCasting && !castDeviceName.isNullOrBlank()) {
                            "Streaming audio to $castDeviceName"
                        } else {
                            "Stream audio to Chromecast, Google Home, Nest, or Smart TV"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }

                if (isCasting) {
                    Surface(
                        onClick = {
                            playerConnection?.service?.disconnectCast()
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                    ) {
                        Text(
                            text = stringResource(R.string.ui_text_disconnect),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerCastIconButton(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
) {
    val playerConnection = LocalPlayerConnection.current
    val isCasting by (playerConnection?.service?.isCasting?.collectAsState() ?: remember { mutableStateOf(false) })
    val buttonRef = remember { mutableStateOf<MediaRouteButton?>(null) }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable {
                runCatching {
                    buttonRef.value?.performClick()
                }.onFailure { e ->
                    Timber.w(e, "Failed to launch MediaRouteButton chooser")
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                createCastRouteButton(ctx)?.also { buttonRef.value = it }
                    ?: FrameLayout(ctx)
            },
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
        )

        Icon(
            painter = painterResource(if (isCasting) R.drawable.ic_cast_connected else R.drawable.ic_cast),
            contentDescription = stringResource(R.string.ui_a11y_cast_audio),
            tint = if (isCasting) MaterialTheme.colorScheme.primary else tint,
            modifier = Modifier.size(22.dp),
        )
    }
}
