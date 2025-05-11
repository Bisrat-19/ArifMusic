package com.arifmusic.app.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.arifmusic.app.data.model.Music
import com.arifmusic.app.data.model.Watchlist
import com.arifmusic.app.data.model.WatchlistMusicCrossRef

data class WatchlistWithMusic(
    @Embedded val watchlist: Watchlist,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(WatchlistMusicCrossRef::class)
    )
    val music: List<Music>
)
