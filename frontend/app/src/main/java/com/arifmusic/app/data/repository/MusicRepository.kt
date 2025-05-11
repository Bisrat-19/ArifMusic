package com.arifmusic.app.data.repository
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.arifmusic.app.data.local.MusicDao
import com.arifmusic.app.data.local.MusicPlayedDao
import com.arifmusic.app.data.model.Music
import com.arifmusic.app.data.model.MusicApprovalStatus
import com.arifmusic.app.data.model.MusicPlayed
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepository @Inject constructor(
    private val musicDao: MusicDao,
    private val musicPlayedDao: MusicPlayedDao,

    private val sessionManager: SessionManager,

    @ApplicationContext private val context: Context
) {
    fun getAllMusic(): Flow<List<Music>> {
        return musicDao.getAllMusic()
    }
    suspend fun incrementPlayCountIfFirstTime(musicId: String) {
        val userId = sessionManager.getCurrentUserId()
        val hasPlayed = musicPlayedDao.hasUserPlayedMusic(userId, musicId) > 0
        if (!hasPlayed) {
            musicDao.incrementPlayCount(musicId)
            musicPlayedDao.insertMusicPlayed(MusicPlayed(userId = userId, musicId = musicId))
        }
    }


    suspend fun getFavoriteMusic(): Flow<List<Music>> {
        val userId = sessionManager.getCurrentUserId()
        return musicDao.getFavoriteMusic(userId)
    }
        suspend fun toggleFavorite(musicId: String, isFavorite: Boolean) {
            musicDao.updateFavoriteStatus(musicId, isFavorite)
        }

        suspend fun getMusicById(musicId: String): Music? {
            return musicDao.getMusicById(musicId)
        }

        fun getArtistMusic(artistId: String): Flow<List<Music>> {
            return musicDao.getArtistMusic(artistId)
        }


        suspend fun incrementPlayCount(musicId: String) {
            musicDao.incrementPlayCount(musicId)
        }

        fun getNewReleases(): Flow<List<Music>> {
            return musicDao.getNewReleases()
        }

        fun getTrendingMusic(): Flow<List<Music>> {
            return musicDao.getTrendingMusic()
        }


        suspend fun insertMusic(music: Music) {
            musicDao.insertMusic(music)
            Log.d("UploadDebug", "Music inserted: ${music.title}")

        }


        suspend fun deleteMusic(musicId: String) {
            val music = musicDao.getMusicById(musicId)
            music?.let {
                musicDao.deleteMusic(it)
            }
        }


        fun getPendingApprovalMusic(): Flow<List<Music>> {
            return musicDao.getMusicByApprovalStatus(MusicApprovalStatus.PENDING)
        }

        suspend fun approveMusic(musicId: String) {
            val music = musicDao.getMusicById(musicId)
            music?.let {
                val updatedMusic = it.copy(approvalStatus = MusicApprovalStatus.APPROVED)
                musicDao.insertMusic(updatedMusic)
            }
        }

        suspend fun rejectMusic(musicId: String) {
            val music = musicDao.getMusicById(musicId)
            music?.let {
                val updatedMusic = it.copy(approvalStatus = MusicApprovalStatus.REJECTED)
                musicDao.insertMusic(updatedMusic)
            }
        }
    fun getAudioDuration(context: Context, audioUri: Uri?): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, audioUri)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        } finally {
            retriever.release()
        }
    }

}
