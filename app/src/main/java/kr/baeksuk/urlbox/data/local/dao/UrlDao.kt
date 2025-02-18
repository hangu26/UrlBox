package kr.baeksuk.urlbox.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity

@Dao
interface UrlDao{

    @Insert
    suspend fun insert(urlEntity: UrlEntity)

    @Insert
    suspend fun insertBackup(urlBackupEntity: UrlBackupEntity)

    @Insert
    suspend fun insertUrlBackup(urlBackupEntity: List<UrlBackupEntity>)

    @Query("UPDATE url_backup_history SET imgUri = :newUri WHERE urlLink = :url")
    suspend fun insertImgUri(newUri : String, url: String)

    @Query("SELECT * FROM url_backup_history ORDER BY id DESC")
    fun getAllBackup(): LiveData<List<UrlBackupEntity>>

    @Query("UPDATE url_history SET imageKey = :newImageKey WHERE urlLink = :url")
    suspend fun update(url:String, newImageKey : String)

    @Query("UPDATE url_backup_history SET urlName = :newUrlName, urlMemo = :newUrlMemo WHERE urlLink = :url")
    suspend fun updateUrlInfo(url: String, newUrlName: String, newUrlMemo :String)

    @Query("UPDATE url_backup_history SET imageKey = :newImageKey WHERE urlLink = :url")
    suspend fun updateBackup(url:String, newImageKey : String)

    @Query("UPDATE url_history SET favorite = :newFavorite WHERE urlLink = :url")
    suspend fun updateFavorite(url:String, newFavorite : Boolean)

    @Query("UPDATE url_backup_history SET favorite = :newFavorite WHERE urlLink = :url")
    suspend fun updateUserFavorite(url:String, newFavorite : Boolean)

    @Query("SELECT * FROM url_history WHERE urlLink = :url LIMIT 1")
    suspend fun getUrlIsExist(url: String): UrlEntity?

    @Query("SELECT * FROM url_backup_history WHERE urlLink = :url LIMIT 1")
    suspend fun getBackupUrlIsExist(url: String): UrlBackupEntity?

    @Query("SELECT * FROM url_backup_history WHERE urlLink IN (:urls)")
    suspend fun getUrlBackupIsExist(urls: List<String>): List<UrlBackupEntity>

    @Query("SELECT * FROM url_history ORDER BY id DESC")
    fun getAll(): LiveData<List<UrlEntity>>

    @Query("DELETE FROM url_history WHERE urlLink = :url")
    suspend fun deleteUrl(url: String)

    @Query("DELETE FROM url_backup_history WHERE urlLink = :url")
    suspend fun deleteUserUrl(url: String)

    @Query("DELETE FROM url_backup_history")
    suspend fun deleteUserBackup()

    @Query("SELECT EXISTS (SELECT 1 FROM url_backup_history LIMIT 1)")
    suspend fun hasBackupData(): Boolean

}