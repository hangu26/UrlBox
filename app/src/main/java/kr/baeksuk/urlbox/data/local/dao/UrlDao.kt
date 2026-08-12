package kr.baeksuk.urlbox.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kr.baeksuk.urlbox.data.local.entity.HiddenFolderSecurityEntity
import kr.baeksuk.urlbox.data.local.entity.PreparationTag
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.UserTags

@Dao
interface UrlDao{

    @Insert
    suspend fun insert(urlEntity: UrlEntity)

    @Insert
    suspend fun insertBackup(urlBackupEntity: UrlBackupEntity)

    @Query("DELETE FROM url_backup_history")
    suspend fun deleteAllUrlBackup()

    @Query("DELETE FROM tag_backup_history")
    suspend fun deleteAllTagBackup()

    @Insert
    suspend fun insertUrlBackup(urlBackupEntity: List<UrlBackupEntity>)

    @Query("UPDATE url_backup_history SET imgUri = :newUri WHERE urlLink = :url")
    suspend fun insertImgUri(newUri : String, url: String)

    @Query("SELECT * FROM url_backup_history ORDER BY id DESC")
    fun getAllBackupFlow(): Flow<List<UrlBackupEntity>>

    @Query("SELECT * FROM url_backup_history ORDER BY id DESC")
    fun getAllBackup(): LiveData<List<UrlBackupEntity>>

    @Query("SELECT * FROM tag_backup_history ORDER BY id DESC")
    fun getTagBackupFlow(): Flow<List<TagBackupEntity>>

    @Query("SELECT * FROM tag_backup_history ORDER BY id DESC")
    fun getTagBackup(): LiveData<List<TagBackupEntity>>

    @Query("UPDATE url_history SET imageKey = :newImageKey WHERE urlLink = :url")
    suspend fun update(url:String, newImageKey : String)

    @Query("UPDATE url_backup_history SET urlName = :newUrlName, urlMemo = :newUrlMemo WHERE urlLink = :url")
    suspend fun updateUrlInfo(url: String, newUrlName: String, newUrlMemo :String)

    @Query("UPDATE url_history SET urlName = :newUrlName WHERE urlLink = :url")
    suspend fun updateGuestUrlName(url: String, newUrlName: String)

    @Query("UPDATE url_history SET urlMemo = :newUrlMemo WHERE urlLink = :url")
    suspend fun updateGuestUrlMemo(url: String, newUrlMemo :String)

    @Query("UPDATE url_history SET urlLink = :newUrl WHERE urlLink = :oldUrl")
    suspend fun updateGuestUrlLink(oldUrl: String, newUrl: String)

    @Query("UPDATE url_backup_history SET urlLink = :newUrl WHERE urlLink = :oldUrl")
    suspend fun updateUrlLink(oldUrl: String, newUrl: String)

    @Query("UPDATE url_backup_history SET imageKey = :newImageKey WHERE urlLink = :url")
    suspend fun updateBackup(url:String, newImageKey : String)

    @Query("UPDATE url_backup_history SET urlName = :urlName WHERE urlLink = :url")
    suspend fun updateUrlName(url: String,urlName : String)

    @Query("UPDATE url_backup_history SET urlMemo = :urlMemo WHERE urlLink = :url")
    suspend fun updateUrlMemo(url: String,urlMemo : String)

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

    @Query("SELECT * FROM tag_backup_history WHERE tag In (:tag)")
    suspend fun getTagBackupIsExist(tag: List<String>): List<TagBackupEntity>

    @Query("SELECT * FROM tag_backup_history WHERE tag IN (:tags)")
    suspend fun getTagBackupByTags(tags: List<String>): List<TagBackupEntity>


    @Insert(onConflict = OnConflictStrategy.IGNORE) // 중복 저장 방지
    suspend fun insertTagBackup(tagBackupEntities: List<TagBackupEntity>) // 리스트 저장 지원

    @Query("SELECT * FROM url_history ORDER BY id DESC")
    fun getAllFlow(): Flow<List<UrlEntity>>

    @Query("SELECT * FROM url_history ORDER BY id DESC")
    fun getAll(): LiveData<List<UrlEntity>>

    @Query("DELETE FROM url_history WHERE urlLink = :url")
    suspend fun deleteUrl(url: String)

    @Query("DELETE FROM url_backup_history WHERE urlLink = :url")
    suspend fun deleteUserUrl(url: String)

    @Query("DELETE FROM tag_backup_history WHERE tag = :tag")
    suspend fun deleteTag(tag: String)

    @Query("DELETE FROM url_backup_history")
    suspend fun deleteUserBackup()

    @Query("DELETE FROM tag_backup_history")
    suspend fun deleteUserTagBackup()

    @Query("SELECT EXISTS (SELECT 1 FROM url_backup_history LIMIT 1)")
    suspend fun hasBackupData(): Boolean

    @Query("SELECT * FROM url_backup_history WHERE urlLink = :urlTitle LIMIT 1")
    suspend fun getUrlBackupByTitle(urlTitle: String): UrlBackupEntity?

    @Query("UPDATE url_backup_history SET tag = :tags WHERE urlLink = :urlTitle")
    suspend fun updateUserTags(tags: List<UserTags>, urlTitle: String)

    @Query("SELECT * FROM tag_backup_history WHERE tag = :tag LIMIT 1")
    suspend fun getTagBackupByTitle(tag: String): TagBackupEntity?

    @Query("SELECT * FROM tag_backup_history")
    suspend fun getAllTagBackups(): List<TagBackupEntity>

    @Query("UPDATE tag_backup_history SET urlList = :urls WHERE tag = :tag")
    suspend fun updateUserUrlInTags(urls: String, tag: String)

    @Update
    suspend fun updateUrlInTags(tagBackupEntities: List<TagBackupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreparationTag(prepTag: PreparationTag)

    // 기존 전체 삭제
    @Query("DELETE FROM tag_prepare_history")
    suspend fun clearPreparationTags()

    // 특정 태그만 삭제
    @Query("DELETE FROM tag_prepare_history WHERE tag = :tagName")
    suspend fun deletePreparationTag(tagName: String)

    @Query("SELECT * FROM tag_prepare_history")
    fun getPreparationTagsFlow(): Flow<List<PreparationTag>>

    @Query("SELECT * FROM tag_prepare_history")
    fun getPreparationTags(): LiveData<List<PreparationTag>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHiddenFolderSecurity(entity: HiddenFolderSecurityEntity)

    @Query("SELECT * FROM hidden_folder_security WHERE userId = :userId LIMIT 1")
    suspend fun getHiddenFolderSecurity(userId: String): HiddenFolderSecurityEntity?

    @Query("DELETE FROM hidden_folder_security WHERE userId = :userId")
    suspend fun deleteHiddenFolderSecurity(userId: String)

    @Query("DELETE FROM hidden_folder_security")
    suspend fun deleteAllHiddenFolderSecurity()

    // Hidden URL methods
    @Query("UPDATE url_history SET hidden = :hidden WHERE urlLink = :url")
    suspend fun updateUrlHidden(url: String, hidden: Boolean)

    @Query("UPDATE url_backup_history SET hidden = :hidden WHERE urlLink = :url")
    suspend fun updateUrlBackupHidden(url: String, hidden: Boolean)

    @Query("SELECT * FROM url_history WHERE hidden = 1 ORDER BY id DESC")
    fun getHiddenUrls(): LiveData<List<UrlEntity>>

    @Query("SELECT * FROM url_backup_history WHERE hidden = 1 ORDER BY id DESC")
    fun getHiddenUrlBackups(): LiveData<List<UrlBackupEntity>>

    @Query("DELETE FROM url_backup_history WHERE hidden = 1")
    suspend fun deleteAllHiddenUrlBackups()

    @Query("DELETE FROM url_history WHERE hidden = 1")
    suspend fun deleteAllHiddenUrls()
}
