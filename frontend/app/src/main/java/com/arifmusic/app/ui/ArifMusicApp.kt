package com.arifmusic.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.arifmusic.app.data.model.UserType
import com.arifmusic.app.data.repository.SessionManager
import com.arifmusic.app.ui.components.ArifMusicBottomNavBar
import com.arifmusic.app.ui.components.MiniPlayer
import com.arifmusic.app.ui.navigation.ArifMusicNavGraph
import com.arifmusic.app.ui.navigation.ArifMusicScreens
import com.arifmusic.app.ui.viewmodel.AuthViewModel
import com.arifmusic.app.ui.viewmodel.MusicViewModel

@Composable
fun ArifMusicApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel(),
    musicViewModel: MusicViewModel = hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentMusic by musicViewModel.currentMusic.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState(initial = false)
    val context = LocalContext.current

    val sessionManager = remember { SessionManager(context) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomBarRoutes = setOf(
        ArifMusicScreens.Home.name,
        ArifMusicScreens.Search.name,
        ArifMusicScreens.Library.name,
        ArifMusicScreens.Profile.name,
        ArifMusicScreens.Explore.name


    )

    val isPlayerScreen = currentRoute?.startsWith("${ArifMusicScreens.Player.name}/") == true

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                ArifMusicBottomNavBar(
                    navController,
                    currentUser?.userType ?: UserType.LISTENER
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = modifier.padding(innerPadding)) {
            ArifMusicNavGraph(
                navController = navController,
                startDestination = if (isLoggedIn) {
                    ArifMusicScreens.Home.name
                } else {
                    ArifMusicScreens.Welcome.name
                },
                modifier = Modifier,
                sessionManager = sessionManager,
            )

            if (!isPlayerScreen) {
                currentMusic?.let { music ->
                    MiniPlayer(
                        musicViewModel = musicViewModel,
                        onMiniPlayerClick = {
                            navController.navigate("${ArifMusicScreens.Player.name}/${music.id}")
                        },
                        modifier = Modifier
                    )
                }
            }
        }
    }
}
