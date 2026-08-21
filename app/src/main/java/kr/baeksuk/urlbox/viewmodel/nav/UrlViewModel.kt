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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.local.UrlDatabase
import kr.baeksuk.urlbox.data.local.dao.UrlDao
import kr.baeksuk.urlbox.data.local.entity.HiddenFolderSecurityEntity
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

    private val _showHiddenUrls = MutableLiveData<Boolean>(false)
    val showHiddenUrls: LiveData<Boolean> = _showHiddenUrls

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
            var passwordLoaded = false

            fun finishSyncIfNeeded() {
                if (urlLoaded && tagLoaded && passwordLoaded) {
                    if (mode == RemoteSyncMode.INSERT || mode == RemoteSyncMode.REFRESH) {
                        viewModelScope.launch {
                            sessionManager.setHomeSyncDone()
                        }
                    }
                }
            }

            val urlSource = loadUserHomeDataUseCase.getUrlData(userId)
            _urlData.addSource(urlSource) { data ->
                // Perform sync on IO and ensure DB merge completes before updating UI to avoid race that blanks imgUri
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        syncUrlBackupSuspend(data, mode)
                    } catch (e: Exception) {
                        android.util.Log.e("UrlViewModel", "syncUrlBackupSuspend failed: ${e.message}")
                    }

                    withContext(Dispatchers.Main) {
                        _urlData.value = data
                        _isLoading.value = false
                        _urlData.removeSource(urlSource)

                        urlLoaded = true
                        finishSyncIfNeeded()
                    }
                }
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

            val passwordSource: LiveData<String?> =
                loadUserHomeDataUseCase.getHiddenFolderPassword(userId)
            _tagData.addSource(passwordSource) { password: String? ->
                viewModelScope.launch(Dispatchers.IO) {
                    if (password.isNullOrBlank()) {
                        urlDao.deleteHiddenFolderSecurity(userId)
                    } else {
                        urlDao.upsertHiddenFolderSecurity(
                            HiddenFolderSecurityEntity(
                                userId = userId,
                                password = password
                            )
                        )
                    }
                }
                _tagData.removeSource(passwordSource)

                passwordLoaded = true
                finishSyncIfNeeded()
            }
        }
    }

    private suspend fun syncUrlBackupSuspend(data: Pair<List<Url>, List<String>>, mode: RemoteSyncMode) {
        val urlDataList = data.first
        val imgUriList = data.second

        // Fetch existing backups from Room to preserve existing imgUri when remote value is blank
        val urlLinks = urlDataList.map { it.url }
        val existingBackups = if (urlLinks.isNotEmpty()) urlDao.getUrlBackupIsExist(urlLinks) else emptyList()
        val existingMap = existingBackups.associateBy { it.urlLink }

        val mergedBackupEntities = urlDataList.mapIndexed { index, url ->
            val imgFromRemote = imgUriList.getOrNull(index).orEmpty()
            val preservedImg = existingMap[url.url]?.imgUri.orEmpty()
            val finalImg = if (imgFromRemote.isBlank()) preservedImg else imgFromRemote

            UrlBackupEntity(
                urlLink = url.url,
                imageKey = url.imageKey,
                imgUri = finalImg,
                favorite = url.favorite,
                hidden = url.hidden,
                timeStamp = url.timeStamp,
                urlName = url.urlName,
                urlMemo = url.urlMemo,
                tag = url.tag
            )
        }

        // Apply to Room synchronously on IO dispatcher
        if (mode == RemoteSyncMode.INSERT) {
            // Similar logic as before: only insert new ones
            val existingLinks = existingBackups.map { it.urlLink }
            val toInsert = mergedBackupEntities.filter { it.urlLink !in existingLinks }
            if (toInsert.isNotEmpty()) {
                urlDao.insertUrlBackup(toInsert)
            }
        } else {
            // REFRESH: replace entire backup table with merged data
            urlDao.deleteAllUrlBackup()
            if (mergedBackupEntities.isNotEmpty()) urlDao.insertUrlBackup(mergedBackupEntities)
        }
    }

    private fun syncTagBackup(tagList: List<Tag>, mode: RemoteSyncMode) {
        val allTagIds = tagList.mapNotNull { it.id }.filter { it.isNotBlank() }
        val localSavedOrder = kotlinx.coroutines.runBlocking {
            urlDao.getAllTagBackups()
                .flatMap { it.tagOrder.orEmpty() }
                .filter { it.isNotBlank() }
                .distinct()
        }

        val preservedOrder = if (localSavedOrder.isNotEmpty()) {
            val remoteSet = allTagIds.toSet()
            (localSavedOrder.filter { it in remoteSet } + allTagIds.filter { it !in localSavedOrder })
                .distinct()
        } else {
            allTagIds
        }

        val tagBackupEntity = tagList.map {
            TagBackupEntity(
                tag = it.tag!!,
                timeStamp = it.timeStamp,
                urlList = it.urlList,
                firebaseTagId = it.id,
                tagOrder = preservedOrder
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

    fun saveTagOrderToLocal(newIdOrder: List<String>) {
        val safeOrder = newIdOrder.filter { it.isNotBlank() && it.startsWith("tag") }
        if (safeOrder.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            val currentTags = urlDao.getAllTagBackups()
            if (currentTags.isEmpty()) return@launch

            val updatedTags = currentTags.map { tag ->
                tag.copy(tagOrder = safeOrder)
            }
            urlDao.updateTagOrder(updatedTags)
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

    fun deleteUrl(url: Url) {
        viewModelScope.launch {
            val session = sessionManager.userSession.first()
            val isLoggedIn = session.autoLogin
            val userId = session.userId ?: ""

            if (isLoggedIn && url.url.isNotBlank() && userId.isNotBlank()) {
                _repo.deleteUserData(url.url, url.imageKey, userId)
            } else {
                _repo.deleteGuestData(url.url)
            }
        }
    }

    /** hideUrl - 숨기기 기능 */
    fun hideUrl(url: Url) {
        viewModelScope.launch {
            val session = sessionManager.userSession.first()
            val userId = session.userId ?: ""

            if (userId.isNotBlank() && url.url.isNotBlank()) {
                _repo.hideUrl(url.url, userId)
            }
        }
    }

    /** showUrl - 숨겨진 URL 표시 */
    fun showUrl(url: String, userId: String) {
        viewModelScope.launch {
            if (userId.isNotBlank() && url.isNotBlank()) {
                _repo.showUrl(url, userId)
            }
        }
    }

    /** getHiddenUrls - 숨겨진 URL 목록 조회 */
    fun getHiddenUrls(): LiveData<List<UrlBackupEntity>> {
        return _repo.getHiddenUrl()
    }

    /** toggleShowHiddenUrls - 숨겨진 URL 표시 토글 */
    fun toggleShowHiddenUrls() {
        _showHiddenUrls.value = !(_showHiddenUrls.value ?: false)
    }

    /** setShowHiddenUrls - 숨겨진 URL 표시 상태 설정 */
    fun setShowHiddenUrls(show: Boolean) {
        _showHiddenUrls.value = show
    }

    /** addReceivedUrl - 공유로 받은 URL 저장 */
    fun addReceivedUrl(url: Url, userId: String) {
        viewModelScope.launch {
            if (userId.isNotBlank()) {
                _repo.insertUserUrl(url, userId)
            } else {
                _repo.insertGuestUrl(url)
            }
        }
    }

    /** saveSharedUrlForLoggedInUser - 로그인 상태에서 공유받은 링크를 Room + Firebase에 저장 */
    fun saveSharedUrlForLoggedInUser(url: Url, userId: String) {
        viewModelScope.launch {
            _repo.saveSharedUrlForLoggedInUser(url, userId)
        }
    }

}
