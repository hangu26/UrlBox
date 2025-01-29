package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class MyPageViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnEditState = MutableLiveData<Boolean>()
    val btnEditState = _btnEditState

    fun btnEdit(){
        _btnEditState.value = true
    }

}