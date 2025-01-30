package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class MyPageViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnEditState = MutableLiveData<Boolean>()
    val btnEditState = _btnEditState

    private val _btnSavedLinkState = MutableLiveData<Boolean>()
    val btnSavedLinkState = _btnSavedLinkState

    private val _btnFavoriteState = MutableLiveData<Boolean>()
    val btnFavoriteState = _btnFavoriteState

    fun btnEdit(){
        _btnEditState.value = true
    }

    fun btnSavedLink(){
        _btnSavedLinkState.value = true
    }

    fun btnFavorite(){
        _btnFavoriteState.value = true
    }

}