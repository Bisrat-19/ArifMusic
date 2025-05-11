package com.arifmusic.app.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arifmusic.app.data.model.ArtistAnalytics
import com.arifmusic.app.data.model.Music
import com.arifmusic.app.data.model.MusicApprovalStatus
import com.arifmusic.app.data.model.User
import com.arifmusic.app.data.model.UserType
import com.arifmusic.app.data.repository.ArtistStatsRepository
import com.arifmusic.app.data.repository.MusicRepository
import com.arifmusic.app.data.repository.SessionManager
import com.arifmusic.app.data.repository.UserRepository
import com.arifmusic.app.utils.ImagePickerHelper
import com.arifmusic.app.utils.MediaAccessHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ArtistViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val musicRepository: MusicRepository,
    private val artistStatsRepository: ArtistStatsRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _artists = MutableStateFlow<List<User>>(emptyList())
    val artists: StateFlow<List<User>> = _artists.asStateFlow()

    private val _currentArtist = MutableStateFlow<User?>(null)
    val currentArtist: StateFlow<User?> = _currentArtist.asStateFlow()

    private val _artistMusic = MutableStateFlow<List<Music>>(emptyList())
    val artistMusic: StateFlow<List<Music>> = _artistMusic.asStateFlow()

    private val _artistAnalytics = MutableStateFlow<ArtistAnalytics?>(null)
    val artistAnalytics: StateFlow<ArtistAnalytics?> = _artistAnalytics.asStateFlow()

    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _operationStatus = MutableStateFlow<String?>(null)
    val operationStatus: StateFlow<String?> = _operationStatus.asStateFlow()

    private val _selectedTab = MutableStateFlow(DashboardTab.UPLOAD_MUSIC)
    val selectedTab: StateFlow<DashboardTab> = _selectedTab.asStateFlow()

    init {
        loadArtists()

        viewModelScope.launch {
            val session = sessionManager.sessionFlow.first()
            if (session.userType == UserType.ARTIST) {
                val artist = userRepository.getUserByEmail(session.email)
                artist?.let {
                    _currentArtist.value = it
                    loadArtistData(it.id)
                }
            }
        }
    }

    fun setSelectedTab(tab: DashboardTab) {
        _selectedTab.value = tab
    }

    private fun loadArtists() {
        viewModelScope.launch {
            userRepository.getArtists().collectLatest { artistList ->
                _artists.value = artistList
            }
        }
    }

    fun loadArtistData(artistId: String) {
        viewModelScope.launch {
            // Load artist music
            musicRepository.getArtistMusic(artistId).collectLatest { music ->
                _artistMusic.value = music
                Log.d("ArtistViewModel", "Loaded ${music.size} music tracks for artist $artistId")
            }
        }

        viewModelScope.launch {
            try {
                // Collect the flow and update _artistAnalytics
                artistStatsRepository.getArtistAnalytics(artistId).collectLatest { analytics ->
                    if (analytics != null) {
                        _artistAnalytics.value = analytics
                    } else {
                        val defaultAnalytics = ArtistAnalytics(
                            artistId = artistId,
                            totalPlays = 0,
                            monthlyListeners = 0,
                            playlistAdds = 0,
                            watchlistSaves = 0
                        )
                        artistStatsRepository.addArtistAnalytics(defaultAnalytics)
                        _artistAnalytics.value = defaultAnalytics
                    }
                }
            } catch (e: Exception) {
                Log.e("ArtistViewModel", "Error loading analytics: ${e.message}", e)
                _operationStatus.value = "Failed to load analytics: ${e.message}"
            }
        }
    }

    fun updateArtistApprovalStatus(artistId: String, approved: Boolean) {
        viewModelScope.launch {
            userRepository.updateArtistApprovalStatus(artistId, approved)
            loadArtists()
        }
    }




    fun uploadMusic(
        context: Context,
        title: String,
        genre: String?,
        description: String?,
        audioUri: Uri?,
        coverImageUri: Uri?
    ) {
        if (audioUri == null) {
            _operationStatus.value = "No audio file selected"
            return
        }

        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 0f

            try {
                val session = sessionManager.sessionFlow.first()
                val artist = userRepository.getUserByEmail(session.email) ?: throw Exception("Artist not found")
                val userId = sessionManager.getCurrentUserId()

                Log.d("MusicUpload", "Starting upload for artist: ${artist.id}, title: $title")

                val resolvedAudioUri = MediaAccessHelper.resolveUri(context, audioUri)
                val resolvedCoverUri = coverImageUri?.let { MediaAccessHelper.resolveUri(context, it) }


                if (!MediaAccessHelper.isUriAccessible(context, resolvedAudioUri)) {
                    throw Exception("Cannot access audio file. Please select a different file.")
                }

                resolvedCoverUri?.let {
                    if (!MediaAccessHelper.isUriAccessible(context, it)) {
                        throw Exception("Cannot access cover image. Please select a different image.")
                    }
                }

                for (i in 1..10) {
                    delay(300)
                    _uploadProgress.value = i / 10f
                }

                val musicId = UUID.randomUUID().toString()
                Log.d("MusicUpload", "Generated music ID: $musicId")
                var coverImagePath: String? = null
                if (resolvedCoverUri != null) {
                    coverImagePath = ImagePickerHelper.saveImageToInternalStorage(
                        context,
                        resolvedCoverUri,
                        "cover_${musicId}.jpg"
                    )

                    if (coverImagePath == null) {
                        Log.e("MusicUpload", "Failed to save cover image")
                        // Continue without the cover image
                    }
                }

                val audioDuration = musicRepository.getAudioDuration(context, resolvedAudioUri)
                val music = Music(
                    id = musicId,
                    title = title,
                    artist = artist.name,
                    artistId = artist.id,
                    playlistId = null,
                    album = "Single",
                    genre = genre,
                    description = description,
                    duration = audioDuration,
                    path = resolvedAudioUri.toString(), // Use the resolved URI
                    artworkUri = coverImagePath, // Use the saved local path instead of the URI
                    isFavorite = false,
                    playCount = 0,
                    uploadDate = System.currentTimeMillis(),
                    approvalStatus = MusicApprovalStatus.PENDING,
                    userId = userId,
                )

                Log.d("MusicUpload", "Created music object: $music")

                try {
                    musicRepository.insertMusic(music)
                    Log.d("MusicUpload", "Music inserted successfully")
                    _operationStatus.value = "Music uploaded successfully"

                    // Switch to My Music tab after successful upload
                    _selectedTab.value = DashboardTab.MY_MUSIC
                } catch (e: Exception) {
                    Log.e("MusicUpload", "Error inserting music: ${e.message}", e)
                    _operationStatus.value = "Failed to save music: ${e.message}"
                }

                _uploadProgress.value = null
                _isUploading.value = false

                // Refresh artist data to show the new music
                loadArtistData(artist.id)

            } catch (e: Exception) {
                Log.e("MusicUpload", "Error during music upload: ${e.message}", e)
                _uploadProgress.value = null
                _isUploading.value = false
                _operationStatus.value = "Upload failed: ${e.message}"
            }
        }
    }


    fun deleteMusic(musicId: String) {
        viewModelScope.launch {
            try {
                musicRepository.deleteMusic(musicId)
                _operationStatus.value = "Music deleted successfully"

                val session = sessionManager.sessionFlow.first()
                val artist = userRepository.getUserByEmail(session.email) ?: return@launch
                loadArtistData(artist.id)
            } catch (e: Exception) {
                _operationStatus.value = "Failed to delete music: ${e.message}"
            }
        }
    }

    fun clearOperationStatus() {
        _operationStatus.value = null
    }
}

enum class DashboardTab {
    UPLOAD_MUSIC, MY_MUSIC, ANALYTICS
}
