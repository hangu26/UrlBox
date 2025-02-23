package kr.baeksuk.urlbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tag_backup_history")
data class TagBackupEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val tag: String,
    val count : String? = null,
    val timeStamp : String? = null
)
