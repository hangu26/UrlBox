package kr.baeksuk.urlbox.util.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kr.baeksuk.urlbox.model.UrlInTag
import kr.baeksuk.urlbox.model.UserTags

class UrlListInTagConverter {

    // List<UrlInTag>를 String으로 변환
    @TypeConverter
    fun fromUserUrlList(urls: List<String>?): String? {
        return Gson().toJson(urls)
    }

    @TypeConverter
    fun toUserUrlsList(urlString: String?): List<String>? {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(urlString, listType)
    }
}