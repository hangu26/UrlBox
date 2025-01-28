package kr.baeksuk.urlbox.viewmodel.urldetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import kr.baeksuk.urlbox.data.repository.UrlRepository

class UrlDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _repo = UrlRepository(application)

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnEditState = MutableLiveData<Boolean>()
    val btnEditState = _btnEditState

    private val _btnDelete = MutableLiveData<Boolean>()
    val btnDelete = _btnDelete

    private val _btnLoadUrl = MutableLiveData<Boolean>()
    val btnLoadUrl = _btnLoadUrl

    fun btnLoadUrl() {
        _btnLoadUrl.value = true
    }

    fun btnClose() {
        _btnCloseState.value = true
    }
    fun btnEdit() {
        _btnEditState.value = true
    }

    fun btnDelete() {
        _btnDelete.value = true
    }

    fun deleteGuestData(url : String){
        _repo.deleteGuestData(url)
    }

}