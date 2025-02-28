package kr.baeksuk.urlbox.viewmodel.editurl.settag

import android.app.Application
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.model.Tag

class SetTagViewModel(application: Application) : AndroidViewModel(application) {

    private val _repo = UrlRepository(application)
    private val tagBackup = _repo.getUserTagBackup()
    private val urlBackup = _repo.getUserUrlBackup()

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnShowTagsState = MutableLiveData<Boolean>()
    val btnShowTagsState = _btnShowTagsState

    private val _urlInputDoneState = MutableLiveData<Boolean>()
    val urlInputDoneState = _urlInputDoneState

    var isClicked = 0

    fun btnClose() {
        _btnCloseState.value = true
    }

    fun btnShowTags() {
        _btnShowTagsState.value = isClicked % 2 == 0
        isClicked++
    }

    fun getTagData(): LiveData<List<TagBackupEntity>> {
        return this.tagBackup
    }

    fun getCurrentTagsData(): LiveData<List<UrlBackupEntity>> {
        return this.urlBackup
    }

    fun insertUserTag(tag: String, urlTitle : String) {

        _repo.insertUserTag(tag, urlTitle)

    }

    fun deleteUserTag(tag: String, urlTitle : String) {

        _repo.deleteUserUrlTag(tag, urlTitle)

    }

    fun onUrlInputDone(actionId : Int) : Boolean{
        return if (actionId == EditorInfo.IME_ACTION_DONE){
            _urlInputDoneState.value = true
            true
        }else{
            _urlInputDoneState.value = false
            false
        }
    }

}