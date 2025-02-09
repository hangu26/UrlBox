package kr.baeksuk.urlbox.model

data class User(
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val profileImage: String? = null
)