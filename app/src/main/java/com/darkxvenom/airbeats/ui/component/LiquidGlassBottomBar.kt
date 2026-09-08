package com.darkxvenom.airbeats.ui.component

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.darkxvenom.airbeats.R
import com.darkxvenom.airbeats.ui.screens.Screens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.nio.IntBuffer

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LiquidGlassBottomNavigationBar(
    modifier: Modifier = Modifier,
    items: List<CurvedBottomNavigationItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    backdrop: PlatformBackdrop
) {
    if (items.isEmpty()) return
    val layer = rememberGraphicsLayer()
    val luminanceAnimation = remember { Animatable(0.3f) }

    val themeContrastColor by animateColorAsState(
        targetValue = Color.White,
        animationSpec = tween(500),
        label = "ContrastColor"
    )

    val itemBgColor by animateColorAsState(
        targetValue = Color.White.copy(alpha = 0.2f),
        animationSpec = tween(500),
        label = "ItemBgColor"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Centered Toolbar containing Home (0) and Explore (1)
        HorizontalFloatingToolbar(
            modifier = Modifier
                .drawBackdropCustomShape(
                    backdrop = backdrop,
                    layer = layer,
                    luminanceAnimation = luminanceAnimation.value,
                    shape = CircleShape
                )
                .wrapContentSize(),
            colors = androidx.compose.material3.FloatingToolbarDefaults.standardFloatingToolbarColors()
                .copy(toolbarContainerColor = Color.Transparent),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp),
            expanded = true
        ) {
            items.dropLast(1).forEachIndexed { index, item ->
                val isSelected = selectedIndex == index
                Button(
                    onClick = { onItemSelected(index) },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors().copy(
                        containerColor = if (isSelected) itemBgColor else Color.Transparent,
                        contentColor = if (isSelected) Color(0xFFF9A825) else themeContrastColor
                    ),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (isSelected) item.iconActive else item.iconInactive),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Separate FloatingActionButton containing the last item (e.g. Library) next to it
        val lastIndex = items.size - 1
        val lastItem = items.last()
        val isLastSelected = selectedIndex == lastIndex
        FloatingActionButton(
            modifier = Modifier
                .drawBackdropCustomShape(
                    backdrop = backdrop,
                    layer = layer,
                    luminanceAnimation = luminanceAnimation.value,
                    shape = CircleShape
                ),
            onClick = { onItemSelected(lastIndex) },
            shape = CircleShape,
            containerColor = Color.Transparent,
            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
        ) {
            Icon(
                painter = painterResource(id = if (isLastSelected) lastItem.iconActive else lastItem.iconInactive),
                contentDescription = null,
                tint = if (isLastSelected) Color(0xFFF9A825) else themeContrastColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
