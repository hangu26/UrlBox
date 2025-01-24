package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class UrlViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnAddState = MutableLiveData<Boolean>()
    val btnAddState = _btnAddState

    fun btnAdd(){
        _btnAddState.value = true
    }

}