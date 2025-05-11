package com.arifmusic.app.data.repository

import com.arifmusic.app.data.local.FollowDao
import com.arifmusic.app.data.local.UserDao
import com.arifmusic.app.data.model.Follow
import com.arifmusic.app.data.model.User
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowRepository @Inject constructor(
    private val followDao: FollowDao,
    private val userDao: UserDao
) {
    suspend fun followArtist(userId: String, artistId: String) {
        val follow = Follow(
            followerId = userId,
            followingId = artistId,
            createdAt = Date()
        )
        followDao.follow(follow)
    }

    suspend fun unfollowArtist(userId: String, artistId: String) {
        val follow = Follow(
            followerId = userId,
            followingId = artistId,
            createdAt = Date() // This won't matter for deletion
        )
        followDao.unfollow(follow)
    }

    suspend fun isFollowing(userId: String, artistId: String): Boolean {
        return followDao.isFollowing(userId, artistId)
    }

    suspend fun getFollowersCount(artistId: String): Int {
        return followDao.getFollowersCount(artistId)
    }

    suspend fun getFollowingCount(userId: String): Int {
        return followDao.getFollowingCount(userId)
    }

    suspend fun getFollowers(artistId: String): List<User> {
        val follows = followDao.getFollowers(artistId)
        return follows.mapNotNull { follow ->
            userDao.getUserByEmail(follow.followerId)
        }
    }

    suspend fun getFollowing(userId: String): List<User> {
        val follows = followDao.getFollowing(userId)
        return follows.mapNotNull { follow ->
            userDao.getUserByEmail(follow.followingId)
        }
    }
}
