package kr.baeksuk.urlbox.viewmodel.favorite

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository

class FavoriteViewModel(application: Application) : AndroidViewModel(application) {

    private val _repo = UrlRepository(application)
    private val url = _repo.getGuestUrl()

    fun getGuestUrl() : LiveData<List<UrlEntity>> {
        return this.url
    }

}