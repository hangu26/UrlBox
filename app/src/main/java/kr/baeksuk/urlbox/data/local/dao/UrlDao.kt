package kr.baeksuk.urlbox.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Url

@Dao
interface UrlDao{

    @Insert
    suspend fun insert(urlEntity: UrlEntity)

    @Query("UPDATE url_history SET imageKey = :newImageKey WHERE urlLink = :url")
    suspend fun update(url:String, newImageKey : String)

    @Query("UPDATE url_history SET favorite = :newFavorite WHERE urlLink = :url")
    suspend fun updateFavorite(url:String, newFavorite : Boolean)

    @Query("SELECT * FROM url_history WHERE urlLink = :url LIMIT 1")
    suspend fun getUrlIsExist(url: String): UrlEntity?

    @Query("SELECT * FROM url_history ORDER BY id DESC")
    fun getAll(): LiveData<List<UrlEntity>>

    @Query("DELETE FROM url_history WHERE urlLink = :url")
    suspend fun deleteKeyword(url: String)


}