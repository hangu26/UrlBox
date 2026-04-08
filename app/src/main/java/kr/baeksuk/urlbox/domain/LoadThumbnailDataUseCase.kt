package kr.baeksuk.urlbox.domain

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.util.util.UserSessionManager

class LoadThumbnailDataUseCase(
    private val sessionManager: UserSessionManager,
    private val repository: UrlRepository
) {

    suspend fun isLoggedIn(): Boolean {
        val session = sessionManager.userSession.first()
        return session.autoLogin ?: false
    }

    fun getGuestUrls() : LiveData<List<UrlEntity>> {
        return repository.getGuestUrl()
    }

    fun getLoginUrlBackup() : LiveData<List<UrlBackupEntity>> {
        return repository.getUserUrlBackup()
    }

    fun getLoginTagBackup() : LiveData<List<TagBackupEntity>> {
        return repository.getUserTagBackup()
    }

}