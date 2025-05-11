package com.arifmusic.app.ui.screens.home

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.arifmusic.app.data.model.Music
import com.arifmusic.app.data.model.Playlist
import com.arifmusic.app.data.model.User
import com.arifmusic.app.data.model.Watchlist
import com.arifmusic.app.ui.theme.Green
import com.arifmusic.app.ui.viewmodel.ExploreViewModel
import com.arifmusic.app.ui.viewmodel.FollowViewModel
import com.arifmusic.app.ui.viewmodel.LibraryViewModel
import com.arifmusic.app.ui.viewmodel.MusicViewModel
import com.arifmusic.app.ui.viewmodel.SessionViewModel
import com.arifmusic.app.utils.SimpleMediaAccessHelper
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    exploreViewModel: ExploreViewModel,
    musicViewModel: MusicViewModel,
    libraryViewModel: LibraryViewModel,
    sessionViewModel: SessionViewModel,
    followViewModel: FollowViewModel,
    onMusicClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onSeeAllClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onExploreClick: () -> Unit,
    onSeeAllNewReleasesClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onPlaylistClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val trendingMusic by exploreViewModel.trendingMusic.collectAsState()
    val featuredArtists by exploreViewModel.featuredArtists.collectAsState()
    val newReleases by exploreViewModel.newReleases.collectAsState()
    val isLoading by exploreViewModel.isLoading.collectAsState()
    val featuredBanner by exploreViewModel.featuredBanner.collectAsState()
    val featuredMusic by exploreViewModel.featuredMusic.collectAsState()
    val userPlaylists by libraryViewModel.userPlaylists.collectAsState()
    val userWatchlists by libraryViewModel.userWatchlists.collectAsState()
    val favoriteMusic by musicViewModel.favoriteMusic.collectAsState()
    val libraryError by libraryViewModel.error.collectAsState()
    val currentUser by sessionViewModel.currentUser.collectAsState()
    val context = LocalContext.current

    // State for showing all artists in grid view
    var showAllArtists by remember { mutableStateOf(false) }

    // State to track if data is refreshing
    var isRefreshing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Refresh user data when entering the screen
    LaunchedEffect(Unit) {
        isRefreshing = true
        currentUser?.id?.let { userId ->
            libraryViewModel.loadUserPlaylists(userId)
            libraryViewModel.loadUserWatchlists(userId)
            musicViewModel.loadFavorites()
            followViewModel.loadFollowedArtists(userId)
        }
        isRefreshing = false
    }

    // Show library errors in snackbar
    LaunchedEffect(libraryError) {
        libraryError?.let {
            snackbarHostState.showSnackbar(
                message = it,
                duration = SnackbarDuration.Long
            )
            libraryViewModel.clearError()
        }
    }

    // State to track which music items are showing the play icon on hover
    val hoverStateMap = remember { mutableStateMapOf<String, Boolean>() }

    // State for add to playlist dialog
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var selectedMusicForPlaylist by remember { mutableStateOf<Music?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDescription by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLoading || isRefreshing) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Green
            )
        } else {
            if (showAllArtists) {
                // Show all artists in grid view
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showAllArtists = false }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "Featured Artists",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(featuredArtists) { artist ->
                            ArtistGridItem(
                                artist = artist,
                                followViewModel = followViewModel,
                                onClick = { onArtistClick(artist.id) }
                            )
                        }
                    }
                }
            } else {
                // Regular home screen
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Search bar
                    item {
                        OutlinedTextField(
                            value = "",
                            onValueChange = { },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .height(48.dp)
                                .clickable { onSearchClick() },
                            placeholder = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Search songs, artists, playlists...",
                                        color = Color.Gray,
                                        fontSize = 14.sp
                                    )
                                }
                            },
                            enabled = false,
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                disabledTextColor = Color.Gray,
                                disabledBorderColor = Color.DarkGray,
                                disabledPlaceholderColor = Color.Gray,
                                disabledLeadingIconColor = Color.Gray,
                                containerColor = Color(0xFF1A1A1A)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                    }

                    // Featured Banner
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1A1A1A))
                        ) {
                            // Banner background image if available
                            featuredBanner?.imageUrl?.let { imageUrl ->
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl)
                                        .crossfade(true)
                                        .diskCachePolicy(CachePolicy.ENABLED)
                                        .memoryCachePolicy(CachePolicy.ENABLED)
                                        .build(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                    alpha = 0.7f
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = featuredBanner?.title ?: "Discover Ethiopian Music",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                featuredBanner?.description?.let { description ->
                                    Text(
                                        text = description,
                                        color = Color.LightGray,
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = {
                                            // Play the featured music set by admin
                                            featuredMusic?.let { music ->
                                                musicViewModel.playMusic(music)
                                                onMusicClick(music.id)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Green
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Play Featured")
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    OutlinedButton(
                                        onClick = { onExploreClick() },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    ) {
                                        Text("Explore")
                                    }
                                }
                            }
                        }
                    }

                    // Trending Now
                    item {
                        SectionHeader(
                            title = "Trending Now",
                            onSeeAllClick = { onSeeAllClick("trending") }
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(trendingMusic) { music ->
                                MusicItem(
                                    music = music,
                                    onClick = {
                                        // When card is clicked, show the play icon
                                        hoverStateMap[music.id] = true
                                    },
                                    onPlayClick = {
                                        musicViewModel.playMusic(music)
                                        onMusicClick(music.id)
                                    },
                                    onMenuItemClick = { action ->
                                        currentUser?.id?.let { userId ->
                                            handleMenuAction(
                                                action = action,
                                                music = music,
                                                musicViewModel = musicViewModel,
                                                libraryViewModel = libraryViewModel,
                                                userId = userId,
                                                context = context,
                                                onAddToPlaylist = {
                                                    selectedMusicForPlaylist = music
                                                    showPlaylistDialog = true
                                                },
                                                snackbarHostState = snackbarHostState,
                                                scope = scope,
                                                userWatchlists = userWatchlists,
                                                favoriteMusic = favoriteMusic
                                            )
                                        }
                                    },
                                    onHoverChanged = { isHovered ->
                                        hoverStateMap[music.id] = isHovered
                                    },
                                    isHovered = hoverStateMap[music.id] ?: false,
                                    isFavorite = favoriteMusic.any { it.id == music.id }
                                )
                            }
                        }
                    }

                    // Featured Artists Section Header
                    item {
                        SectionHeader(
                            title = "Featured Artists",
                            onSeeAllClick = { showAllArtists = true }
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(featuredArtists) { artist ->
                                ArtistItem(
                                    artist = artist,
                                    followViewModel = followViewModel,
                                    onClick = { onArtistClick(artist.id) }
                                )
                            }
                        }
                    }

                    // New Releases
                    item {
                        SectionHeader(
                            title = "New Releases",
                            onSeeAllClick = { onSeeAllNewReleasesClick() }
                        )
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(newReleases) { music ->
                                MusicItem(
                                    music = music,
                                    onClick = {
                                        // When card is clicked, show the play icon
                                        hoverStateMap[music.id] = true
                                    },
                                    onPlayClick = {
                                        musicViewModel.playMusic(music)
                                        onMusicClick(music.id)
                                    },
                                    onMenuItemClick = { action ->
                                        currentUser?.id?.let { userId ->
                                            handleMenuAction(
                                                action = action,
                                                music = music,
                                                musicViewModel = musicViewModel,
                                                libraryViewModel = libraryViewModel,
                                                userId = userId,
                                                context = context,
                                                onAddToPlaylist = {
                                                    selectedMusicForPlaylist = music
                                                    showPlaylistDialog = true
                                                },
                                                snackbarHostState = snackbarHostState,
                                                scope = scope,
                                                userWatchlists = userWatchlists,
                                                favoriteMusic = favoriteMusic
                                            )
                                        }
                                    },
                                    onHoverChanged = { isHovered ->
                                        hoverStateMap[music.id] = isHovered
                                    },
                                    isHovered = hoverStateMap[music.id] ?: false,
                                    isFavorite = favoriteMusic.any { it.id == music.id }
                                )
                            }
                        }
                    }
                }
            }

            // Snackbar host for notifications
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 80.dp)
            )
        }

        // Add to playlist dialog
        if (showPlaylistDialog && selectedMusicForPlaylist != null) {
            AddToPlaylistDialog(
                playlists = userPlaylists,
                onDismiss = { showPlaylistDialog = false },
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
                    showPlaylistDialog = false
                },
                onCreateNewPlaylist = {
                    showPlaylistDialog = false
                    showCreatePlaylistDialog = true
                }
            )
        }

        // Create new playlist dialog
        if (showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onDismiss = { showCreatePlaylistDialog = false },
                onCreatePlaylist = { name, description ->
                    currentUser?.id?.let { userId ->
                        scope.launch {
                            libraryViewModel.createLibraryItem(name, description, userId, true)
                            // After creating the playlist, we need to reload playlists
                            libraryViewModel.loadUserPlaylists(userId)

                            selectedMusicForPlaylist?.let { music ->
                                // We need to wait a bit for the playlist to be created and loaded
                                kotlinx.coroutines.delay(500)

                                // Find the newly created playlist (assuming it's the last one)
                                val newPlaylist = libraryViewModel.userPlaylists.value.lastOrNull()
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
                    }
                    showCreatePlaylistDialog = false
                },
                playlistName = newPlaylistName,
                playlistDescription = newPlaylistDescription,
                onPlaylistNameChange = { newPlaylistName = it },
                onPlaylistDescriptionChange = { newPlaylistDescription = it }
            )
        }
    }
}

@Composable
fun MusicItem(
    music: Music,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onMenuItemClick: (String) -> Unit,
    onHoverChanged: (Boolean) -> Unit,
    isHovered: Boolean,
    isFavorite: Boolean,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .width(120.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1A1A1A))
        ) {
            // Music artwork with improved image loading
            music.artworkUri?.let { uri ->
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(uri)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = music.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } ?: run {
                // Placeholder if no artwork
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2A2A2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            if (isHovered) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Green)
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Music info with overflow menu
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = music.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = music.artist,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Play count
                Text(
                    text = formatPlayCount(music.playCount),
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Overflow menu (three dots)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(Color(0xFF1A1A1A))
                ) {
                    // Add to Watchlist option with heart icon
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = if (isFavorite) "Remove from Watchlist" else "Add to Watchlist",
                                color = Color.White
                            )
                        },
                        onClick = {
                            onMenuItemClick("watchlist")
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = Color.Red
                            )
                        }
                    )

                    // Add to Playlist option
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = Color.White) },
                        onClick = {
                            onMenuItemClick("playlist")
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PlaylistAdd,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    )

                    // Share option
                    DropdownMenuItem(
                        text = { Text("Share", color = Color.White) },
                        onClick = {
                            onMenuItemClick("share")
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
    }
}

@Composable
fun ArtistItem(
    artist: User,
    followViewModel: FollowViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFollowing by followViewModel.isFollowingArtist(artist.id).collectAsState(initial = false)
    val followerCount by followViewModel.getArtistFollowerCount(artist.id).collectAsState(initial = 0)

    Column(
        modifier = modifier
            .width(80.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            // Artist profile image
            if (!artist.profileImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artist.profileImageUrl)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback to initial letter
                Text(
                    text = artist.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = artist.name,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Follower count
        Text(
            text = "$followerCount followers",
            color = Color.Gray,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ArtistGridItem(
    artist: User,
    followViewModel: FollowViewModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFollowing by followViewModel.isFollowingArtist(artist.id).collectAsState(initial = false)
    val followerCount by followViewModel.getArtistFollowerCount(artist.id).collectAsState(initial = 0)
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .width(100.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            // Artist profile image
            if (!artist.profileImageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(artist.profileImageUrl)
                        .crossfade(true)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .build(),
                    contentDescription = artist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Fallback to initial letter
                Text(
                    text = artist.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            color = Color.White,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Follower count
        Text(
            text = "$followerCount followers",
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Follow/Unfollow button
        Button(
            onClick = {
                scope.launch {
                    if (isFollowing) {
                        followViewModel.unfollowArtist(artist.id)
                    } else {
                        followViewModel.followArtist(artist.id)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isFollowing) Color.DarkGray else Green
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.height(32.dp)
        ) {
            Text(
                text = if (isFollowing) "Following" else "Follow",
                fontSize = 12.sp
            )
        }
    }
}

private fun handleMenuAction(
    action: String,
    music: Music,
    musicViewModel: MusicViewModel,
    libraryViewModel: LibraryViewModel,
    userId: String,
    context: Context,
    onAddToPlaylist: () -> Unit,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    userWatchlists: List<Watchlist>,
    favoriteMusic: List<Music>
) {
    when (action) {
        "playlist" -> onAddToPlaylist()
        "share" -> shareMusic(context, music)
        "watchlist" -> {
            scope.launch {
                try {
                    // Check if the music is already in the user's favorites
                    val isAlreadyFavorite = favoriteMusic.any { it.id == music.id }

                    // Get or create watchlist
                    var watchlist = userWatchlists.firstOrNull()

                    if (watchlist == null) {
                        libraryViewModel.createLibraryItem("My Watchlist", "Favorite songs", userId, false)

                        kotlinx.coroutines.delay(800)

                        libraryViewModel.loadUserWatchlists(userId)

                        kotlinx.coroutines.delay(500)

                        watchlist = libraryViewModel.userWatchlists.value.firstOrNull()
                    }

                    if (watchlist != null) {
                        if (!isAlreadyFavorite) {
                            // First update the music repository
                            musicViewModel.toggleFavorite(music.id)

                            // Then add to watchlist
                            libraryViewModel.addMusicToLibraryItem(watchlist.id, music, false)

                            snackbarHostState.showSnackbar(
                                message = "Added to watchlist",
                                duration = SnackbarDuration.Short
                            )
                        } else {
                            // First remove from watchlist
                            libraryViewModel.removeMusicFromLibraryItem(watchlist.id, music.id, false)

                            // Then update the music repository
                            musicViewModel.toggleFavorite(music.id)

                            snackbarHostState.showSnackbar(
                                message = "Removed from watchlist",
                                duration = SnackbarDuration.Short
                            )
                        }

                        // Reload favorites to update UI
                        musicViewModel.loadFavorites()
                    } else {
                        snackbarHostState.showSnackbar(
                            message = "Unable to create watchlist. Please try again.",
                            duration = SnackbarDuration.Long
                        )
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = "Error: ${e.message ?: "Unknown error occurred"}",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        TextButton(onClick = onSeeAllClick) {
            Text(
                text = "View all",
                color = Color.Gray,
                fontSize = 14.sp
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
    onCreateNewPlaylist: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Playlist", color = Color.White) },
        text = {
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

                                    if (playlist.description.isNotEmpty()) {
                                        Text(
                                            text = playlist.description,
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }

                            Divider(color = Color.DarkGray.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onCreateNewPlaylist,
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreatePlaylist: (name: String, description: String) -> Unit,
    playlistName: String,
    playlistDescription: String,
    onPlaylistNameChange: (String) -> Unit,
    onPlaylistDescriptionChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Playlist", color = Color.White) },
        text = {
            Column {
                Text(
                    text = "Give your playlist a name and description.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Name",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedTextField(
                    value = playlistName,
                    onValueChange = onPlaylistNameChange,
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
                    onValueChange = onPlaylistDescriptionChange,
                    placeholder = { Text("A collection of my favorite songs", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Green,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (playlistName.isNotBlank()) {
                        onCreatePlaylist(playlistName, playlistDescription)
                    }
                },
                enabled = playlistName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green,
                    disabledContainerColor = Green.copy(alpha = 0.5f)
                )
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = Color(0xFF1E1E1E),
        titleContentColor = Color.White,
        textContentColor = Color.White
    )
}

fun shareMusic(
    context: Context,
    music: Music
) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Check out this song!")
        putExtra(
            Intent.EXTRA_TEXT,
            "I'm listening to ${music.title} by ${music.artist} on ArifMusic. Check it out!"
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
}

// Format play count to display as "950K plays" or "1.2M plays"
private fun formatPlayCount(count: Int): String {
    return when {
        count >= 1_000_000 -> "${count / 1_000_000}M plays"
        count >= 1_000 -> "${count / 1_000}K plays"
        else -> "$count plays"
    }
}
