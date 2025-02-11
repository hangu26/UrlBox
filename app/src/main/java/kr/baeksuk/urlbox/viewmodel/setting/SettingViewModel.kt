package kr.baeksuk.urlbox.viewmodel.setting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class SettingViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnLanguageState = MutableLiveData<Boolean>()
    val btnLanguageState = _btnLanguageState

    fun btnLanguage(){
        _btnLanguageState.value = true
    }

    fun btnClose(){
        _btnCloseState.value = true
    }

}