package kr.baeksuk.urlbox.viewmodel.share

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.domain.CaptureContentAction
import kr.baeksuk.urlbox.domain.CaptureSaveRequest
import kr.baeksuk.urlbox.domain.CaptureSaveResult
import kr.baeksuk.urlbox.domain.CaptureSaveUseCase
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class ShareReceiverUiState {
    object Idle : ShareReceiverUiState()
    object Loading : ShareReceiverUiState()
    object Finished : ShareReceiverUiState()
    data class Error(val message: String) : ShareReceiverUiState()
}

class ShareReceiverViewModel(
    application: Application,
    private val captureSaveUseCase: CaptureSaveUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ShareReceiverUiState>(ShareReceiverUiState.Idle)
    val uiState: StateFlow<ShareReceiverUiState> = _uiState.asStateFlow()

    fun handleSharedIntent(sharedIntent: Intent) {
        viewModelScope.launch {
            _uiState.value = ShareReceiverUiState.Loading

            val result = runCatching { processSharedIntent(sharedIntent) }
            _uiState.value = when {
                result.isFailure -> ShareReceiverUiState.Error(
                    result.exceptionOrNull()?.message ?: "공유 처리에 실패했습니다."
                )
                result.getOrNull() == null -> ShareReceiverUiState.Finished
                result.getOrNull() is CaptureSaveResult.Success -> ShareReceiverUiState.Finished
                result.getOrNull() is CaptureSaveResult.Duplicate -> ShareReceiverUiState.Error("이미 존재하는 URL입니다.")
                result.getOrNull() is CaptureSaveResult.Failure -> ShareReceiverUiState.Error(
                    (result.getOrNull() as CaptureSaveResult.Failure).throwable.message
                        ?: "저장에 실패했습니다."
                )
                else -> ShareReceiverUiState.Finished
            }
        }
    }

    private suspend fun processSharedIntent(sharedIntent: Intent): CaptureSaveResult? {
        val action = sharedIntent.action
        val type = sharedIntent.type

        if (action != Intent.ACTION_SEND || type.isNullOrEmpty()) {
            Log.d(TAG, "지원하지 않는 공유 인텐트입니다.")
            return null
        }

        val application = getApplication<Application>()
        val directory = application.filesDir
        val txMemo = application.getString(R.string.tx_memo)

        val imageUri: Uri? = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            sharedIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            sharedIntent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        val sharedUrl = sharedIntent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (imageUri != null && sharedUrl.isBlank()) {
            Log.d(TAG, "이미지 공유지만 URL이 없어 저장하지 않습니다.")
            return null
        }

        val bitmap = when {
            imageUri != null -> loadBitmap(imageUri) ?: return null
            isYouTubeUrl(sharedUrl) -> getYouTubeThumbnail(sharedUrl) ?: getDefaultBitmap()
            sharedUrl.isNotBlank() -> getDefaultBitmap()
            else -> {
                Log.d(TAG, "공유된 URL이 없습니다.")
                return null
            }
        }

        return saveSharedContent(
            url = sharedUrl,
            bitmap = bitmap,
            directory = directory,
            txMemo = txMemo
        )
    }

    private suspend fun loadBitmap(imageUri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        try {
            getApplication<Application>().contentResolver.openInputStream(imageUri)?.use {
                BitmapFactory.decodeStream(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "공유 이미지 디코딩 실패: ${e.message}")
            null
        }
    }

    private suspend fun getYouTubeThumbnail(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val videoId = extractYouTubeId(url) ?: return@withContext null
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
            Log.d(TAG, "YouTube Thumbnail URL: $thumbnailUrl")

            val connection = java.net.URL(thumbnailUrl).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.getInputStream().use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "유튜브 썸네일 다운로드 실패: ${e.message}")
            null
        }
    }

    private fun extractYouTubeId(url: String): String? {
        val regex = "(?<=v=|/videos/|youtu.be/|/embed/)[^#&?\\n]*".toRegex()
        return regex.find(url)?.value
    }

    private fun isYouTubeUrl(url: String): Boolean {
        return url.contains("youtube.com") || url.contains("youtu.be")
    }

    private fun getDefaultBitmap(): Bitmap {
        val drawable = AppCompatResources.getDrawable(getApplication(), R.drawable.urlbox_icon)!!
        return (drawable as BitmapDrawable).bitmap
    }

    private suspend fun saveSharedContent(
        url: String,
        bitmap: Bitmap,
        directory: File,
        txMemo: String
    ): CaptureSaveResult {
        val imageKey = UUID.randomUUID().toString()
        val file = File(directory, "$imageKey.png")

        return try {
            withContext(Dispatchers.IO) {
                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
            }

            captureSaveUseCase(
                CaptureSaveRequest(
                    action = CaptureContentAction.SAVE,
                    url = url,
                    imageKey = imageKey,
                    file = file,
                    tags = emptyList(),
                    isEdit = false,
                    txMemo = txMemo.ifBlank { "메모" }
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "공유 저장 실패: ${e.message}")
            CaptureSaveResult.Failure(e)
        }
    }

    private companion object {
        private const val TAG = "ShareReceiverVM"
    }
}

