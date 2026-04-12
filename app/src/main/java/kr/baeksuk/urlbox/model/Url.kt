package kr.baeksuk.urlbox.model

data class Url(
    val url: String = "",
    val imageKey: String = "",
    val imgUri : String = "",
    val favorite : Boolean = false,
    val timeStamp : Long = 0,
    val urlName : String? = null,
    val urlMemo : String? = "",
    val tag : List<UserTags>? = null

)