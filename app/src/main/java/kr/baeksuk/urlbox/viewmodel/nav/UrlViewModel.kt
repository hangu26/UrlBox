package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
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
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.data.repository.UserRepository
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.model.Url

class UrlViewModel(application: Application) : AndroidViewModel(application) {

    private val _userRepo = UserRepository(application)
    private val _repo = UrlRepository(application)
    private val url = _repo.getGuestUrl()
    private val urlBackup = _repo.getUserUrlBackup()
    private val tagBackup = _repo.getUserTagBackup()

    private val urlDatabase = UrlDatabase.getInstance(application)
    private val urlDao: UrlDao = urlDatabase.urlDao()

    private val _btnAddState = MutableLiveData<Boolean>()
    val btnAddState = _btnAddState

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading = _isLoading

    private val _btnRefreshState = MutableLiveData<Boolean>()
    val btnRefreshState = _btnRefreshState

    fun btnAdd() {
        _btnAddState.value = true
    }

    fun btnRefresh() {
        _btnRefreshState.value = true
    }

    fun getGuestUrl(): LiveData<List<UrlEntity>> {
        return this.url
    }

    fun getUserUrlBackup(): LiveData<List<UrlBackupEntity>> {
        return this.urlBackup
    }

    fun getUserTagBackup(): LiveData<List<TagBackupEntity>> {
        return this.tagBackup
    }

    fun getUrlData(lifecycleOwner: LifecycleOwner): LiveData<Pair<List<Url>, List<String>>> {
        val mutableUrl = MutableLiveData<Pair<List<Url>, List<String>>>()

        _isLoading.value = true

        _userRepo.getUrlData().observe(lifecycleOwner) {

            _isLoading.value = false

            mutableUrl.value = it

        }

        Handler(Looper.getMainLooper()).postDelayed({

            if (_isLoading.value == true) _isLoading.value = false

        }, 3000)

        return mutableUrl

    }

    fun getTagData(lifecycleOwner: LifecycleOwner): LiveData<List<Tag>> {
        val mutableTag = MutableLiveData<List<Tag>>()
        _userRepo.getTagData().observe(lifecycleOwner) {
            mutableTag.value = it.sortedByDescending { it.timeStamp }
        }

        return mutableTag
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

    fun insertTagBackup(tagBackupEntity: List<TagBackupEntity>) {
        viewModelScope.launch(Dispatchers.IO) {

            val urlTag = tagBackupEntity.map { it.tag }

            // tag가 이미 존재하는지 확인
            val existingTags = urlDao.getTagBackupIsExist(urlTag)

            val newTags = tagBackupEntity.filter { tagEntity ->
                !existingTags.any{ it.tag == tagEntity.tag }
            }

            if (newTags.isNotEmpty()) {
                // 중복되지 않으면 저장
                urlDao.insertTagBackup(newTags)
            }
        }
    }


    fun hasBackupData(): LiveData<Boolean> = liveData {
        val result = _repo.hasBackupData() // suspend 함수 호출
        emit(result) // LiveData로 반환
    }
}



