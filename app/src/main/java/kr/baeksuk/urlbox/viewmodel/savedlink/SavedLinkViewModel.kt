package kr.baeksuk.urlbox.viewmodel.savedlink

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.util.util.UserSessionManager

class SavedLinkViewModel(
    application: Application,
    private val sessionManager: UserSessionManager
) : AndroidViewModel(application) {

    private val repo = UrlRepository(application)
    private val url = repo.getGuestUrl()
    private val urlBackup = repo.getUserUrlBackup()

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    fun loadSessionState() {
        viewModelScope.launch {
            _isLoggedIn.value = sessionManager.userSession.first().autoLogin ?: false
        }
    }

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun getGuestUrl(): LiveData<List<UrlEntity>> {
        return this.url
    }

    fun getUrlBackup(): LiveData<List<UrlBackupEntity>> {
        return this.urlBackup
    }

}