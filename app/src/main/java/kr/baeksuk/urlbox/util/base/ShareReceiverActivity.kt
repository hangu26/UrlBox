package kr.baeksuk.urlbox.util.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.view.addlink.capture.CaptureActivity
import kr.baeksuk.urlbox.viewmodel.addlink.capture.CaptureViewModel
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class ShareReceiverActivity : AppCompatActivity() {

    private val viewModel: CaptureViewModel by inject() // DI 사용 중이라 가정

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleSharedIntent(intent)
        finish() // 처리 끝나면 종료
    }

    private fun handleSharedIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if (action != Intent.ACTION_SEND || type.isNullOrEmpty()) return

        val directory = filesDir
        val autoLogin = getSharedPreferences("User", Context.MODE_PRIVATE)
            .getBoolean("auto login", false)
        val txMemo = resources.getString(R.string.tx_memo)

        // 1️⃣ 이미지 공유
        val imageUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

        // 2️⃣ 텍스트 공유
        val sharedUrl: String = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim() ?: ""

        if (imageUri != null) {
            // 이미지가 있으면 바로 bitmap으로 저장
            try {
                val bitmap = contentResolver.openInputStream(imageUri)?.use { BitmapFactory.decodeStream(it) }
                if (bitmap != null) {
                    logD("Shared image detected, saving...")
                    saveSharedContent(sharedUrl, bitmap, autoLogin, directory, txMemo)
                } else {
                    logD("Bitmap decode failed from shared image URI.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                logD("Exception decoding shared image: ${e.message}")
            }
        } else if (sharedUrl.contains("youtube.com") || sharedUrl.contains("youtu.be")) {
            // 유튜브 링크이면 썸네일 가져오기 시도
            Thread {
                try {
                    val bitmap = getYouTubeThumbnail(sharedUrl)
                    runOnUiThread {
                        if (bitmap != null) {
                            logD("YouTube thumbnail fetched for $sharedUrl")
                            saveSharedContent(sharedUrl, bitmap, autoLogin, directory, txMemo)
                        } else {
                            logD("Failed to fetch YouTube thumbnail, fallback to default image")
                            saveSharedContent(sharedUrl, getDefaultBitmap(), autoLogin, directory, txMemo)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    runOnUiThread {
                        logD("Exception fetching YouTube thumbnail: ${e.message}")
                        saveSharedContent(sharedUrl, getDefaultBitmap(), autoLogin, directory, txMemo)
                    }
                }
            }.start()
        } else if (sharedUrl.isNotEmpty()) {
            // 단순 텍스트 URL 공유
            saveSharedContent(sharedUrl, getDefaultBitmap(), autoLogin, directory, txMemo)
        } else {
            logD("No valid image or URL shared.")
        }
    }

    /** 기본 이미지 비트맵 **/
    private fun getDefaultBitmap(): Bitmap {
        val drawable = getDrawable(R.drawable.urlbox_icon)!!
        return (drawable as BitmapDrawable).bitmap
    }

    // YouTube 썸네일 가져오기
    private fun getYouTubeThumbnail(url: String): Bitmap? {
        try {
            val videoId = extractYouTubeId(url) ?: return null
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
            logD("YouTube ID: $videoId, Thumbnail URL: $thumbnailUrl")

            val connection = java.net.URL(thumbnailUrl).openConnection()
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val inputStream = connection.getInputStream()
            return BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            logD("썸네일 다운로드 중 예외 발생: ${e.message}")
            return null
        }
    }

    private fun extractYouTubeId(url: String): String? {
        val regex = "(?<=v=|/videos/|youtu.be/|/embed/)[^#&?\\n]*".toRegex()
        val match = regex.find(url)
        return match?.value
    }

    /** 공유된 내용 저장 **/
    private fun saveSharedContent(url: String, bitmap: Bitmap, autoLogin: Boolean, directory: File, txMemo: String) {
        val imageKey = UUID.randomUUID().toString()
        val file = File(directory, "$imageKey.png")
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            if (autoLogin) {
                val entity = UrlBackupEntity(
                    urlLink = url,
                    imageKey = imageKey,
                    favorite = false,
                    imgUri = "",
                    timeStamp = System.currentTimeMillis(),
                    urlName = url,
                    urlMemo = txMemo ?: "메모",
                    tag = emptyList()
                )
                logD("저장됨: $url")
                viewModel.insertBackupUrl(entity, url, this, file, "")
            } else {
                val entity = UrlEntity(
                    urlLink = url,
                    imageKey = imageKey,
                    favorite = false,
                    timeStamp = System.currentTimeMillis(),
                    urlName = url,
                    urlMemo = txMemo ?: "메모",
                    tag = ""
                )
                viewModel.insertUrl(entity, url, this)
            }

            logD("Shared content saved successfully for URL: $url")
        } catch (e: Exception) {
            e.printStackTrace()
            logD("Failed to save shared content: ${e.message}")
        }
    }

    private fun logD(msg: String) = Log.d("공유 인텐트", msg)
}