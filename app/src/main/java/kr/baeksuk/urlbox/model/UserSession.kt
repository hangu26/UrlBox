package kr.baeksuk.urlbox.model

data class UserSession(
    val userId: String?,
    val userEmail: String?,
    val userName: String?,
    val userProfile: String?,
    val autoLogin: Boolean
)