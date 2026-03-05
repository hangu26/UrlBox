package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.EditorInfo
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
import kr.baeksuk.urlbox.model.UserTags

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

    private val _isTagLoading = MutableLiveData<Boolean>()
    val isTagLoading = _isTagLoading

    private val _btnRefreshState = MutableLiveData<Boolean>()
    val btnRefreshState = _btnRefreshState

    private val _urlInputDoneState = MutableLiveData<Boolean>()
    val urlInputDoneState = _urlInputDoneState

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

            mutableUrl.value = it

            _isLoading.value = false

        }

        Handler(Looper.getMainLooper()).postDelayed({

            if (_isLoading.value == true) _isLoading.value = false

        }, 5000)

        return mutableUrl

    }

    fun getTagData(lifecycleOwner: LifecycleOwner): LiveData<List<Tag>> {
        val mutableTag = MutableLiveData<List<Tag>>()

        isTagLoading.value = true

        _userRepo.getTagData().observe(lifecycleOwner) {

            mutableTag.value = it.sortedByDescending { it.timeStamp }.distinct()
            isTagLoading.value = false

        }

        Handler(Looper.getMainLooper()).postDelayed({

            if (isTagLoading.value == true) isTagLoading.value = false

        }, 5000)

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

    fun refreshUrlBackup(urlBackupEntity: List<UrlBackupEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            urlDao.deleteAllUrlBackup()
            urlDao.insertUrlBackup(urlBackupEntity)
        }
    }

    fun refreshTagBackup(tagBackupEntity: List<TagBackupEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            urlDao.deleteAllTagBackup()
            urlDao.insertTagBackup(tagBackupEntity)
        }
    }

    fun insertTagBackup(tagBackupEntity: List<TagBackupEntity>) {
        viewModelScope.launch(Dispatchers.IO) {
            // 1️⃣ 저장하려는 태그 리스트 가져오기
            val urlTagList = tagBackupEntity.map { it.tag }

            // 2️⃣ 이미 존재하는 태그 가져오기 (DB에서 조회)
            val existingTags = urlDao.getTagBackupByTags(urlTagList).associateBy { it.tag }

            // 3️⃣ 새로운 태그 & 업데이트할 태그 분리
            val newTags = mutableListOf<TagBackupEntity>()
            val tagsToUpdate = mutableListOf<TagBackupEntity>()

            for (tagEntity in tagBackupEntity) {
                val existingTag = existingTags[tagEntity.tag]

                if (existingTag != null) {
                    // 🔥 기존 태그의 urlList를 받아온 데이터로 덮어쓰기
                    val updatedTagEntity = existingTag.copy(urlList = tagEntity.urlList)
                    tagsToUpdate.add(updatedTagEntity)
                } else {
                    // 🔥 없는 태그 → 새로 추가
                    newTags.add(tagEntity)
                }
            }

            // 4️⃣ 새로운 태그 삽입
            if (newTags.isNotEmpty()) {
                urlDao.insertTagBackup(newTags)
            }

            // 5️⃣ 기존 태그는 받아온 데이터로 덮어쓰기
            if (tagsToUpdate.isNotEmpty()) {
                urlDao.updateUrlInTags(tagsToUpdate)
            }
        }
    }

    /** EditText의 onEditorActionListener는 키보드 액션 이벤트를 처리할 때 반드시 Boolean 값을 반환해야 한다.  **/
    fun onUrlInputDone(actionId : Int) : Boolean{
        return if (actionId == EditorInfo.IME_ACTION_DONE){
            _urlInputDoneState.value = true
            true
        }else{
            _urlInputDoneState.value = false
            false
        }
    }

    fun hasBackupData(): LiveData<Boolean> = liveData {
        val result = _repo.hasBackupData() // suspend 함수 호출
        emit(result) // LiveData로 반환
    }
}



