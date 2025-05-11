package com.arifmusic.app.ui.screens.listener

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.arifmusic.app.data.model.User
import com.arifmusic.app.ui.theme.Green
import com.arifmusic.app.ui.viewmodel.AuthViewModel
import com.arifmusic.app.ui.viewmodel.FollowViewModel
import com.arifmusic.app.utils.ImagePickerHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListenerProfileScreen(
    onBackClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLogoutClick: () -> Unit,
    authViewModel: AuthViewModel,
    followViewModel: FollowViewModel,
    currentUser: User? = null
) {
    val context = LocalContext.current
    val user = currentUser ?: authViewModel.currentUser.collectAsState().value

    val followersCount by followViewModel.getFollowersCount.collectAsState(initial = 0)
    val followingCount by followViewModel.getFollowingCount.collectAsState(initial = 0)

    // State for profile image
    var tempProfileImageUri by remember { mutableStateOf<Uri?>(null) }
    var isImageLoading by remember { mutableStateOf(false) }
    var localImagePath by remember { mutableStateOf<String?>(null) }

    // Use the modern photo picker with PickVisualMedia
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            // Show temporary image immediately
            tempProfileImageUri = it
            isImageLoading = true

            // Save image to internal storage
            ImagePickerHelper.saveImageToInternalStorageAsync(
                context = context,
                uri = it,
                fileName = "profile_listener_${System.currentTimeMillis()}.jpg"
            ) { path ->
                isImageLoading = false
                if (path != null) {
                    localImagePath = path
                    authViewModel.updateProfileImage(context, Uri.parse("file://$path"))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile setting", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black),
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
                .verticalScroll(rememberScrollState())
        ) {
            // Profile image and name
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.DarkGray.copy(alpha = 0.5f))
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .clickable {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {

                    when {
                        tempProfileImageUri != null -> {
                            AsyncImage(
                                model = tempProfileImageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        localImagePath != null -> {
                            AsyncImage(
                                model = File(localImagePath!!),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        user?.profileImageUrl != null -> {
                            // Handle both URI strings and file paths
                            val imageModel = if (user.profileImageUrl.startsWith("/") ||
                                user.profileImageUrl.startsWith("file:")) {
                                try {
                                    File(user.profileImageUrl.replace("file://", ""))
                                } catch (e: Exception) {
                                    user.profileImageUrl
                                }
                            } else {
                                user.profileImageUrl
                            }

                            AsyncImage(
                                model = imageModel,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    // Edit icon overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Green)
                            .clickable {
                                imagePickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Profile Picture",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Loading indicator
                    if (isImageLoading) {
                        CircularProgressIndicator(
                            color = Green,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = user?.fullName ?: "Listener's Name",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Followers and Following
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = followersCount.toString(),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Followers",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = followingCount.toString(),
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Following",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Menu items
            Spacer(modifier = Modifier.height(16.dp))

            ProfileMenuItem(
                icon = Icons.Default.Person,
                title = "Edit Profile",
                onClick = onEditProfileClick
            )

            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "Settings",
                onClick = onSettingsClick
            )

            ProfileMenuItem(
                icon = Icons.Default.Help,
                title = "Help & Support",
                onClick = onHelpClick
            )

            ProfileMenuItem(
                icon = Icons.Default.Info,
                title = "About",
                onClick = onAboutClick
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Logout",
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        Divider(
            color = Color.DarkGray.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 56.dp)
        )
    }
}