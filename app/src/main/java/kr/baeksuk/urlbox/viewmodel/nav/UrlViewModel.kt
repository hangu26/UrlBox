package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import android.graphics.Bitmap
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.data.repository.UserRepository
import kr.baeksuk.urlbox.model.Url

class UrlViewModel(application: Application) : AndroidViewModel(application) {

    private val _userRepo = UserRepository(application)
    private val _repo = UrlRepository(application)
    private val url = _repo.getGuestUrl()
    private val urlBackup = _repo.getUserUrlBackup()

    private val urlDatabase = UrlDatabase.getInstance(application)
    private val urlDao: UrlDao = urlDatabase.urlDao()

    private val _btnAddState = MutableLiveData<Boolean>()
    val btnAddState = _btnAddState

    fun btnAdd() {
        _btnAddState.value = true
    }

    fun getGuestUrl(): LiveData<List<UrlEntity>> {
        return this.url
    }

    fun getUserUrlBackup(): LiveData<List<UrlBackupEntity>> {
        return this.urlBackup
    }

    fun getUrlData(lifecycleOwner: LifecycleOwner): LiveData<Pair<List<Url>, List<String>>> {
        val mutableUrl = MutableLiveData<Pair<List<Url>, List<String>>>()
        _userRepo.getUrlData().observe(lifecycleOwner) {
            mutableUrl.value = it
        }
        return mutableUrl
    }

    /** 데이터를 파이어베이스에서 받아오고 룸에 저장해서 매번 받아오지도 않게 만듦 **/
    fun insertUrlBackup(urlBackupEntity: List<UrlBackupEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            val urlLinks = urlBackupEntity.map { it.urlLink }

            val existingUrls = urlDao.getUrlBackupIsExist(urlLinks)

            val newUrls = urlBackupEntity.filter { urlEntity ->

                !existingUrls.any { it.urlLink == urlEntity.urlLink }
            }

            if (newUrls.isNotEmpty()) {
                urlDao.insertUrlBackup(newUrls)  // 타입 명시적으로 지정
            }
        }
    }

    fun hasBackupData(): LiveData<Boolean> = liveData {
        val result = _repo.hasBackupData() // suspend 함수 호출
        emit(result) // LiveData로 반환
    }
}



