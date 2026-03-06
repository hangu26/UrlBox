package kr.baeksuk.urlbox.viewmodel.urldetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlbox.data.repository.UrlRepository

class UrlDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _repo = UrlRepository(application)

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnChangeImgState = MutableLiveData<Boolean>()
    val btnChangeImgState = _btnChangeImgState

    private val _btnDelete = MutableLiveData<Boolean>()
    val btnDelete = _btnDelete

    private val _btnLoadUrl = MutableLiveData<Boolean>()
    val btnLoadUrl = _btnLoadUrl

    private val _btnFavoriteState = MutableLiveData<Boolean>()
    val btnFavoriteState = _btnFavoriteState

    private val _btnEditState = MutableLiveData<Boolean>()
    val btnEditState = _btnEditState

    fun btnEdit() {
        _btnEditState.value = true
    }

    fun btnToLink(){
        _btnLoadUrl.value = true
    }

    fun btnLoadUrl() {
        _btnLoadUrl.value = true
    }

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun btnChangeImg() {
        _btnChangeImgState.value = true
    }

    fun btnDelete() {
        _btnDelete.value = true
    }

    fun deleteGuestData(url: String) {
        _repo.deleteGuestData(url)
    }

    fun deleteUserData(url: String, imageKey: String) {

        _repo.deleteUserData(url, imageKey)

    }

    fun btnFavorite() {
        _btnFavoriteState.value = true
    }

    fun updateUrlName(url: String, urlName :String){

        viewModelScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                _repo.updateUrlName(url, urlName)
            }

        }

    }

    fun updateUrlMemo(url: String, urlMemo :String){

        viewModelScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                _repo.updateUrlMemo(url, urlMemo)
            }

        }

    }

    fun updateFavorite(url: String, isFavorite: Boolean) {

        viewModelScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                _repo.updateFavorite(url, isFavorite)
            }

        }

    }

    fun updateUserFavorite(url: String, isFavorite: Boolean) {

        viewModelScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                _repo.updateUserFavorite(url, isFavorite)
            }

        }

    }

}