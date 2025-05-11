package com.arifmusic.app.ui.screens.library

import android.content.Intent
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.net.toUri
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.arifmusic.app.R
import com.arifmusic.app.data.local.PlaylistWithMusic
import com.arifmusic.app.data.model.Music
import com.arifmusic.app.data.model.Playlist
import com.arifmusic.app.ui.theme.Green
import com.arifmusic.app.ui.viewmodel.LibraryViewModel
import com.arifmusic.app.ui.viewmodel.MusicViewModel
import com.arifmusic.app.utils.SimpleMediaAccessHelper
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.TimeUnit

enum class LibraryTab {
    PLAYLISTS, WATCHLIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    musicViewModel: MusicViewModel,
    userId: String,
    onPlaylistClick: (Long) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onMusicClick: (Music) -> Unit,
    onAddToPlaylistClick: (music: Music, playlistId: Long) -> Unit,
    onRemoveFromWatchlistClick: (musicId: String, watchlistId: Long) -> Unit
) {
    val userPlaylists by libraryViewModel.userPlaylists.collectAsState()
    val userWatchlists by libraryViewModel.userWatchlists.collectAsState()
    val favoriteMusic by musicViewModel.favoriteMusic.collectAsState()
    val isLoading by libraryViewModel.isLoading.collectAsState()
    val error by libraryViewModel.error.collectAsState()
    var selectedTab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var selectedMusicForPlaylist by remember { mutableStateOf<Music?>(null) }
    var selectedPlaylistId by remember { mutableStateOf<Long?>(null) }
    val context = LocalContext.current

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Load playlists and watchlists when the screen is displayed
    LaunchedEffect(userId) {
        libraryViewModel.loadUserPlaylists(userId)
        libraryViewModel.loadUserWatchlists(userId)
        musicViewModel.loadFavorites()
    }

    // Show error message if any
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Short
            )
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onCreatePlaylist = { name, description ->
                scope.launch {
                    libraryViewModel.createLibraryItem(name, description, userId, true)
                    snackbarHostState.showSnackbar(
                        message = "Playlist '$name' created successfully",
                        duration = SnackbarDuration.Short
                    )
                    // Reload playlists to show the new one
                    libraryViewModel.loadUserPlaylists(userId)
                }
                showCreatePlaylistDialog = false
            }
        )
    }

    if (showAddToPlaylistDialog && selectedMusicForPlaylist != null) {
        AddToPlaylistDialog(
            playlists = userPlaylists,
            onDismiss = { showAddToPlaylistDialog = false },
            onPlaylistSelected = { playlist ->
                selectedMusicForPlaylist?.let { music ->
                    scope.launch {
                        libraryViewModel.addMusicToLibraryItem(playlist.id, music, true)
                        snackbarHostState.showSnackbar(
                            message = "Added to ${playlist.name}",
                            duration = SnackbarDuration.Short
                        )
                    }
                }
                showAddToPlaylistDialog = false
            },
            onCreateNewPlaylist = { name, description ->
                scope.launch {
                    libraryViewModel.createLibraryItem(name, description, userId, true)
                    // Reload playlists to get the new one
                    libraryViewModel.loadUserPlaylists(userId)

                    // Wait a bit for the playlist to be created
                    kotlinx.coroutines.delay(500)

                    // Find the newly created playlist (assuming it's the last one)
                    val newPlaylist = libraryViewModel.userPlaylists.value.lastOrNull()

                    selectedMusicForPlaylist?.let { music ->
                        if (newPlaylist != null) {
                            libraryViewModel.addMusicToLibraryItem(newPlaylist.id, music, true)
                            snackbarHostState.showSnackbar(
                                message = "Created playlist '$name' and added ${music.title}",
                                duration = SnackbarDuration.Short
                            )
                        } else {
                            snackbarHostState.showSnackbar(
                                message = "Created playlist '$name'",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                }
                showAddToPlaylistDialog = false
            }
        )
    }

    if (selectedPlaylistId != null) {
        PlaylistDetailsScreen(
            playlistId = selectedPlaylistId!!,
            libraryViewModel = libraryViewModel,
            musicViewModel = musicViewModel,
            onBackClick = { selectedPlaylistId = null },
            onMusicClick = onMusicClick
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Your Library", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black
                    ),
                    actions = {
                        Button(
                            onClick = { showCreatePlaylistDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Green
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Playlist",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Playlist", color = Color.White)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { /* Handle back navigation */ }) {
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
                        selected = selectedTab == LibraryTab.PLAYLISTS,
                        onClick = { selectedTab = LibraryTab.PLAYLISTS },
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
                        selected = selectedTab == LibraryTab.WATCHLIST,
                        onClick = { selectedTab = LibraryTab.WATCHLIST },
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

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Green)
                    }
                } else {
                    when (selectedTab) {
                        LibraryTab.PLAYLISTS -> {
                            val playlistWithMusicMap = libraryViewModel.playlistWithMusicList
                                .associateBy { it.playlist.id }

                            if (userPlaylists.isEmpty()) {
                                EmptyPlaylistsView(onCreatePlaylistClick = { showCreatePlaylistDialog = true })
                            } else {
                                PlaylistsGridView(
                                    playlists = userPlaylists,
                                    playlistWithMusicMap = playlistWithMusicMap,
                                    onPlaylistClick = { playlistId ->
                                        // Set the selected playlist ID to show the details screen
                                        selectedPlaylistId = playlistId
                                    },
                                    onEditPlaylist = { playlistId ->
                                        scope.launch {
                                            libraryViewModel.loadLibraryItem(playlistId, true)
                                            // Navigate to edit screen or show edit dialog
                                        }
                                    },
                                    onDeletePlaylist = { playlist ->
                                        scope.launch {
                                            libraryViewModel.deleteLibraryItem(playlist, true)
                                            snackbarHostState.showSnackbar(
                                                message = "Playlist '${playlist.name}' deleted",
                                                duration = SnackbarDuration.Short
                                            )
                                            // Reload playlists to update UI
                                            libraryViewModel.loadUserPlaylists(userId)
                                        }
                                    },
                                    onSharePlaylist = { playlistId ->
                                        val playlist = userPlaylists.find { it.id == playlistId }
                                        playlist?.let {
                                            val shareText = "Check out my playlist '${it.name}' on ArifMusic! 🎶"
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Playlist"))
                                        }
                                    }
                                )
                            }
                        }

                        LibraryTab.WATCHLIST -> {
                            if (favoriteMusic.isEmpty()) {
                                EmptyWatchlistView()
                            } else {
                                WatchlistView(
                                    favoriteMusic = favoriteMusic,
                                    onMusicClick = onMusicClick,
                                    onAddToPlaylistClick = { music ->
                                        selectedMusicForPlaylist = music
                                        showAddToPlaylistDialog = true
                                    },
                                    onRemoveFromWatchlistClick = { music ->
                                        val watchlistId = userWatchlists.firstOrNull()?.id ?: 0L
                                        scope.launch {
                                            // First remove from watchlist in the repository
                                            libraryViewModel.removeMusicFromLibraryItem(watchlistId, music.id, false)

                                            // Then toggle favorite status in music repository
                                            musicViewModel.toggleFavorite(music.id)

                                            snackbarHostState.showSnackbar(
                                                message = "Removed from watchlist",
                                                duration = SnackbarDuration.Short
                                            )

                                            // Reload favorites to update UI
                                            musicViewModel.loadFavorites()
                                        }
                                    },
                                    onShareMusic = { music ->
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
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistsGridView(
    playlists: List<Playlist>,
    playlistWithMusicMap: Map<Long, PlaylistWithMusic>,
    onPlaylistClick: (Long) -> Unit,
    onEditPlaylist: (Long) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onSharePlaylist: (Long) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(playlists) { playlist ->
            val playlistWithMusic = playlistWithMusicMap[playlist.id]
            PlaylistGridItem(
                playlist = playlist,
                musicCount = playlistWithMusic?.musicList?.size ?: 0,
                onPlaylistClick = onPlaylistClick,
                onEditPlaylist = onEditPlaylist,
                onDeletePlaylist = onDeletePlaylist,
                onSharePlaylist = onSharePlaylist
            )
        }
    }
}

@Composable
fun PlaylistGridItem(
    playlist: Playlist,
    musicCount: Int,
    onPlaylistClick: (Long) -> Unit,
    onEditPlaylist: (Long) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onSharePlaylist: (Long) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPlaylistClick(playlist.id) },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.coverArtUrl != null) {
                    val imageUri = remember(playlist.coverArtUrl) {
                        SimpleMediaAccessHelper.getPlayableUri(context, playlist.coverArtUrl)
                            ?: playlist.coverArtUrl.toUri()
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
                            Log.e("PlaylistGridItem", "Error loading image: ${playlist.coverArtUrl}")
                        }
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Play button overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Green)
                        .clickable { onPlaylistClick(playlist.id) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist.name,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "$musicCount songs",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color.White
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(Color(0xFF333333))
        ) {
            DropdownMenuItem(
                text = { Text("Edit", color = Color.White) },
                onClick = {
                    onEditPlaylist(playlist.id)
                    showMenu = false
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            )

            DropdownMenuItem(
                text = { Text("Delete", color = Color.White) },
                onClick = {
                    onDeletePlaylist(playlist)
                    showMenu = false
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
                    onSharePlaylist(playlist.id)
                    showMenu = false
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

@Composable
fun WatchlistView(
    favoriteMusic: List<Music>,
    onMusicClick: (Music) -> Unit,
    onAddToPlaylistClick: (Music) -> Unit,
    onRemoveFromWatchlistClick: (Music) -> Unit,
    onShareMusic: (Music) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(favoriteMusic) { music ->
            MusicListItem(
                music = music,
                onMusicClick = onMusicClick,
                onMoreClick = { showMenu ->
                    // Show dropdown menu with options
                    if (showMenu) {
                        DropdownMenuItem(
                            text = { Text("Add to Playlist", color = Color.White) },
                            onClick = { onAddToPlaylistClick(music) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PlaylistAdd,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Remove", color = Color.White) },
                            onClick = { onRemoveFromWatchlistClick(music) },
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
                            onClick = { onShareMusic(music) },
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
            )
        }
    }
}

@Composable
fun MusicListItem(
    music: Music,
    onMusicClick: (Music) -> Unit,
    onMoreClick: (@Composable (Boolean) -> Unit)
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
                // Use SimpleMediaAccessHelper to resolve the URI
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
                        Log.e("MusicListItem", "Error loading image: ${music.artworkUri}")
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
                onMoreClick(true)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreatePlaylist: (name: String, description: String) -> Unit
) {
    var playlistName by remember { mutableStateOf("") }
    var playlistDescription by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF222222)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Create New Playlist",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Give your playlist a name and description.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )

                Text(
                    text = "Name",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    placeholder = { Text("My Awesome Playlist", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Green,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Description (optional)",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = playlistDescription,
                    onValueChange = { playlistDescription = it },
                    placeholder = { Text("A collection of my favorite songs", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Green,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (playlistName.isNotEmpty()) {
                                onCreatePlaylist(playlistName, playlistDescription)
                            }
                        },
                        enabled = playlistName.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green,
                            contentColor = Color.White,
                            disabledContainerColor = Green.copy(alpha = 0.5f)
                        )
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPlaylistsView(onCreatePlaylistClick: () -> Unit) {
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
                text = "No playlists yet!",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create your first playlist to organize your favorite music.",
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onCreatePlaylistClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Playlist", color = Color.White)
            }
        }
    }
}

@Composable
fun EmptyWatchlistView() {
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
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                tint = Green,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Your watchlist is empty",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add songs to your watchlist by tapping the bookmark icon.",
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onCreateNewPlaylist: (name: String, description: String) -> Unit
) {
    var showCreateNewPlaylist by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDescription by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist", color = Color.White) },
        text = {
            if (showCreateNewPlaylist) {
                Column {
                    Text(
                        text = "Create New Playlist",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("Playlist Name", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newPlaylistDescription,
                        onValueChange = { newPlaylistDescription = it },
                        label = { Text("Description (optional)", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Green,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Column {
                    if (playlists.isEmpty()) {
                        Text(
                            text = "You don't have any playlists yet.",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            items(playlists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onPlaylistSelected(playlist) }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistPlay,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = playlist.name,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        )

                                        Text(
                                            text = playlist.description,
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Divider(color = Color.DarkGray.copy(alpha = 0.5f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showCreateNewPlaylist = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Green
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Playlist")
                    }
                }
            }
        },
        confirmButton = {
            if (showCreateNewPlaylist) {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            onCreateNewPlaylist(newPlaylistName, newPlaylistDescription)
                        }
                    },
                    enabled = newPlaylistName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green,
                        disabledContainerColor = Green.copy(alpha = 0.5f)
                    )
                ) {
                    Text("Create")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = Color.White)
                }
            }
        },
        dismissButton = {
            if (showCreateNewPlaylist) {
                TextButton(
                    onClick = { showCreateNewPlaylist = false }
                ) {
                    Text("Back", color = Color.White)
                }
            }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

// Helper function to format duration
fun formatDuration(durationMs: Long): String {
    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) -
            TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%d:%02d", minutes, seconds)
}
