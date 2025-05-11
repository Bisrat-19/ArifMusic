package com.arifmusic.app.ui.screens.search

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import kotlinx.coroutines.launch

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.arifmusic.app.data.model.Music
import com.arifmusic.app.data.model.User
import com.arifmusic.app.ui.theme.Green
import com.arifmusic.app.ui.viewmodel.MusicViewModel
import com.arifmusic.app.ui.viewmodel.SearchViewModel
import kotlinx.coroutines.flow.collectLatest

enum class SearchTab { ALL, SONGS, ARTISTS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onMusicClick: (Music) -> Unit,
    onArtistClick: (User) -> Unit,
    onBackClick: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel,
    searchViewModel: SearchViewModel
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 1. Collect StateFlows
    val query by viewModel.searchQuery.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val musicResults by viewModel.musicResults.collectAsState()
    val artistResults by viewModel.artistResults.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()

    var selectedTab by remember { mutableStateOf(SearchTab.ALL) }
    var searchActive by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.snackbarMessage.collectLatest { message ->
            message?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.onSnackbarShown()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            SearchBar(
                query = query,
                onQueryChange = {
                    viewModel.search(it)
                },
                onSearch = {
                    viewModel.search(it)
                    searchActive = false
                },
                active = searchActive,
                onActiveChange = { searchActive = it },
                placeholder = { Text("Search songs, artists...") },
                leadingIcon = {
                    if (searchActive) IconButton(onClick = { searchActive = false }) {
                        Icon(Icons.Default.ArrowBack, null, tint = Color.White)
                    } else {
                        Icon(Icons.Default.Search, null, tint = Color.White)
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = {
                            viewModel.clearSearch()
                            scope.launch {
                                snackbarHostState.showSnackbar("Cleared search")
                            }
                        }) {
                            Icon(Icons.Default.Clear, null, tint = Color.White)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = SearchBarDefaults.colors(
                    containerColor = Color(0xFF1E1E1E),
                    dividerColor = Color.DarkGray,
                    inputFieldColors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Green
                    )
                )
            ) {
                if (recentSearches.isNotEmpty()) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recent Searches", color = Color.White, fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                viewModel.clearRecentSearches()
                                scope.launch {
                                    snackbarHostState.showSnackbar("Search history cleared")
                                }
                            }) {
                                Text("Clear All", color = Green)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        LazyColumn {
                            items(recentSearches) { recent ->
                                Text(
                                    text = recent,
                                    color = Color.White,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.search(recent)
                                            searchActive = false
                                        }
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (!searchActive) {
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color.Transparent,
                    contentColor = Green
                ) {
                    Tab(
                        selected = selectedTab == SearchTab.ALL,
                        onClick = {
                            selectedTab = SearchTab.ALL
                            if (query.isNotBlank()) viewModel.search(query)
                        },
                        text = { Text("All") }
                    )
                    Tab(
                        selected = selectedTab == SearchTab.SONGS,
                        onClick = {
                            selectedTab = SearchTab.SONGS
                            if (query.isNotBlank()) viewModel.search(query)
                        },
                        text = { Text("Songs") }
                    )
                    Tab(
                        selected = selectedTab == SearchTab.ARTISTS,
                        onClick = {
                            selectedTab = SearchTab.ARTISTS
                            if (query.isNotBlank()) viewModel.search(query)
                        },
                        text = { Text("Artists") }
                    )
                }
            }

            // D. Loading spinner
            AnimatedVisibility(visible = isSearching, enter = fadeIn(), exit = fadeOut()) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    query.isNotEmpty() && !isSearching -> {
                        when (selectedTab) {
                            SearchTab.ALL -> AllResults(musicResults, artistResults, onMusicClick, onArtistClick)
                            SearchTab.SONGS -> SongsResults(musicResults, onMusicClick)
                            SearchTab.ARTISTS -> ArtistsResults(artistResults, onArtistClick)
                        }
                    }
                    query.isEmpty() && !isSearching -> {
                        Placeholder { viewModel.search(it) }
                    }
                }
            }
        }
    }
}

@Composable
fun Placeholder(onSearch: (String) -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Search for your favorite music,artists...",
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onSearch("") }, colors = ButtonDefaults.buttonColors(containerColor = Green)) {
                Icon(imageVector = Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Searching")
            }
        }
    }
}

@Composable
fun AllResults(
    musicResults: List<Music>,
    artistResults: List<User>,
    onMusicClick: (Music) -> Unit,
    onArtistClick: (User) -> Unit
) {
    Column {
        if (musicResults.isEmpty() && artistResults.isEmpty()) {
            NoResultsFound("")
        } else {
            // Music results
            LazyColumn {
                items(musicResults) { music ->
                    MusicResultItem(music) {
                        onMusicClick(music) // Wrapping the function in a lambda
                    }
                }
            }
            // Artist results
            LazyColumn {
                items(artistResults) { artist ->
                    ArtistResultItem(artist) {
                        onArtistClick(artist) // Wrapping the function in a lambda
                    }
                }
            }
        }
    }
}

// 2. SongsResults Composable
@Composable
fun SongsResults(
    musicResults: List<Music>,
    onMusicClick: (Music) -> Unit
) {
    if (musicResults.isEmpty()) {
        NoResultsFound("")
    } else {
        LazyColumn {
            items(musicResults) { music ->
                MusicResultItem(music) {
                    onMusicClick(music) // Wrapping the function in a lambda
                }
            }
        }
    }
}

// 3. ArtistsResults Composable
@Composable
fun ArtistsResults(
    artistResults: List<User>,
    onArtistClick: (User) -> Unit
) {
    if (artistResults.isEmpty()) {
        NoResultsFound("")
    } else {
        LazyColumn {
            items(artistResults) { artist ->
                ArtistResultItem(artist) {
                    onArtistClick(artist) // Wrapping the function in a lambda
                }
            }
        }
    }
}

// 4. NoResultsFound Composable
@Composable
fun NoResultsFound(query: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No results found for \"$query\"",
                color = Color.White,
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try different keywords or check your spelling",
                color = Color.Gray,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// 6. MusicResultItem Composable
@Composable
fun MusicResultItem(
    music: Music,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF2A2A2A)
        ) {
            if (music.artworkUri != null) {
                AsyncImage(
                    model = music.artworkUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Green,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = music.title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${music.artist} • ${music.album}",
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Text(
            text = formatDuration(music.duration),
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

// 7. ArtistResultItem Composable
@Composable
fun ArtistResultItem(
    artist: User,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            if (artist.profilePictureUrl != null) {
                AsyncImage(
                    model = artist.profilePictureUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = artist.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${artist.followerCount} followers",
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (artist.isVerified) {
            Icon(
                imageVector = Icons.Default.Verified,
                contentDescription = "Verified Artist",
                tint = Green,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// 8. formatDuration function
private fun formatDuration(durationMs: Long): String {
    val minutes = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(durationMs)
    val seconds = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(durationMs) -
            java.util.concurrent.TimeUnit.MINUTES.toSeconds(minutes)
    return String.format("%d:%02d", minutes, seconds)
}
