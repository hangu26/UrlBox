package kr.baeksuk.urlbox.viewmodel.editurl

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.util.util.UserSessionManager

class EditUrlViewModel(
    application: Application,
    private val sessionManager: UserSessionManager
) : AndroidViewModel(application) {

    private val _repo = UrlRepository(application)

    private val _btnChangeImgState = MutableLiveData<Boolean>()
    val btnChangeImgState = _btnChangeImgState

    private val _btnSaveChangesState = MutableLiveData<Boolean>()
    val btnSaveChangesState = _btnSaveChangesState

    private val _btnBackState = MutableLiveData<Boolean>()
    val btnBackState = _btnBackState

    private val _btnSetTagState = MutableLiveData<Boolean>()
    val btnSetTagState = _btnSetTagState

    fun btnSetTag(){
        _btnSetTagState.value = true
    }

    fun btnBack(){
        _btnBackState.value = true
    }

    fun btnChangeImg() {
        _btnChangeImgState.value = true
    }

    fun btnSaveChanges(){
        _btnSaveChangesState.value = true
    }

    fun updateUserUrl(url: String, urlName : String, urlMemo : String, context: Context) {

        viewModelScope.launch(Dispatchers.IO) {
            // 새로운 URL과 기존 URL이 다르고, 새로운 URL이 이미 존재하는지 확인
            if (url != urlName && _repo.hasBackupUrl(urlName)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "이미 존재하는 URL입니다.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                val userId = sessionManager.userId.first().orEmpty()
                _repo.updateUrlInfo(url, urlName, urlMemo, userId)
                Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
            }

        }
    }

    fun updateGuestUrl(url: String, urlName : String, urlMemo : String, context: Context){

        viewModelScope.launch(Dispatchers.IO) {
            // 새로운 URL과 기존 URL이 다르고, 새로운 URL이 이미 존재하는지 확인
            if (url != urlName && _repo.hasGuestUrl(urlName)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "이미 존재하는 URL입니다.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(context, "저장되었습니다.", Toast.LENGTH_SHORT).show()
            }

        }

    }

}