package kr.baeksuk.urlbox.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kr.baeksuk.urlbox.data.local.entity.UrlEntity

@Dao
interface UrlDao{

    @Insert
    suspend fun insert(urlEntity: UrlEntity)

    @Query("SELECT * FROM url_history WHERE urlLink = :url LIMIT 1")
    suspend fun getUrlIsExist(url: String): UrlEntity?

    @Query("SELECT * FROM url_history ORDER BY id DESC")
    fun getAll(): LiveData<List<UrlEntity>>

}