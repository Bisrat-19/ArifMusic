package com.arifmusic.app.ui.components

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.arifmusic.app.ui.theme.Green
import com.arifmusic.app.ui.viewmodel.MusicViewModel
import com.arifmusic.app.utils.SimpleMediaAccessHelper
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun MiniPlayer(
    musicViewModel: MusicViewModel,
    onMiniPlayerClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMusic by musicViewModel.currentMusic.collectAsState()
    val isPlaying by musicViewModel.isPlaying.collectAsState()
    val currentPosition by musicViewModel.currentPosition.collectAsState()
    val duration by musicViewModel.duration.collectAsState()
    val context = LocalContext.current

    var offsetX by remember { mutableStateOf(0f) }
    var visible by remember { mutableStateOf(true) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(1000)
            musicViewModel.updateCurrentPosition()
        }
    }

    AnimatedVisibility(
        visible = currentMusic != null && visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it * 2 }, animationSpec = tween(durationMillis = 300))
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offsetX += delta
                    },
                    onDragStopped = { velocity ->
                        if (offsetX > context.resources.displayMetrics.widthPixels * 0.25f) {
                            visible = false
                            musicViewModel.stopMusic()

                        } else {
                            offsetX = 0f
                        }
                    }
                )
                .clickable { onMiniPlayerClick() },
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (duration > 0) {
                    LinearProgressIndicator(
                        progress = (currentPosition.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp),
                        color = Green,
                        trackColor = Color.DarkGray
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2C2C2E)),
                        contentAlignment = Alignment.Center
                    ) {
                        currentMusic?.artworkUri?.let { artworkUri ->
                            val imageUri = remember(artworkUri) {
                                SimpleMediaAccessHelper.getPlayableUri(context, artworkUri.toString())
                                    ?: artworkUri
                            }

                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(imageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                onError = {
                                    Log.e("MiniPlayer", "Error loading image: $artworkUri")
                                }
                            )
                        } ?: Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = currentMusic?.title ?: "Unknown",
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = currentMusic?.artist ?: "Unknown Artist",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                musicViewModel.pauseMusic()
                            } else {
                                musicViewModel.resumeMusic()
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Green)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
