package kr.baeksuk.urlbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "url_history") // 테이블
data class UrlEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0, // ID 필드 자동으로 생성
    val urlLink : String,
    val imageKey : String
)
