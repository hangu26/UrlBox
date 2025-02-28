package kr.baeksuk.urlbox.util.util

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kr.baeksuk.urlbox.model.UserTags

class Converters {

    // List<UserTagsEntity>를 String으로 변환
    @TypeConverter
    fun fromUserTagsList(tags: List<UserTags>?): String? {
        return Gson().toJson(tags)
    }

    // String을 List<UserTagsEntity>로 변환
    @TypeConverter
    fun toUserTagsList(tagsString: String?): List<UserTags>? {
        val listType = object : TypeToken<List<UserTags>>() {}.type
        return Gson().fromJson(tagsString, listType)
    }
}
