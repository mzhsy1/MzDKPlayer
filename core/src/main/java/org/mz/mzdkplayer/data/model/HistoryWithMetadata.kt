package org.mz.mzdkplayer.data.model


import androidx.room.Embedded
import androidx.room.Relation
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.local.MediaHistoryEntity

data class HistoryWithMetadata(
    @Embedded val history: MediaHistoryEntity,

    @Relation(
        parentColumn = "mediaUri",
        entityColumn = "videoUri"
    )
    val metadata: MediaCacheEntity? // 可能为空（如果没有刮削到信息）
)