package com.arifmusic.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arifmusic.app.data.model.FeaturedContent
import com.arifmusic.app.data.model.FeaturedType
import com.arifmusic.app.data.model.Music
import com.arifmusic.app.data.model.User
import com.arifmusic.app.data.repository.FeaturedContentRepository
import com.arifmusic.app.data.repository.MusicRepository
import com.arifmusic.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val featuredContentRepository: FeaturedContentRepository,
    private val musicRepository: MusicRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _featuredBanner = MutableStateFlow<FeaturedContent?>(null)
    val featuredBanner: StateFlow<FeaturedContent?> = _featuredBanner
    private val _featuredMusic = MutableStateFlow<Music?>(null)
    val featuredMusic: StateFlow<Music?> = _featuredMusic


    private val _trendingMusic = MutableStateFlow<List<Music>>(emptyList())
    val trendingMusic: StateFlow<List<Music>> = _trendingMusic

    private val _featuredArtists = MutableStateFlow<List<User>>(emptyList())
    val featuredArtists: StateFlow<List<User>> = _featuredArtists

    private val _newReleases = MutableStateFlow<List<Music>>(emptyList())
    val newReleases: StateFlow<List<Music>> = _newReleases

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadFeaturedBanner()
        loadTrendingMusic()
        loadFeaturedArtists()
        loadNewReleases()
    }

    private fun loadFeaturedBanner() {
        viewModelScope.launch {
            try {
                val banner = featuredContentRepository.getLatestFeaturedBanner(FeaturedType.BANNER)
                _featuredBanner.value = banner

                val musicId = banner?.contentId
                if (!musicId.isNullOrEmpty()) {
                    val music = musicRepository.getMusicById(musicId)
                    _featuredMusic.value = music
                } else {
                    _featuredMusic.value = null
                    println("Warning: Featured banner contentId is null or empty.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println("Error loading featured banner or music: ${e.localizedMessage}")
                _featuredBanner.value = null
                _featuredMusic.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadTrendingMusic() {
        viewModelScope.launch {
            musicRepository.getTrendingMusic().collectLatest { music ->
                _trendingMusic.value = music
            }
        }
    }

    private fun loadFeaturedArtists() {
        viewModelScope.launch {
            try {
                featuredContentRepository.getFeaturedContentByType(FeaturedType.ARTIST).collectLatest { contents ->
                    val artists = mutableListOf<User>()

                    contents.forEach { content ->
                        val contentId = content.contentId
                        if (!contentId.isNullOrEmpty()) {
                            val user = userRepository.getUserById(contentId)
                            if (user != null) {
                                artists.add(user)
                            }
                        }
                    }

                    _featuredArtists.value = artists
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false // Stop the loading spinner in case of error
            }
        }
    }

    private fun loadNewReleases() {
        viewModelScope.launch {
            musicRepository.getNewReleases().collectLatest { releases ->
                _newReleases.value = releases
            }
        }
    }

    fun refresh() {
        _isLoading.value = true
        loadFeaturedBanner()
        loadTrendingMusic()
        loadFeaturedArtists()
        loadNewReleases()
    }
}