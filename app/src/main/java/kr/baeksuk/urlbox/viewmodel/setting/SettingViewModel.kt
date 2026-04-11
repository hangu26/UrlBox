package kr.baeksuk.urlbox.viewmodel.setting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class SettingViewModel(application: Application) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnLanguageState = MutableLiveData<Boolean>()
    val btnLanguageState = _btnLanguageState

    private val _btnPrivacyState = MutableLiveData<Boolean>()
    val btnPrivacyState = _btnPrivacyState

    private val _btnUseTermsState = MutableLiveData<Boolean>()
    val btnUseTermsState = _btnUseTermsState

    private val _btnReviewState = MutableLiveData<Boolean>()
    val btnReviewState = _btnReviewState

    private val _btnFeedbackState = MutableLiveData<Boolean>()
    val btnFeedbackState = _btnFeedbackState

    fun btnLanguage(){
        _btnLanguageState.value = true
    }

    fun btnClose(){
        _btnCloseState.value = true
    }

    fun btnPrivacy(){
        _btnPrivacyState.value = true
    }

    fun btnUseTerms(){
        _btnUseTermsState.value = true
    }

    fun btnReview(){
        _btnReviewState.value = true
    }

    fun btnFeedback(){
        _btnFeedbackState.value = true
    }

}