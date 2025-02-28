package kr.baeksuk.urlbox.viewmodel.useterms

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class UseTermsViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    fun btnClose(){
        _btnCloseState.value = true
    }

}