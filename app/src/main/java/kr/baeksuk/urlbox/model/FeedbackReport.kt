package kr.baeksuk.urlbox.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class FeedbackReport(
    val id: String = "",
    val type: String = "",
    val title: String = "",
    val content: String = "",
    val email: String = "",
    val appVersion: String = "",
    val deviceInfo: String = "",
    val screenshotUrl: String = "",
    val status: String = "NEW",
    val createdAt: Long = 0L
)

