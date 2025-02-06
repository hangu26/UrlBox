package kr.baeksuk.urlbox.viewmodel.login

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.data.repository.UserRepository
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.User
import java.io.File

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val _repo = UserRepository(application)

    private val _urlRepo = UrlRepository(application)
    private val url = _urlRepo.getGuestUrl()

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnGoogleState = MutableLiveData<Boolean>()
    val btnGoogleState = _btnGoogleState

    private val _insertComplete = MutableLiveData<Boolean>()
    val insertComplete: LiveData<Boolean> = _insertComplete

    private val _isDataSyncEnabled = MutableLiveData(false)
    val isDataSyncEnabled = _isDataSyncEnabled

    fun getGuestUrl() : LiveData<List<UrlEntity>>{
        return this.url
    }

    fun onCheckChanged(isChecked: Boolean) {
        _isDataSyncEnabled.value = isChecked
    }

    fun insertUserId(userId: User) {
        viewModelScope.launch {
            // Room DB 또는 Repository의 suspend 함수 실행
            _repo.insertUserId(userId)
            _insertComplete.postValue(true) // 완료되었음을 알림
        }
    }

    fun insertAllData(userId: User, url: List<Url>, imgFileList : List<File>){
        viewModelScope.launch {
            _repo.insertAllData(userId, url , imgFileList)
            _insertComplete.postValue(true) // 완료되었음을 알림
        }
    }

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun btnGoogleLogin() {
        _btnGoogleState.value = true
    }

    /** 구글 로그인 시, db에 아이디 저장 함수 **/
//    fun insertUserId(userId: User) {
//        _repo.insertUserId(userId)
//    }

}