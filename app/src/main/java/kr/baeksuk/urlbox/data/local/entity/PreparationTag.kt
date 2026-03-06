package kr.baeksuk.urlbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kr.baeksuk.urlbox.model.UserTags

@Entity(tableName = "tag_prepare_history")
data class PreparationTag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val tag: String,
    val timeStamp: Long,
)