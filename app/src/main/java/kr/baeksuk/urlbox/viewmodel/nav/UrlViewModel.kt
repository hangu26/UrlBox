package kr.baeksuk.urlbox.viewmodel.nav

import android.app.Application
import android.view.inputmethod.EditorInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.domain.LoadUserHomeDataUseCase
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.util.UserSessionManager

class UrlViewModel(
    application: Application,
    private val loadUserHomeDataUseCase: LoadUserHomeDataUseCase,
    private val _repo: UrlRepository,
    private val sessionManager: UserSessionManager
) : AndroidViewModel(application) {

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

    private val _startMode = MutableLiveData<StartMode>()
    val startMode: LiveData<StartMode> = _startMode

    private val _urlData = MediatorLiveData<Pair<List<Url>, List<String>>>()
    val urlData: LiveData<Pair<List<Url>, List<String>>> = _urlData

    private val _tagData = MediatorLiveData<List<Tag>>()
    val tagData: LiveData<List<Tag>> = _tagData

    enum class RemoteSyncMode {
        INSERT,
        REFRESH
    }


    fun btnAdd() {
        _btnAddState.value = true
    }

    fun btnRefresh() {
        _btnRefreshState.value = true
    }

    fun prepareStartMode(beforeActivity: String) {
        viewModelScope.launch {
            val session = sessionManager.userSession.first()

            if (session.autoLogin != true) {
                _startMode.value = StartMode.GUEST
                return@launch
            }

            _startMode.value = when (beforeActivity) {
                "Login_refresh" -> StartMode.LOGIN_REFRESH
                "CaptureSave" -> StartMode.LOGIN_ONLY
                "Delete" -> StartMode.LOGIN_ONLY
                else -> StartMode.LOGIN_ONLY
            }
        }
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

    fun loadUserData(mode: RemoteSyncMode) {
        viewModelScope.launch {
            _isLoading.value = true
            _isTagLoading.value = true

            val userId = loadUserHomeDataUseCase.getUserId()
            if (userId.isNullOrBlank()) {
                _isLoading.value = false
                _isTagLoading.value = false
                return@launch
            }

            var urlLoaded = false
            var tagLoaded = false

            fun finishSyncIfNeeded() {
                if (urlLoaded && tagLoaded) {
                    if (mode == RemoteSyncMode.INSERT || mode == RemoteSyncMode.REFRESH) {
                        viewModelScope.launch {
                            sessionManager.setHomeSyncDone()
                        }
                    }
                }
            }

            val urlSource = loadUserHomeDataUseCase.getUrlData(userId)
            _urlData.addSource(urlSource) { data ->
                syncUrlBackup(data, mode)
                _urlData.value = data
                _isLoading.value = false
                _urlData.removeSource(urlSource)

                urlLoaded = true
                finishSyncIfNeeded()
            }

            val tagSource = loadUserHomeDataUseCase.getTagData(userId)
            _tagData.addSource(tagSource) { data ->
                val sorted = data.sortedByDescending { it.timeStamp }.distinct()
                syncTagBackup(sorted, mode)
                _tagData.value = sorted
                _isTagLoading.value = false
                _tagData.removeSource(tagSource)

                tagLoaded = true
                finishSyncIfNeeded()
            }
        }
    }

    private fun syncUrlBackup(data: Pair<List<Url>, List<String>>, mode: RemoteSyncMode) {
        val urlDataList = data.first
        val imgUriList = data.second

        val urlBackupEntity = urlDataList.zip(imgUriList) { url, imgUri ->
            UrlBackupEntity(
                urlLink = url.url,
                imageKey = url.imageKey,
                imgUri = imgUri,
                favorite = url.favorite,
                timeStamp = url.timeStamp,
                urlName = url.urlName,
                urlMemo = url.urlMemo,
                tag = url.tag
            )
        }

        when (mode) {
            RemoteSyncMode.INSERT -> insertUrlBackup(urlBackupEntity)
            RemoteSyncMode.REFRESH -> refreshUrlBackup(urlBackupEntity)
        }
    }

    private fun syncTagBackup(tagList: List<Tag>, mode: RemoteSyncMode) {
        val tagBackupEntity = tagList.map {
            TagBackupEntity(
                tag = it.tag!!,
                timeStamp = it.timeStamp,
                urlList = it.urlList
            )
        }

        when (mode) {
            RemoteSyncMode.INSERT -> insertTagBackup(tagBackupEntity)
            RemoteSyncMode.REFRESH -> refreshTagBackup(tagBackupEntity)
        }
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
    fun onUrlInputDone(actionId: Int): Boolean {
        return if (actionId == EditorInfo.IME_ACTION_DONE) {
            _urlInputDoneState.value = true
            true
        } else {
            _urlInputDoneState.value = false
            false
        }
    }

}



