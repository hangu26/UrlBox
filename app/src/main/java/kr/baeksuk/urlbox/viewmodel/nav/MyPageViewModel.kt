package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.data.repository.UserRepository
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.util.UserSessionManager

class MyPageViewModel(
    application: Application,
    private val sessionManager: UserSessionManager
) : AndroidViewModel(application) {

    private val _repo = UserRepository(application)
    private val _urlRepo = UrlRepository(application)
    private val urlBackup = _urlRepo.getUserUrlBackup()
    private val tagBackup = _urlRepo.getUserTagBackup()

    private val _btnEditState = MutableLiveData<Boolean>()
    val btnEditState = _btnEditState

    private val _btnSavedLinkState = MutableLiveData<Boolean>()
    val btnSavedLinkState = _btnSavedLinkState

    private val _btnFavoriteState = MutableLiveData<Boolean>()
    val btnFavoriteState = _btnFavoriteState

    private val _btnOutState = MutableLiveData<Boolean>()
    val btnOutState = _btnOutState

    private val _btnTagState = MutableLiveData<Boolean>()
    val btnTagState = _btnTagState

    fun deleteUserBackup() {
        _urlRepo.deleteUserBackup()
    }

    fun deleteUserTagBackup() {
        _urlRepo.deleteUserTagBackup()
    }

    fun getUrlBackup(): LiveData<List<UrlBackupEntity>> {
        return this.urlBackup
    }

    fun getUserTagBackup(): LiveData<List<TagBackupEntity>> {
        return this.tagBackup
    }

    fun btnEdit() {
        _btnEditState.value = true
    }

    fun btnSavedLink() {
        _btnSavedLinkState.value = true
    }

    fun btnTag() {
        _btnTagState.value = true
    }

    fun btnFavorite() {
        _btnFavoriteState.value = true
    }

    fun btnOut() {
        _btnOutState.value = true
    }

    suspend fun isLoggedIn() : Boolean {
        return sessionManager.userSession.first().autoLogin
    }

}