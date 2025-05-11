package com.arifmusic.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arifmusic.app.R
import com.arifmusic.app.data.model.UserType
import com.arifmusic.app.ui.navigation.ArifMusicScreens
import com.arifmusic.app.ui.theme.Green
import kotlinx.coroutines.launch

data class BottomNavItem(
    val screen: ArifMusicScreens,
    val icon: Int,
    val label: String
)

@Composable
fun ArifMusicBottomNavBar(
    navController: NavController,
    userType: UserType,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(ArifMusicScreens.Home, R.drawable.ic_home, "Home"),
        BottomNavItem(ArifMusicScreens.Search, R.drawable.ic_search, "Search"),
        BottomNavItem(ArifMusicScreens.Library, R.drawable.ic_library, "Library"),
        BottomNavItem(ArifMusicScreens.Profile, R.drawable.ic_profile, "Profile")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Surface(
        modifier = modifier,
        color = Color(0xFF2C2C2E),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Box {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                items.forEach { item ->
                    val selected = currentRoute?.startsWith(item.screen.name) == true

                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = item.icon),
                                contentDescription = item.label,
                                modifier = Modifier.height(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 12.sp
                            )
                        },
                        selected = selected,
                        onClick = {
                            val isGuest = userType == UserType.GUEST
                            val isRestricted = item.screen == ArifMusicScreens.Library || item.screen == ArifMusicScreens.Profile

                            if (!selected) {
                                if (isGuest && isRestricted) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "🔴 You must register to access ${item.label}",
                                            withDismissAction = true
                                        )
                                    }
                                    navController.navigate(ArifMusicScreens.Register.name) {
                                        popUpTo(ArifMusicScreens.Home.name) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else {
                                    navController.navigate(item.screen.name) {
                                        popUpTo(ArifMusicScreens.Home.name) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Green,
                            selectedTextColor = Green,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter),
            )
        }
    }
}


