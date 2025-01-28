package kr.baeksuk.urlbox.viewmodel.urldetail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class UrlDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnCaptureState = MutableLiveData<Boolean>()
    val btnCaptureState = _btnCaptureState

    private val _btnCancel = MutableLiveData<Boolean>()
    val btnCancel = _btnCancel

    private val _btnLoadUrl = MutableLiveData<Boolean>()
    val btnLoadUrl = _btnLoadUrl

    fun btnLoadUrl() {
        _btnLoadUrl.value = true
    }

    fun btnClose() {
        _btnCloseState.value = true
    }
    fun btnCapture() {
        _btnCaptureState.value = true
    }

    fun btnCancel() {
        _btnCancel.value = true
    }


}