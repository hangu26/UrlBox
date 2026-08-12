package kr.baeksuk.urlbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kr.baeksuk.urlbox.model.UserTags
import kr.baeksuk.urlbox.util.util.Converters

@Entity(tableName = "url_backup_history") // 테이블
@TypeConverters(Converters::class)
data class UrlBackupEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0, // ID 필드 자동으로 생성
    val urlLink : String,
    val imageKey : String,
    val imgUri : String,
    val favorite : Boolean,
    val hidden : Boolean = false,
    val timeStamp : Long,
    val urlName : String? = null,
    val urlMemo : String? = null,
    val tag : List<UserTags>? = null
)