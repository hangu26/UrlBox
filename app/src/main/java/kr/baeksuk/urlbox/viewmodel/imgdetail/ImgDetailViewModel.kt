package kr.baeksuk.urlbox.viewmodel.imgdetail

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlbox.data.repository.UrlRepository

class ImgDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _repo = UrlRepository(application)

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnEditState = MutableLiveData<Boolean>()
    val btnEditState = _btnEditState

    private val _btnDelete = MutableLiveData<Boolean>()
    val btnDelete = _btnDelete

    private val _btnLoadUrl = MutableLiveData<Boolean>()
    val btnLoadUrl = _btnLoadUrl

    private val _btnFavoriteState = MutableLiveData<Boolean>()
    val btnFavoriteState = _btnFavoriteState

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

    fun deleteUserData(url : String, imageKey : String){

        _repo.deleteUserData(url, imageKey)

    }

    fun btnFavorite(){
        _btnFavoriteState.value = true
    }

    fun updateFavorite(url : String, isFavorite : Boolean){

        viewModelScope.launch(Dispatchers.IO) {

            withContext(Dispatchers.Main) {
                _repo.updateFavorite(url, isFavorite)
            }

        }

    }

}