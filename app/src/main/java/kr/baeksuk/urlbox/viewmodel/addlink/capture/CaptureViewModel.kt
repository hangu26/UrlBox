package kr.baeksuk.urlbox.viewmodel.addlink.capture

import android.app.Application
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.domain.CaptureContentAction
import kr.baeksuk.urlbox.domain.CaptureLoginStateUseCase
import kr.baeksuk.urlbox.domain.CaptureSaveRequest
import kr.baeksuk.urlbox.domain.CaptureSaveResult
import kr.baeksuk.urlbox.domain.CaptureSaveUseCase
import kr.baeksuk.urlbox.model.UserTags
import java.io.File

sealed class CaptureUiState {
    object Idle : CaptureUiState()
    object Loading : CaptureUiState()
    object Success : CaptureUiState()
    object Duplicate : CaptureUiState()
    data class Error(val message: String) : CaptureUiState()
}

class CaptureViewModel(
    application: Application,
    private val captureLoginStateUseCase: CaptureLoginStateUseCase,
    private val captureSaveUseCase: CaptureSaveUseCase
) : AndroidViewModel(application) {

    private val _btnCloseState = MutableLiveData<Boolean>()
    val btnCloseState = _btnCloseState

    private val _btnCaptureState = MutableLiveData<Boolean>()
    val btnCaptureState = _btnCaptureState

    private val _btnSaveState = MutableLiveData<Boolean>()
    val btnSaveState = _btnSaveState

    private val _btnSkipState = MutableLiveData<Boolean>()
    val btnSkipState = _btnSkipState

    private val _btnCancelState = MutableLiveData<Boolean>()
    val btnCancelState = _btnCancelState

    private val _btnShowTagsState = MutableLiveData<Boolean>()
    val btnShowTagsState = _btnShowTagsState

    private val _btnAddTagsStage = MutableLiveData<Boolean>()
    val btnAddTagsStage = _btnAddTagsStage

    private val _btnAlbumState = MutableLiveData<Boolean>()
    val btnAlbumState = _btnAlbumState

    private val _selectedImageUri = MutableLiveData<Uri?>()
    val selectedImageUri: LiveData<Uri?> = _selectedImageUri

    private val _isLoggedIn = MutableLiveData<Boolean>()
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    private val _saveState = MutableLiveData<CaptureUiState>(CaptureUiState.Idle)
    val saveState: LiveData<CaptureUiState> = _saveState

    var isClicked = 0

    /** 현재 로그인 세션 정보를 조회해서 로그인 여부를 UI 상태에 반영 */
    fun loadSessionState() {
        viewModelScope.launch {
            runCatching { captureLoginStateUseCase() }
                .onSuccess { _isLoggedIn.value = it }
                .onFailure { _isLoggedIn.value = false }
        }
    }

    /** 공유 진입처럼 즉시 필요한 흐름에서 세션 상태를 suspend로 조회 */
    suspend fun getAutoLoginState(): Boolean = captureLoginStateUseCase()

    /** 태그 영역 표시 여부를 토글 */
    fun btnShowTags() {
        _btnShowTagsState.value = isClicked % 2 == 0
        isClicked++
    }

    /** 스킵 버튼 클릭 상태 전달 */
    fun btnSkip() {
        _btnSkipState.value = true
    }

    /** 닫기 버튼 클릭 상태 전달 */
    fun btnClose() {
        _btnCloseState.value = true
    }

    /** 캡처 버튼 클릭 상태 전달 */
    fun btnCapture() {
        _btnCaptureState.value = true
    }

    /** 저장 버튼 클릭 상태 전달 */
    fun btnSave() {
        _btnSaveState.value = true
    }

    /** 공유 인텐트 저장을 완료될 때까지 기다리는 suspend 진입점 */
    suspend fun saveSharedContent(request: CaptureSaveRequest): CaptureSaveResult {
        return captureSaveUseCase(request)
    }

    /** 취소 버튼 클릭 상태 전달 */
    fun btnCancel() {
        _btnCancelState.value = true
    }

    /** 태그 추가 영역을 닫는 상태 전달 */
    fun btnAddTags() {
        _btnAddTagsStage.value = false
    }

    /** 앨범 열기 버튼 클릭 상태 전달 */
    fun btnAlbum() {
        _btnAlbumState.value = true
    }

    /** 앨범에서 선택한 이미지를 현재 미리보기에 반영 */
    fun onAlbumImageSelected(uri: Uri) {
        _selectedImageUri.value = uri
    }

    /** 선택된 앨범 이미지 초기화 */
    fun clearSelectedImage() {
        _selectedImageUri.value = null
    }

    /** 저장 상태를 초기 상태로 되돌림 */
    fun clearSaveState() {
        _saveState.value = CaptureUiState.Idle
    }

    /** 캡처 저장 요청을 UseCase에 전달하고 결과 상태 갱신 */
    fun saveCapture(
        action: CaptureContentAction,
        url: String,
        imageKey: String,
        file: File,
        tags: List<UserTags>,
        isEdit: Boolean,
        txMemo: String
    ) {
        viewModelScope.launch {
            _saveState.value = CaptureUiState.Loading

            val result = captureSaveUseCase(
                CaptureSaveRequest(
                    action = action,
                    url = url,
                    imageKey = imageKey,
                    file = file,
                    tags = tags,
                    isEdit = isEdit,
                    txMemo = txMemo
                )
            )

            _saveState.value = when (result) {
                is CaptureSaveResult.Success -> CaptureUiState.Success
                is CaptureSaveResult.Duplicate -> CaptureUiState.Duplicate
                is CaptureSaveResult.Failure -> CaptureUiState.Error(
                    result.throwable.message ?: "저장에 실패했습니다."
                )
            }
        }
    }

    /** 화면 내 임시 토스트 메시지 표시 */
    private fun toast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /** 기존 호출부를 유지하기 위한 저장 처리 래퍼 실행 */
    private fun launchLegacySave(
        context: Context,
        request: CaptureSaveRequest
    ) {
        viewModelScope.launch {
            when (val result = captureSaveUseCase(request)) {
                is CaptureSaveResult.Success -> toast(context, "저장되었습니다.")
                is CaptureSaveResult.Duplicate -> toast(context, "이미 존재하는 URL입니다.")
                is CaptureSaveResult.Failure -> toast(
                    context,
                    result.throwable.message ?: "저장에 실패했습니다."
                )
            }
        }
    }

    /** 게스트/기존 경로에서 단일 URL 저장 요청 */
    fun insertUrl(urlEntity: UrlEntity, url: String, context: Context) {
        launchLegacySave(
            context = context,
            request = CaptureSaveRequest(
                action = CaptureContentAction.SAVE,
                url = url,
                imageKey = urlEntity.imageKey,
                file = File(context.cacheDir, "${urlEntity.imageKey}.png"),
                tags = emptyList(),
                isEdit = false,
                txMemo = urlEntity.urlMemo.orEmpty()
            )
        )
    }

    /** 로그인 백업 데이터에 태그 1개를 포함해 저장 요청 */
    fun insertBackupUrl(
        urlBackupEntity: UrlBackupEntity,
        url: String,
        context: Context,
        file: File,
        tag: String
    ) {
        launchLegacySave(
            context = context,
            request = CaptureSaveRequest(
                action = CaptureContentAction.SAVE,
                url = url,
                imageKey = urlBackupEntity.imageKey,
                file = file,
                tags = if (tag.isBlank()) emptyList() else listOf(
                    UserTags(tag = tag, timeStamp = System.currentTimeMillis())
                ),
                isEdit = false,
                txMemo = urlBackupEntity.urlMemo.orEmpty()
            )
        )
    }

    /** 로그인 백업 데이터에 여러 태그를 포함해 저장 요청 */
    fun insertBackupUrlMultipleTags(
        urlBackupEntity: UrlBackupEntity,
        url: String,
        context: Context,
        file: File,
        tags: List<UserTags>
    ) {
        launchLegacySave(
            context = context,
            request = CaptureSaveRequest(
                action = CaptureContentAction.SAVE,
                url = url,
                imageKey = urlBackupEntity.imageKey,
                file = file,
                tags = tags,
                isEdit = false,
                txMemo = urlBackupEntity.urlMemo.orEmpty()
            )
        )
    }

    /** 기존 URL 정보 수정 저장 */
    fun updateUrl(urlEntity: UrlEntity, url: String, context: Context) {
        launchLegacySave(
            context = context,
            request = CaptureSaveRequest(
                action = CaptureContentAction.SAVE,
                url = url,
                imageKey = urlEntity.imageKey,
                file = File(context.cacheDir, "${urlEntity.imageKey}.png"),
                tags = urlEntity.tag
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.map { UserTags(tag = it, timeStamp = System.currentTimeMillis()) }
                    .orEmpty(),
                isEdit = true,
                txMemo = urlEntity.urlMemo.orEmpty()
            )
        )
    }

    /** 로그인 백업 URL의 이미지/메타 정보 수정 저장 */
    fun updateBackupUrl(urlBackupEntity: UrlBackupEntity, context: Context, file: File) {
        launchLegacySave(
            context = context,
            request = CaptureSaveRequest(
                action = CaptureContentAction.SAVE,
                url = urlBackupEntity.urlLink,
                imageKey = urlBackupEntity.imageKey,
                file = file,
                tags = urlBackupEntity.tag.orEmpty(),
                isEdit = true,
                txMemo = urlBackupEntity.urlMemo.orEmpty()
            )
        )
    }

}
