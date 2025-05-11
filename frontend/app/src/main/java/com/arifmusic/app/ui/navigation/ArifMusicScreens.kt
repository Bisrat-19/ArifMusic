package com.arifmusic.app.ui.navigation

// Define all screens as enum values
enum class ArifMusicScreens {
    Welcome,
    Login,
    Register,
    ForgotPassword,
    Home,
    Search,
    Explore,
    Library,
    Player,
    ArtistDetail,
    Profile,
    Settings,
    HelpSupport,
    About,
    EditProfile,
    MusicList,
    Favorite,
    Playlist,
    PlaylistDetail,
    WatchlistDetail,
    Notification,
    ArtistDashboard,
    ArtistProfile,
    ArtistVerification,
    ArtistAnalytics,
    AdminPanel,
    AdminProfile,
    AdminFeaturedContent;

    override fun toString(): String {
        return name
    }
}
