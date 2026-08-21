package kr.baeksuk.urlbox.viewmodel.share

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.view.View
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.domain.CaptureContentAction
import kr.baeksuk.urlbox.domain.CaptureSaveRequest
import kr.baeksuk.urlbox.domain.CaptureSaveResult
import kr.baeksuk.urlbox.domain.CaptureSaveUseCase
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class ShareReceiverFallbackUiState {
    object Idle : ShareReceiverFallbackUiState()
    object Loading : ShareReceiverFallbackUiState()
    object Finished : ShareReceiverFallbackUiState()
    data class Error(val message: String) : ShareReceiverFallbackUiState()
}

class ShareReceiverViewModelFallback(
    application: Application,
    private val captureSaveUseCase: CaptureSaveUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ShareReceiverFallbackUiState>(ShareReceiverFallbackUiState.Idle)
    val uiState: StateFlow<ShareReceiverFallbackUiState> = _uiState.asStateFlow()

    fun handleSharedIntent(sharedIntent: Intent) {
        viewModelScope.launch {
            _uiState.value = ShareReceiverFallbackUiState.Loading

            val result = runCatching { processSharedIntent(sharedIntent) }
            _uiState.value = when {
                result.isFailure -> ShareReceiverFallbackUiState.Error(
                    result.exceptionOrNull()?.message ?: "공유 처리에 실패했습니다."
                )
                result.getOrNull() == null -> ShareReceiverFallbackUiState.Finished
                result.getOrNull() is CaptureSaveResult.Success -> ShareReceiverFallbackUiState.Finished
                result.getOrNull() is CaptureSaveResult.Duplicate -> ShareReceiverFallbackUiState.Error("이미 존재하는 URL입니다.")
                result.getOrNull() is CaptureSaveResult.Failure -> ShareReceiverFallbackUiState.Error(
                    (result.getOrNull() as CaptureSaveResult.Failure).throwable.message
                        ?: "저장에 실패했습니다."
                )
                else -> ShareReceiverFallbackUiState.Finished
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
            sharedUrl.isNotBlank() -> loadSharedUrlThumbnail(sharedUrl)
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

    private suspend fun loadSharedUrlThumbnail(url: String): Bitmap = withContext(Dispatchers.IO) {
        val youtube = if (isYouTubeUrl(url)) getYouTubeThumbnail(url) else null
        val webpage = getWebPageThumbnail(url)
        val screenshot = webpage?.let { null } ?: capturePageScreenshot(url)
        youtube ?: webpage ?: screenshot ?: getDefaultBitmap()
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

    private suspend fun getWebPageThumbnail(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val safeUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
            val doc = Jsoup.connect(safeUrl)
                .userAgent("Mozilla/5.0")
                .timeout(5000)
                .followRedirects(true)
                .get()

            val rawImageUrl = listOf(
                doc.selectFirst("meta[property=og:image]")?.attr("content"),
                doc.selectFirst("meta[property=og:image:url]")?.attr("content"),
                doc.selectFirst("meta[name=twitter:image]")?.attr("content"),
                doc.selectFirst("meta[name=twitter:image:url]")?.attr("content"),
                doc.select("img").firstOrNull()?.attr("src"),
                doc.select("link[rel~=icon]").firstOrNull()?.attr("href")
            ).firstOrNull { !it.isNullOrBlank() }

            val resolvedImageUrl = rawImageUrl?.let { imageUrl ->
                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    imageUrl
                } else {
                    try {
                        java.net.URI(safeUrl).resolve(imageUrl).toString()
                    } catch (_: Exception) {
                        null
                    }
                }
            }

            if (resolvedImageUrl.isNullOrBlank()) {
                return@withContext null
            }

            val connection = java.net.URL(resolvedImageUrl).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.getInputStream().use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: Exception) {
            Log.e(TAG, "웹페이지 썸네일 다운로드 실패: ${e.message}")
            null
        }
    }

    private suspend fun capturePageScreenshot(url: String): Bitmap? = withContext(Dispatchers.Main) {
        val safeUrl = if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
        val webView = WebView(getApplication())
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)
        }

        val pageLoadLatch = java.util.concurrent.CountDownLatch(1)
        var captured: Bitmap? = null

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                try {
                    val width = view?.width?.coerceAtLeast(1) ?: 1080
                    val height = view?.height?.coerceAtLeast(1) ?: 1440
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    view?.draw(canvas)
                    captured = bitmap
                } catch (e: Exception) {
                    Log.e(TAG, "페이지 스크린샷 캡처 실패: ${e.message}")
                } finally {
                    pageLoadLatch.countDown()
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: android.webkit.WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                pageLoadLatch.countDown()
            }
        }

        webView.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1440, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, 1080, 1440)

        try {
            webView.loadUrl(safeUrl)
            if (!pageLoadLatch.await(15, java.util.concurrent.TimeUnit.SECONDS)) {
                Log.d(TAG, "페이지 로드 타임아웃: $safeUrl")
            }
        } catch (e: Exception) {
            Log.e(TAG, "스크린샷 로드 실패: ${e.message}")
        }

        val result = captured
        webView.destroy()
        result
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
