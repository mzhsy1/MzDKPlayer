package org.mz.mzdkplayer.data.model


import androidx.room3.Embedded
import androidx.room3.Relation
import org.mz.mzdkplayer.data.local.MediaCacheEntity
import org.mz.mzdkplayer.data.local.MediaHistoryEntity

data class HistoryWithMetadata(
    @Embedded val history: MediaHistoryEntity,

    // Room 3 起 @Relation 只认复数形式：parentColumn/entityColumn → parentColumns/entityColumns（列名数组）
    @Relation(
        parentColumns = ["mediaUri"],
        entityColumns = ["videoUri"]
    )
    val metadata: MediaCacheEntity? // 可能为空（如果没有刮削到信息）
)