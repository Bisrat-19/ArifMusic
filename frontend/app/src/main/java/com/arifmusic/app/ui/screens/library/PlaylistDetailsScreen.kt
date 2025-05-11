package com.arifmusic.app.ui.screens.library

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.arifmusic.app.R
import com.arifmusic.app.data.local.PlaylistWithMusic
import com.arifmusic.app.data.model.Music
import com.arifmusic.app.ui.theme.Green
import com.arifmusic.app.ui.viewmodel.LibraryViewModel
import com.arifmusic.app.ui.viewmodel.MusicViewModel
import com.arifmusic.app.utils.SimpleMediaAccessHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailsScreen(
    playlistId: Long,
    libraryViewModel: LibraryViewModel,
    musicViewModel: MusicViewModel,
    onBackClick: () -> Unit,
    onMusicClick: (Music) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val playlistWithMusic = remember { mutableStateOf<PlaylistWithMusic?>(null) }
    val isLoading = remember { mutableStateOf(true) }
    val error = remember { mutableStateOf<String?>(null) }

    var selectedTab by remember { mutableStateOf(0) } // 0 for Playlists, 1 for Watchlist

    LaunchedEffect(playlistId) {
        isLoading.value = true
        try {
            libraryViewModel.getPlaylistWithMusic(playlistId).collect { result ->
                playlistWithMusic.value = result
                isLoading.value = false
            }
        } catch (e: Exception) {
            error.value = "Failed to load playlist: ${e.message}"
            isLoading.value = false
        }
    }


    LaunchedEffect(error.value) {
        error.value?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Your music in \"${playlistWithMusic.value?.playlist?.name ?: "Playlist"}\"",
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                ),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        containerColor = Color.Black,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            // Tab selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Playlists")
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Green,
                        selectedLabelColor = Color.White,
                        containerColor = Color.DarkGray.copy(alpha = 0.5f),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Watchlist")
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Green,
                        selectedLabelColor = Color.White,
                        containerColor = Color.DarkGray.copy(alpha = 0.5f),
                        labelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            if (isLoading.value) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Green)
                }
            } else {
                playlistWithMusic.value?.let { pwm ->
                    if (pwm.musicList.isEmpty()) {
                        EmptyPlaylistContent()
                    } else {
                        PlaylistMusicList(
                            musicList = pwm.musicList,
                            onMusicClick = onMusicClick,
                            onAddToWatchlistClick = { music ->
                                scope.launch {
                                    musicViewModel.toggleFavorite(music.id)
                                    snackbarHostState.showSnackbar(
                                        message = "Added to watchlist",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            },
                            onRemoveFromPlaylistClick = { music ->
                                scope.launch {
                                    libraryViewModel.removeMusicFromLibraryItem(playlistId, music.id, true)

                                    val result = libraryViewModel.getPlaylistWithMusic(playlistId).first()
                                    playlistWithMusic.value = result

                                    snackbarHostState.showSnackbar(
                                        message = "Removed from playlist",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                            ,
                            onShareMusicClick = { music ->
                                val shareText = "Check out '${music.title}' by ${music.artist} on ArifMusic! 🎶"
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share Song"))
                            }
                        )
                    }
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Playlist not found",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistMusicList(
    musicList: List<Music>,
    onMusicClick: (Music) -> Unit,
    onAddToWatchlistClick: (Music) -> Unit,
    onRemoveFromPlaylistClick: (Music) -> Unit,
    onShareMusicClick: (Music) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(musicList) { music ->
            PlaylistMusicItem(
                music = music,
                onMusicClick = onMusicClick,
                onAddToWatchlistClick = onAddToWatchlistClick,
                onRemoveFromPlaylistClick = onRemoveFromPlaylistClick,
                onShareMusicClick = onShareMusicClick
            )
        }
    }
}

@Composable
fun PlaylistMusicItem(
    music: Music,
    onMusicClick: (Music) -> Unit,
    onAddToWatchlistClick: (Music) -> Unit,
    onRemoveFromPlaylistClick: (Music) -> Unit,
    onShareMusicClick: (Music) -> Unit
) {
    var showDropdownMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMusicClick(music) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.DarkGray.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            if (music.artworkUri != null) {
                val imageUri = remember(music.artworkUri) {
                    SimpleMediaAccessHelper.getPlayableUri(context, music.artworkUri.toString())
                        ?: music.artworkUri
                }

                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .error(R.drawable.ic_music_note)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    onError = {
                        Log.e("PlaylistMusicItem", "Error loading image: ${music.artworkUri}")
                    }
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Song info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = music.title,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = music.artist,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Duration
        Text(
            text = formatDuration(music.duration),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Play button
        IconButton(
            onClick = { onMusicClick(music) },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Green),
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Box {
            IconButton(
                onClick = { showDropdownMenu = true }
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.White
                )
            }

            DropdownMenu(
                expanded = showDropdownMenu,
                onDismissRequest = { showDropdownMenu = false },
                modifier = Modifier.background(Color(0xFF333333))
            ) {
                DropdownMenuItem(
                    text = { Text("Add to Watchlist", color = Color.White) },
                    onClick = {
                        onAddToWatchlistClick(music)
                        showDropdownMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("Remove", color = Color.White) },
                    onClick = {
                        onRemoveFromPlaylistClick(music)
                        showDropdownMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("Share", color = Color.White) },
                    onClick = {
                        onShareMusicClick(music)
                        showDropdownMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun EmptyPlaylistContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This playlist is empty",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add songs to this playlist to start listening.",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 16.sp
            )
        }
    }
}

