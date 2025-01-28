package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository

class ThumbnailViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = UrlRepository(application)
    private val url = repo.getGuestUrl()

    private val _btnAddState = MutableLiveData<Boolean>()
    val btnAddState = _btnAddState

    fun btnAdd() {
        _btnAddState.value = true
    }

    fun getGuestThumbnail() : LiveData<List<UrlEntity>>{
        return this.url
    }

}