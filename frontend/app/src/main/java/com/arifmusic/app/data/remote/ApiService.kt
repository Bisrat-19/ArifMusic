package com.arifmusic.app.data.remote

import com.arifmusic.app.data.model.Playlist
import com.arifmusic.app.data.model.User
import com.arifmusic.app.data.model.Watchlist
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // User endpoints
    @POST("api/users")
    suspend fun registerUser(@Body user: UserRegistrationRequest): Response<UserResponse>

    @POST("api/users/login")
    suspend fun loginUser(@Body loginRequest: LoginRequest): Response<UserResponse>

    @GET("api/users/profile")
    suspend fun getUserProfile(): Response<User>

    @PUT("api/users/profile")
    suspend fun updateUserProfile(@Body updateRequest: UserUpdateRequest): Response<User>

    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") userId: String): Response<User>

    @DELETE("api/users/{id}")
    suspend fun deleteUser(@Path("id") userId: String): Response<MessageResponse>

    @POST("api/users/{id}/follow")
    suspend fun followUser(@Path("id") userId: String): Response<MessageResponse>

    @DELETE("api/users/{id}/follow")
    suspend fun unfollowUser(@Path("id") userId: String): Response<MessageResponse>

    @GET("api/users/{id}/followers")
    suspend fun getUserFollowers(@Path("id") userId: String): Response<List<User>>

    @GET("api/users/{id}/following")
    suspend fun getUserFollowing(@Path("id") userId: String): Response<List<User>>

    // Playlist endpoints
    @POST("api/playlists")
    suspend fun createPlaylist(@Body playlist: PlaylistRequest): Response<Playlist>

    @GET("api/playlists")
    suspend fun getUserPlaylists(): Response<List<Playlist>>

    @GET("api/playlists/{id}")
    suspend fun getPlaylistById(@Path("id") playlistId: Long): Response<Playlist>

    @PUT("api/playlists/{id}")
    suspend fun updatePlaylist(
        @Path("id") playlistId: Long,
        @Body updateRequest: PlaylistUpdateRequest
    ): Response<Playlist>

    @DELETE("api/playlists/{id}")
    suspend fun deletePlaylist(@Path("id") playlistId: Long): Response<MessageResponse>

    @POST("api/playlists/{id}/songs")
    suspend fun addSongToPlaylist(
        @Path("id") playlistId: Long,
        @Body request: AddSongRequest
    ): Response<Playlist>

    @DELETE("api/playlists/{id}/songs/{musicId}")
    suspend fun removeSongFromPlaylist(
        @Path("id") playlistId: Long,
        @Path("musicId") musicId: String
    ): Response<Playlist>

    // Watchlist endpoints
    @POST("api/watchlists")
    suspend fun createWatchlist(@Body watchlist: WatchlistRequest): Response<Watchlist>

    @GET("api/watchlists")
    suspend fun getUserWatchlists(): Response<List<Watchlist>>

    @GET("api/watchlists/{id}")
    suspend fun getWatchlistById(@Path("id") watchlistId: Long): Response<Watchlist>

    @PUT("api/watchlists/{id}")
    suspend fun updateWatchlist(
        @Path("id") watchlistId: Long,
        @Body updateRequest: WatchlistUpdateRequest
    ): Response<Watchlist>

    @DELETE("api/watchlists/{id}")
    suspend fun deleteWatchlist(@Path("id") watchlistId: Long): Response<MessageResponse>

    @POST("api/watchlists/{id}/songs")
    suspend fun addSongToWatchlist(
        @Path("id") watchlistId: Long,
        @Body request: AddSongRequest
    ): Response<Watchlist>

    @DELETE("api/watchlists/{id}/songs/{musicId}")
    suspend fun removeSongFromWatchlist(
        @Path("id") watchlistId: Long,
        @Path("musicId") musicId: String
    ): Response<Watchlist>

    @GET("api/watchlists/check/{musicId}")
    suspend fun checkSongInWatchlists(@Path("musicId") musicId: String): Response<CheckWatchlistResponse>
}


data class UserRegistrationRequest(
    val id: String,
    val email: String,
    val password: String,
    val name: String,
    val fullName: String,
    val userType: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val fullName: String,
    val userType: String,
    val token: String
)

data class UserUpdateRequest(
    val fullName: String? = null,
    val bio: String? = null,
    val password: String? = null,
    val profileImageUrl: String? = null
)

data class PlaylistRequest(
    val id: Long,
    val name: String,
    val description: String,
    val coverArtUrl: String? = null,
    val isPublic: Boolean = true
)

data class PlaylistUpdateRequest(
    val name: String? = null,
    val description: String? = null,
    val coverArtUrl: String? = null,
    val isPublic: Boolean? = null
)

data class WatchlistRequest(
    val id: Long,
    val name: String,
    val description: String
)

data class WatchlistUpdateRequest(
    val name: String? = null,
    val description: String? = null
)

data class AddSongRequest(
    val musicId: String
)

data class MessageResponse(
    val message: String
)

data class CheckWatchlistResponse(
    val inWatchlist: Boolean,
    val watchlists: List<WatchlistInfo>
)

data class WatchlistInfo(
    val id: Long,
    val name: String
)
