package kr.baeksuk.urlbox.viewmodel.tag

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.model.Tag

class TagViewModel(application: Application) : AndroidViewModel(application) {

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
        _urlRepo.deleteUserTag(tag)
    }

}