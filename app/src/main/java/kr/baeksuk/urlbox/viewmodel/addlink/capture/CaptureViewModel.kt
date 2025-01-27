package kr.baeksuk.urlbox.viewmodel.addlink.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    fun btnClose() {
        _btnCloseState.value = true
    }

}