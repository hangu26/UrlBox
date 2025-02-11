package kr.baeksuk.urlbox.viewmodel.language

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class LanguageViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnKoreanState = MutableLiveData<Boolean>()
    val btnKoreanState = _btnKoreanState

    private val _btnEnglishState = MutableLiveData<Boolean>()
    val btnEnglishState = _btnEnglishState

    private val _btnJapanese = MutableLiveData<Boolean>()
    val btnJapanese = _btnJapanese

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    fun btnClose(){
        _btnCloseState.value = true
    }

    fun btnKorean(){
        btnKoreanState.value = true
    }

    fun btnEnglish(){
        btnEnglishState.value = true
    }

    fun btnJapanese(){
        btnJapanese.value = true
    }


}