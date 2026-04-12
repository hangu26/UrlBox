package kr.baeksuk.urlbox.viewmodel.login

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.GoogleLoginRepository
import kr.baeksuk.urlbox.data.repository.KakaoLoginRepository
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.data.repository.UserRepository
import kr.baeksuk.urlbox.domain.LoginRequest
import kr.baeksuk.urlbox.domain.LoginResult
import kr.baeksuk.urlbox.domain.LoginUseCase
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.UrlToLogin
import kr.baeksuk.urlbox.model.User
import java.io.File

class LoginViewModel(application: Application,private val loginUseCase: LoginUseCase) : AndroidViewModel(application) {
    private val _repo = UserRepository(application)
    private val _kakaoRepo = KakaoLoginRepository(application)
    private val _googleRepo = GoogleLoginRepository(application)

    private val _urlRepo = UrlRepository(application)
    private val url = _urlRepo.getGuestUrl()

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnGoogleState = MutableLiveData<Boolean>()
    val btnGoogleState = _btnGoogleState

    private val _btnKakaoState = MutableLiveData<Boolean>()
    val btnKakaoState = _btnKakaoState

    private val _insertComplete = MutableLiveData<Boolean>()
    val insertComplete: LiveData<Boolean> = _insertComplete

    private val _loadingBar = MutableLiveData<Boolean>()
    val loadingBar: LiveData<Boolean> = _loadingBar

    private val _isDataSyncEnabled = MutableLiveData(false)
    val isDataSyncEnabled = _isDataSyncEnabled

    private val _kakaoLoginState = MutableLiveData<Boolean>()
    val kakaoLoginState: LiveData<Boolean> get() = _kakaoLoginState

    private val _googleLoginState = MutableLiveData<Boolean>()
    val googleLoginState: LiveData<Boolean> get() = _googleLoginState

    private val _userData = MutableLiveData<User>()
    val userData: LiveData<User> get() = _userData

    private val _btnGuestState = MutableLiveData<Boolean>()
    val btnGuestState: LiveData<Boolean> = _btnGuestState

    private val _loginSelectLoading = MutableLiveData<Boolean>()
    val loginSelectLoading: LiveData<Boolean> = _loginSelectLoading

    private val _loginError = MutableLiveData<String>()
    val loginError: LiveData<String> = _loginError

    fun setKakaoUserData(user: User) {
        _userData.value = user
    }

    fun setLoadingBar(isLoad: Boolean) {
        _loadingBar.value = isLoad
    }

    fun kakaoLogin(context: Context) {
        _loadingBar.value = true // 버튼 클릭 시 기존 로딩바
        _kakaoRepo.kakaoLogin(context, this) { success ->
            changeLoadingBar()
            _kakaoLoginState.postValue(success)
        }
    }

    fun changeLoadingBar() {
        _loadingBar.value = false
        _loginSelectLoading.value = true // 계정 선택 후 로티
    }


    fun btnGuestContinue() {
        _btnGuestState.value = true
    }

    fun getGuestUrl(): LiveData<List<UrlEntity>> {
        return this.url
    }

    fun onCheckChanged(isChecked: Boolean) {
        _isDataSyncEnabled.value = isChecked
    }

    fun submitLogin(
        user: User,
        isUpload: Boolean,
        url: List<UrlToLogin>,
        imgFileList: List<File>
    ) {
        viewModelScope.launch {
            _loadingBar.value = true

            when (
                val result = loginUseCase(
                    LoginRequest(
                        user = user,
                        isUpload = isUpload,
                        urlList = url,
                        imgFileList = imgFileList,
                        autoLogin = true
                    )
                )
            ) {
                is LoginResult.Success -> {
                    _insertComplete.value = true
                    _loadingBar.value = false
                }

                is LoginResult.Failure -> {
                    Log.e("LoginViewModel", "login 실패", result.throwable)
                    _loginError.value = result.throwable.message ?: "로그인 처리 실패"
                    _loadingBar.value = false

                }
            }
        }
    }

        fun btnClose() {
            _btnCloseState.value = true
        }

        fun btnGoogleLogin() {
            _btnGoogleState.value = true
        }

        fun btnKakaoLogin() {
            _btnKakaoState.value = true
        }

    }
