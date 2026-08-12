package kr.baeksuk.urlbox.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_folder_security")
data class HiddenFolderSecurityEntity(
    @PrimaryKey
    val userId: String,
    val password: String
)
