package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.data.repository.UserRepository
import kr.baeksuk.urlbox.model.Url

class MyPageViewModel(application: Application) : AndroidViewModel(application) {

    private val _repo = UserRepository(application)
    private val _urlRepo = UrlRepository(application)
    private val urlBackup = _urlRepo.getUserUrlBackup()

    private val _btnEditState = MutableLiveData<Boolean>()
    val btnEditState = _btnEditState

    private val _btnSavedLinkState = MutableLiveData<Boolean>()
    val btnSavedLinkState = _btnSavedLinkState

    private val _btnFavoriteState = MutableLiveData<Boolean>()
    val btnFavoriteState = _btnFavoriteState

    private val _btnOutState = MutableLiveData<Boolean>()
    val btnOutState = _btnOutState

    fun getUrlData(lifecycleOwner: LifecycleOwner) : LiveData<Pair<List<Url>, List<String>>>{
        val mutableUrl = MutableLiveData<Pair<List<Url>, List<String>>>()
        _repo.getUrlData().observe(lifecycleOwner) {
            mutableUrl.value = it
        }
        return mutableUrl
    }

    fun deleteUserBackup(){
        _urlRepo.deleteUserBackup()
    }

    fun getUrlBackup(): LiveData<List<UrlBackupEntity>> {
        return this.urlBackup
    }

    fun btnEdit(){
        _btnEditState.value = true
    }

    fun btnSavedLink(){
        _btnSavedLinkState.value = true
    }

    fun btnFavorite(){
        _btnFavoriteState.value = true
    }

    fun btnOut(){
        _btnOutState.value= true
    }

}