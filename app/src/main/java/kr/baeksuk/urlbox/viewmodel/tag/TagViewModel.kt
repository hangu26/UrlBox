package kr.baeksuk.urlbox.viewmodel.tag

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.util.util.UserSessionManager

class TagViewModel(
    application: Application,
    private val sessionManager: UserSessionManager
) : AndroidViewModel(application) {

    private val _urlRepo = UrlRepository(application)

    private val tagBackup = _urlRepo.getUserTagBackup()

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    fun getUserTagBackup(): LiveData<List<TagBackupEntity>> {
        return this.tagBackup
    }

    fun btnClose(){
        _btnCloseState.value = true
    }

    fun deleteTag(tag : String){
        viewModelScope.launch {
            val userId = sessionManager.userId.first().orEmpty()
            _urlRepo.deleteUserTag(tag, userId)
        }
    }

}