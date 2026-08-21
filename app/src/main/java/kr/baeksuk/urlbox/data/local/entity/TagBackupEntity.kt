package kr.baeksuk.urlbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kr.baeksuk.urlbox.model.UrlInTag
import kr.baeksuk.urlbox.util.util.UrlListInTagConverter

@Entity(tableName = "tag_backup_history")
@TypeConverters(UrlListInTagConverter::class)
data class TagBackupEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val tag: String,
    val count : String? = null,
    val timeStamp : String? = null,
    val urlList : List<String>? = null,
    val firebaseTagId: String? = null,
    val tagOrder: List<String>? = null
)
