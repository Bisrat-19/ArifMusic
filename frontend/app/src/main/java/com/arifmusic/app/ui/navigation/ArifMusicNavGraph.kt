package com.arifmusic.app.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.arifmusic.app.data.model.UserType
import com.arifmusic.app.data.repository.SessionManager
import com.arifmusic.app.ui.screens.admin.*
import com.arifmusic.app.ui.screens.artist.*
import com.arifmusic.app.ui.screens.explore.ExploreScreen
import com.arifmusic.app.ui.screens.music.FavoriteScreen
import com.arifmusic.app.ui.screens.forgotpassword.ForgotPasswordScreen
import com.arifmusic.app.ui.screens.home.HomeScreen
import com.arifmusic.app.ui.screens.library.LibraryScreen
import com.arifmusic.app.ui.screens.listener.ListenerProfileScreen
import com.arifmusic.app.ui.screens.login.LoginScreen
import com.arifmusic.app.ui.screens.music.MusicListScreen
import com.arifmusic.app.ui.screens.player.PlayerScreen
import com.arifmusic.app.ui.screens.profile.*
import com.arifmusic.app.ui.screens.register.RegisterScreen
import com.arifmusic.app.ui.screens.search.SearchScreen
import com.arifmusic.app.ui.screens.welcome.WelcomeScreen
import com.arifmusic.app.ui.viewmodel.*
import kotlinx.coroutines.launch

@Composable
fun ArifMusicNavGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    sessionManager: SessionManager,
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val musicViewModel: MusicViewModel = hiltViewModel()
    val exploreViewModel: ExploreViewModel = hiltViewModel()
    val searchViewModel: SearchViewModel = hiltViewModel()
    val userViewModel: UserViewModel = hiltViewModel()
    val artistViewModel: ArtistViewModel = hiltViewModel()
    val libraryViewModel: LibraryViewModel = hiltViewModel()
    val adminViewModel: AdminViewModel = hiltViewModel()
    val followViewModel: FollowViewModel = hiltViewModel()
    val notificationViewModel: NotificationViewModel = hiltViewModel()

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(ArifMusicScreens.Welcome.name) {
            WelcomeScreen(
                onLoginClick = { navController.navigate(ArifMusicScreens.Login.name) },
                onRegisterClick = { navController.navigate(ArifMusicScreens.Register.name) },
                onGuestClick = {
                    // Set user as guest in session manager
                    navController.navigate(ArifMusicScreens.Home.name) {
                        popUpTo(ArifMusicScreens.Welcome.name) { inclusive = true }
                    }
                }
            )
        }

        composable(ArifMusicScreens.Login.name) {
            LoginScreen(
                authViewModel = authViewModel,
                onLoginSuccess = {
                    navController.navigate(ArifMusicScreens.Home.name) {
                        popUpTo(ArifMusicScreens.Welcome.name) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
                onSignUpClick = { navController.navigate(ArifMusicScreens.Register.name) },
                onForgotPasswordClick = {
                    navController.navigate(ArifMusicScreens.ForgotPassword.name)
                },
            )
        }

        composable(ArifMusicScreens.ForgotPassword.name) {
            ForgotPasswordScreen(
                viewModel = hiltViewModel(),
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() },
                onResetSuccess = { navController.navigate(ArifMusicScreens.Login.name) },
            )
        }

        // Register Screen with optional "from" parameter to track where user came from
        composable(
            route = "${ArifMusicScreens.Register.name}?from={from}",
            arguments = listOf(
                navArgument("from") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val fromScreen = backStackEntry.arguments?.getString("from")

            RegisterScreen(
                authViewModel = authViewModel,
                viewModel = hiltViewModel(),
                onRegisterSuccess = {
                    navController.navigate(ArifMusicScreens.Home.name) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                // Handle back button based on where user came from
                onBackClick = {
                    if (fromScreen != null) {
                        // If redirected from Library or Profile, go back to Home
                        navController.navigate(ArifMusicScreens.Home.name) {
                            popUpTo(ArifMusicScreens.Home.name) { inclusive = false }
                        }
                    } else {
                        // Normal back behavior
                        navController.popBackStack()
                    }
                },
                onLoginClick = { navController.navigate(ArifMusicScreens.Login.name) }
            )
        }

        composable(ArifMusicScreens.Home.name) {
            val exploreViewModel: ExploreViewModel = hiltViewModel()
            val libraryViewModel: LibraryViewModel = hiltViewModel()
            val sessionViewModel: SessionViewModel = hiltViewModel()
            val followViewModel: FollowViewModel = hiltViewModel()

            HomeScreen(
                exploreViewModel = exploreViewModel,
                musicViewModel = musicViewModel,
                libraryViewModel = libraryViewModel,
                sessionViewModel = sessionViewModel,
                onMusicClick = { musicId ->
                    musicViewModel.loadMusic(musicId)
                    navController.navigate("${ArifMusicScreens.Player.name}/$musicId")
                },
                onArtistClick = { artistId ->
                    navController.navigate("${ArifMusicScreens.ArtistDetail.name}/$artistId")
                },
                onSearchClick = {
                    navController.navigate(ArifMusicScreens.Search.name)
                },
                onExploreClick = {
                    navController.navigate(ArifMusicScreens.Explore.name)
                },
                onSeeAllNewReleasesClick = {
                    navController.navigate("${ArifMusicScreens.MusicList.name}/new")
                },
                onNotificationClick = {
                    navController.navigate(ArifMusicScreens.Notification.name)
                },
                onSeeAllClick = { section ->
                    navController.navigate("${ArifMusicScreens.MusicList.name}/$section")
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate("${ArifMusicScreens.PlaylistDetail.name}/$playlistId")
                },
                modifier = Modifier,
                followViewModel = followViewModel,
            )
        }

        composable(ArifMusicScreens.Search.name) {
            SearchScreen(
                onMusicClick = { music ->
                    musicViewModel.loadMusic(music.id)
                    navController.navigate("${ArifMusicScreens.Player.name}/${music.id}")
                },
                onArtistClick = { artist ->
                    navController.navigate("${ArifMusicScreens.ArtistDetail.name}/${artist.id}")
                },
                onBackClick = { navController.popBackStack() },
                musicViewModel = musicViewModel,
                searchViewModel = searchViewModel
            )
        }

        composable(ArifMusicScreens.Explore.name) {
            ExploreScreen(
                exploreViewModel = exploreViewModel,
                onSearchClick = { navController.navigate(ArifMusicScreens.Search.name) },
                onMusicClick = { music ->
                    musicViewModel.loadMusic(music.id)
                    navController.navigate("${ArifMusicScreens.Player.name}/${music.id}")
                },
                onArtistClick = { artist ->
                    navController.navigate("${ArifMusicScreens.ArtistDetail.name}/${artist.id}")
                },
                onViewAllTrendingClick = {
                    navController.navigate("${ArifMusicScreens.MusicList.name}/trending")
                },
                onViewAllArtistsClick = {
                    navController.navigate("${ArifMusicScreens.MusicList.name}/artists")
                },
                onViewAllNewReleasesClick = {
                    navController.navigate("${ArifMusicScreens.MusicList.name}/new")
                },
                onPlayFeaturedClick = { musicId ->
                    musicViewModel.loadMusic(musicId)
                    navController.navigate("${ArifMusicScreens.Player.name}/$musicId")
                },
                onExploreClick = { }
            )
        }

        composable(ArifMusicScreens.Library.name) {
            val currentUser by authViewModel.currentUser.collectAsState()
            val isGuest = currentUser?.userType == UserType.GUEST

            LaunchedEffect(isGuest) {
                if (isGuest) {
                    navController.navigate(ArifMusicScreens.Welcome.name) {
                        popUpTo(0) { inclusive = true }                    }
                }
            }

            if (!isGuest) {
                val userId = currentUser?.id ?: return@composable
                val scope = rememberCoroutineScope()
                val snackbarHostState = remember { SnackbarHostState() }

                LibraryScreen(
                    libraryViewModel = libraryViewModel,
                    musicViewModel = musicViewModel,
                    userId = userId,
                    onPlaylistClick = { playlistId ->
                        navController.navigate("${ArifMusicScreens.PlaylistDetail.name}/$playlistId")
                    },
                    onCreatePlaylistClick = {
                        // Implement playlist creation
                    },
                    onMusicClick = { music ->
                        musicViewModel.playMusic(music)
                        navController.navigate("${ArifMusicScreens.Player.name}/${music.id}")
                    },
                    onAddToPlaylistClick = { music, playlistId ->
                        scope.launch {
                            libraryViewModel.addMusicToLibraryItem(playlistId, music, true)
                            snackbarHostState.showSnackbar(
                                message = "Added to playlist",
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    onRemoveFromWatchlistClick = { musicId, watchlistId ->
                        scope.launch {
                            libraryViewModel.removeMusicFromLibraryItem(watchlistId, musicId, false)
                            musicViewModel.toggleFavorite(musicId)
                            snackbarHostState.showSnackbar(
                                message = "Removed from watchlist",
                                duration = SnackbarDuration.Short
                            )
                            musicViewModel.loadFavorites()
                        }
                    }
                )
            }
        }

        composable(
            route = "${ArifMusicScreens.Player.name}/{musicId}",
            arguments = listOf(navArgument("musicId") { type = NavType.StringType })
        ) { backStackEntry ->
            val musicId = backStackEntry.arguments?.getString("musicId") ?: ""
            val context = LocalContext.current
            LaunchedEffect(musicId) {
                musicViewModel.loadMusic(musicId)
            }

            PlayerScreen(
                musicViewModel = musicViewModel,
                musicId = musicId,
                onBackClick = { navController.popBackStack() },
                onArtistClick = { artistId ->
                    navController.navigate("${ArifMusicScreens.ArtistDetail.name}/$artistId")
                },
                onShareClick = {
                    musicViewModel.currentMusic.value?.let { current ->
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out this song: ${current.title} by ${current.artist}"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                    }
                },
                onFavoriteClick = {
                    musicViewModel.currentMusic.value?.id?.let { id ->
                        musicViewModel.toggleFavorite(id)
                    }
                }
            )
        }

        composable(
            route = "${ArifMusicScreens.ArtistDetail.name}/{artistId}",
            arguments = listOf(navArgument("artistId") { type = NavType.StringType })
        ) { backStackEntry ->
            val artistId = backStackEntry.arguments?.getString("artistId") ?: ""
            ArtistDetailScreen(
                artistId = artistId,
                artistViewModel = artistViewModel,
                musicViewModel = musicViewModel,
                onBackClick = { navController.popBackStack() },
                onMusicClick = { music ->
                    musicViewModel.loadMusic(music.id)
                    navController.navigate("${ArifMusicScreens.Player.name}/${music.id}")
                },
                followViewModel = followViewModel,
                sessionManager = sessionManager
            )
        }

        composable(ArifMusicScreens.Profile.name) {
            val currentUser by authViewModel.currentUser.collectAsState()
            val isGuest = currentUser?.userType == UserType.GUEST
            LaunchedEffect(isGuest) {
                if (isGuest) {
                    navController.navigate(ArifMusicScreens.Welcome.name) {
                        popUpTo(ArifMusicScreens.Home.name) { inclusive = true }
                    }
                }
            }

            if (!isGuest) {
                when (currentUser?.userType) {
                    UserType.ADMIN -> {
                        AdminProfileScreen(
                            onAdminPanelClick = { navController.navigate(ArifMusicScreens.AdminPanel.name) },
                            onManageFeaturedClick = { navController.navigate(ArifMusicScreens.AdminFeaturedContent.name) },
                            onSettingsClick = { navController.navigate(ArifMusicScreens.Settings.name) },
                            onHelpAndSupportClick = { navController.navigate(ArifMusicScreens.HelpSupport.name) },
                            onAboutClick = { navController.navigate(ArifMusicScreens.About.name) },
                            onLogoutClick = {
                                authViewModel.logout()
                                navController.navigate(ArifMusicScreens.Welcome.name) {
                                    popUpTo(0) { inclusive = true }                                }
                            },
                            currentUser = currentUser,
                            onEditProfileClick = { navController.navigate(ArifMusicScreens.EditProfile.name) },
                            authViewModel = authViewModel
                        )
                    }

                    UserType.ARTIST -> {
                        ArtistProfileScreen(
                            onBackClick = { navController.popBackStack() },
                            onDashboardClick = { navController.navigate(ArifMusicScreens.ArtistDashboard.name) },
                            onEditProfileClick = { navController.navigate(ArifMusicScreens.EditProfile.name) },
                            onGetVerifiedClick = { navController.navigate(ArifMusicScreens.ArtistVerification.name) },
                            onSettingsClick = { navController.navigate(ArifMusicScreens.Settings.name) },
                            onHelpClick = { navController.navigate(ArifMusicScreens.HelpSupport.name) },
                            onAboutClick = { navController.navigate(ArifMusicScreens.About.name) },
                            onLogoutClick = {
                                authViewModel.logout()
                                navController.navigate(ArifMusicScreens.Welcome.name) {
                                    popUpTo(0) { inclusive = true }                                }
                            },
                            artistViewModel = artistViewModel,
                            authViewModel = authViewModel,
                            followViewModel = followViewModel,
                            currentUser = currentUser
                        )
                    }

                    UserType.LISTENER -> {
                        ListenerProfileScreen(
                            onBackClick = { navController.popBackStack() },
                            onEditProfileClick = { navController.navigate(ArifMusicScreens.EditProfile.name) },
                            onSettingsClick = { navController.navigate(ArifMusicScreens.Settings.name) },
                            onHelpClick = { navController.navigate(ArifMusicScreens.HelpSupport.name) },
                            onAboutClick = { navController.navigate(ArifMusicScreens.About.name) },
                            onLogoutClick = {
                                authViewModel.logout()
                                navController.navigate(ArifMusicScreens.Welcome.name) {
                                    popUpTo(0) { inclusive = true }                                }
                            },
                            authViewModel = authViewModel,
                            followViewModel = followViewModel,
                            currentUser = currentUser,
                        )
                    }

                    else -> {
                        LaunchedEffect(Unit) {
                            val session = sessionManager.getCurrentUser()

                            when {
                                !session.isLoggedIn && session.email.isBlank() -> {
                                    navController.navigate(ArifMusicScreens.Welcome.name) {
                                        popUpTo(0) { inclusive = true }                                    }
                                }

                                !session.isLoggedIn && session.email.isNotBlank() -> {
                                    navController.navigate(ArifMusicScreens.Welcome.name) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }

                                else -> {
                                    navController.navigate(ArifMusicScreens.Home.name) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            }
                        }

                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }

        composable(
            route = "${ArifMusicScreens.MusicList.name}/{listType}",
            arguments = listOf(navArgument("listType") { type = NavType.StringType })
        ) { entry ->
            val type = entry.arguments?.getString("listType") ?: "trending"
            val musicViewModel: MusicViewModel = hiltViewModel()

            MusicListScreen(
                listType = type,
                musicViewModel = musicViewModel,
                onBackClick = { navController.popBackStack() },
                onSearchClick = { navController.navigate(ArifMusicScreens.Search.name) },
                onMusicClick = { track ->
                    musicViewModel.loadMusic(track.id)
                    navController.navigate("${ArifMusicScreens.Player.name}/${track.id}")
                }
            )
        }

        composable(ArifMusicScreens.Settings.name) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(ArifMusicScreens.HelpSupport.name) {
            HelpSupportScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(ArifMusicScreens.About.name) {
            AboutScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(ArifMusicScreens.EditProfile.name) {
            EditProfileScreen(
                userViewModel = userViewModel,
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        composable(ArifMusicScreens.Favorite.name) {
            val musicViewModel: MusicViewModel = hiltViewModel()

            FavoriteScreen(
                musicViewModel = musicViewModel,
                onBackClick = { navController.popBackStack() },
                onMusicClick = { musicId ->
                    musicViewModel.loadMusic(musicId)
                    navController.navigate("${ArifMusicScreens.Player.name}/$musicId")
                },
                onRemoveFromFavoritesClick = { musicId ->
                    musicViewModel.toggleFavorite(musicId)
                }
            )
        }



        composable(ArifMusicScreens.ArtistDashboard.name) {
            ArtistDashboardScreen(
                onBackClick = { navController.popBackStack() },
                onEditTrackClick = { music ->
                    navController.navigate("${ArifMusicScreens.ArtistDashboard.name}/edit/${music.id}")
                },
                onDeleteTrackClick = { music ->
                    artistViewModel.deleteMusic(music.id)
                },
                artistViewModel = artistViewModel,
            )
        }

        composable(ArifMusicScreens.ArtistProfile.name) {
            ArtistProfileScreen(
                onBackClick = { navController.popBackStack() },
                onDashboardClick = { navController.navigate(ArifMusicScreens.ArtistDashboard.name) },
                onEditProfileClick = { navController.navigate("${ArifMusicScreens.ArtistProfile.name}/edit") },
                onGetVerifiedClick = { navController.navigate(ArifMusicScreens.ArtistVerification.name) },
                onSettingsClick = { navController.navigate(ArifMusicScreens.Settings.name) },
                onHelpClick = { navController.navigate(ArifMusicScreens.HelpSupport.name) },
                onAboutClick = { navController.navigate(ArifMusicScreens.About.name) },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(ArifMusicScreens.Welcome.name) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                artistViewModel = artistViewModel,
                authViewModel = authViewModel,
                followViewModel = followViewModel,
            )
        }

        composable(ArifMusicScreens.ArtistVerification.name) {
            ArtistVerificationScreen(
                onBackClick = { navController.popBackStack() },
                onSubmitClick = {
                    navController.popBackStack()
                }
            )
        }

        // Admin Panel
        composable(ArifMusicScreens.AdminPanel.name) {
            AdminPanelScreen(
                onBackClick = { navController.popBackStack() },
                onUserClick = { user ->
                    navController.navigate("${ArifMusicScreens.AdminProfile.name}/${user.id}")
                },
                onMusicClick = { musicId ->
                    navController.navigate("${ArifMusicScreens.Player.name}/$musicId")
                },
                onVerificationClick = { verificationId ->
                    navController.navigate("${ArifMusicScreens.AdminPanel.name}/verifications/$verificationId")
                },
                onFeaturedContentClick = {
                    navController.navigate(ArifMusicScreens.AdminFeaturedContent.name)
                },
                adminViewModel = adminViewModel
            )
        }

        composable(
            route = "${ArifMusicScreens.AdminProfile.name}/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            AdminProfileScreen(
                onAdminPanelClick = { navController.navigate(ArifMusicScreens.AdminPanel.name) },
                onManageFeaturedClick = { navController.navigate(ArifMusicScreens.AdminFeaturedContent.name) },
                onSettingsClick = { navController.navigate(ArifMusicScreens.Settings.name) },
                onHelpAndSupportClick = { navController.navigate(ArifMusicScreens.HelpSupport.name) },
                onAboutClick = { navController.navigate(ArifMusicScreens.About.name) },
                onLogoutClick = {
                    authViewModel.logout()
                    navController.navigate(ArifMusicScreens.Welcome.name) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onEditProfileClick = TODO(),
                currentUser = TODO(),
                authViewModel = TODO()
            )
        }


        composable(ArifMusicScreens.AdminFeaturedContent.name) {
            AdminFeaturedContentScreen(
                adminViewModel = adminViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}