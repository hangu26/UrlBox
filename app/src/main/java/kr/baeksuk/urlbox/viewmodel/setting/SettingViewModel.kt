package kr.baeksuk.urlbox.viewmodel.setting

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.domain.feedback.CheckAdminAccessUseCase
import kr.baeksuk.urlbox.util.util.UserSessionManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

sealed class FeedbackDestination {
    object UserForm : FeedbackDestination()
    object AdminPanel : FeedbackDestination()
}

class SettingViewModel(
    application: Application,
    private val checkAdminAccessUseCase: CheckAdminAccessUseCase,
    private val sessionManager: UserSessionManager
) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _requirePinOnHiddenUse = MutableLiveData<Boolean>()
    val requirePinOnHiddenUse = _requirePinOnHiddenUse

    private val _persistShowHiddenOnExit = MutableLiveData<Boolean>()
    val persistShowHiddenOnExit = _persistShowHiddenOnExit

    init {
        viewModelScope.launch {
            sessionManager.requirePinOnHiddenUse.collect { value ->
                _requirePinOnHiddenUse.postValue(value)
            }
        }
        viewModelScope.launch {
            sessionManager.persistShowHiddenOnExit.collect { value ->
                _persistShowHiddenOnExit.postValue(value)
            }
        }
    }

    fun setRequirePinOnHiddenUse(value: Boolean) {
        viewModelScope.launch {
            sessionManager.setRequirePinOnHiddenUse(value)
            _requirePinOnHiddenUse.value = value
        }
    }

    fun setPersistShowHiddenOnExit(value: Boolean) {
        viewModelScope.launch {
            sessionManager.setPersistShowHiddenOnExit(value)
            _persistShowHiddenOnExit.value = value
        }
    }

    private val _btnLanguageState = MutableLiveData<Boolean>()
    val btnLanguageState = _btnLanguageState

    private val _btnPrivacyState = MutableLiveData<Boolean>()
    val btnPrivacyState = _btnPrivacyState

    private val _btnUseTermsState = MutableLiveData<Boolean>()
    val btnUseTermsState = _btnUseTermsState

    private val _btnReviewState = MutableLiveData<Boolean>()
    val btnReviewState = _btnReviewState

    private val _feedbackDestination = MutableLiveData<FeedbackDestination?>()
    val feedbackDestination = _feedbackDestination

    fun btnLanguage() {
        _btnLanguageState.value = true
    }

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun btnPrivacy() {
        _btnPrivacyState.value = true
    }

    fun btnUseTerms() {
        _btnUseTermsState.value = true
    }

    fun btnReview() {
        _btnReviewState.value = true
    }

    fun btnFeedback() {
        viewModelScope.launch {
            _feedbackDestination.value = if (checkAdminAccessUseCase()) {
                FeedbackDestination.AdminPanel
            } else {
                FeedbackDestination.UserForm
            }
        }
    }

    fun clearFeedbackDestination() {
        _feedbackDestination.value = null
    }

}