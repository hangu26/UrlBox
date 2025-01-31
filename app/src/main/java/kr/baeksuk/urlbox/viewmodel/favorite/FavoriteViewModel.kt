package kr.baeksuk.urlbox.viewmodel.favorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {

    private val _repo = UrlRepository(application)
    private val url = _repo.getGuestUrl()

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    fun getGuestUrl() : LiveData<List<UrlEntity>> {
        return this.url
    }

    fun btnClose() {
        _btnCloseState.value = true
    }

}