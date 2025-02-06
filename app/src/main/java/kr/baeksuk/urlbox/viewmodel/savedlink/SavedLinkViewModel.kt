package kr.baeksuk.urlbox.viewmodel.savedlink

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository

class SavedLinkViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UrlRepository(application)
    private val url = repo.getGuestUrl()
    private val urlBackup = repo.getUserUrlBackup()

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun getGuestUrl() : LiveData<List<UrlEntity>> {
        return this.url
    }

    fun getUrlBackup(): LiveData<List<UrlBackupEntity>> {
        return this.urlBackup
    }

}