package kr.baeksuk.urlbox.viewmodel.addlink.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class CaptureViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnCaptureState = MutableLiveData<Boolean>()
    val btnCaptureState = _btnCaptureState

    private val _btnSaveState = MutableLiveData<Boolean>()
    val btnSaveState = _btnSaveState

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun btnCapture(){
        _btnCaptureState.value = true
    }

    fun btnSave(){
        _btnSaveState.value = true
    }

}