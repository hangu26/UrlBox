package kr.baeksuk.urlbox.util.share

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.database.FirebaseDatabase
import com.kakao.sdk.share.ShareClient
import com.kakao.sdk.template.model.Button
import com.kakao.sdk.template.model.Content
import com.kakao.sdk.template.model.FeedTemplate
import com.kakao.sdk.template.model.Link
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.util.UserSessionManager

class UrlShareUseCase(
    private val fragment: Fragment,
    private val sessionManager: UserSessionManager
) {
    companion object {
        private const val SHARE_TTL_MILLIS = 48 * 60 * 60 * 1000L // 48 hours
    }

    fun shareSelectedUrls(
        urls: List<Url>,
        anchorView: View? = null,
        extraAboveOffsetPx: Int = 0,
        allowOverflow: Boolean = false
    ) {
        if (urls.isEmpty()) {
            Toast.makeText(fragment.requireContext(), "공유할 URL을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        showShareTypeDialog(
            urls,
            anchorView ?: fragment.requireView(),
            extraAboveOffsetPx,
            allowOverflow
        )
    }

    private fun showShareTypeDialog(
        urls: List<Url>,
        anchorView: View,
        extraAboveOffsetPx: Int = 0,
        allowOverflow: Boolean = false
    ) {
        val popupView = LayoutInflater.from(fragment.requireContext())
            .inflate(R.layout.popup_share_method, null)
        val kakaoText = popupView.findViewById<TextView>(R.id.tv_kakao_share)
        val urlText = popupView.findViewById<TextView>(R.id.tv_url_share)

        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            isFocusable = true
            isClippingEnabled = !allowOverflow
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 16f
        }

        kakaoText.setOnClickListener {
            popupWindow.dismiss()
            shareToKakaoWithFallback(urls)
        }
        urlText.setOnClickListener {
            popupWindow.dismiss()
            shareUrls(urls)
        }

        val popupHeight = popupView.measuredHeight.takeIf { it > 0 }
            ?: run {
                popupView.measure(
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                )
                popupView.measuredHeight
            }

        val xOffset = anchorView.width + fragment.resources.getDimensionPixelSize(R.dimen.share_popup_offset_x)
        val yOffset = -(anchorView.height + fragment.resources.getDimensionPixelSize(R.dimen.share_popup_offset_y) + extraAboveOffsetPx)

        if (allowOverflow) {
            popupWindow.showAsDropDown(anchorView, xOffset, yOffset)
            return
        }

        popupWindow.showAsDropDown(anchorView, xOffset, yOffset)
    }

    fun shareToKakaoWithFallback(urls: List<Url>) {
        if (urls.isEmpty()) return

        fragment.lifecycleScope.launch {
            val builder = UrlShareBuilder(fragment.requireContext(), sessionManager)
            val prepared = builder.prepareShareableUrls(urls)
            val shareId = savePendingShareBundle(prepared)
            val deepLink = "urlbox://share?id=$shareId"
            val imageUrl = builder.resolveKakaoImageUrl(prepared)
            shareSingleKakaoLink(
                deepLink,
                "UrlBox에서 ${urls.size}개의 링크를 보냈어요",
                "앱에서 열기",
                imageUrl
            )
        }
    }

    private fun shareSingleKakaoLink(
        deepLink: String,
        title: String,
        buttonText: String,
        imageUrl: String = UrlShareBuilder.DEFAULT_KAKAO_SHARE_IMAGE_URL
    ) {
        val appPackageId = "kr.baeksuk.urlBox"
        val playStoreUrl = "https://play.google.com/store/apps/details?id=$appPackageId"

        val template = FeedTemplate(
            content = Content(
                title = title,
                description = "앱에서 열기 버튼으로 전체 링크 묶음을 저장해보세요.",
                imageUrl = imageUrl,
                link = Link(
                    webUrl = deepLink,
                    mobileWebUrl = deepLink,
                    androidExecutionParams = mapOf("id" to deepLink.substringAfter("id="))
                )
            ),
            buttons = listOf(
                Button(
                    buttonText,
                    Link(
                        webUrl = deepLink,
                        mobileWebUrl = deepLink,
                        androidExecutionParams = mapOf("id" to deepLink.substringAfter("id="))
                    )
                ),
                Button(
                    "설치하기",
                    Link(
                        webUrl = playStoreUrl,
                        mobileWebUrl = playStoreUrl
                    )
                )
            )
        )

        if (ShareClient.instance.isKakaoTalkSharingAvailable(fragment.requireContext())) {
            ShareClient.instance.shareDefault(fragment.requireContext(), template) { result, error ->
                if (error != null) {
                    Log.e("ShareError", "Failed to share single bulk link", error)
                    Toast.makeText(fragment.requireContext(), "카카오톡 공유창을 열지 못했습니다.", Toast.LENGTH_SHORT).show()
                    return@shareDefault
                }

                if (result != null) {
                    try {
                        fragment.startActivity(result.intent)
                    } catch (e: Exception) {
                        Log.e("ShareLaunch", "Failed to launch Kakao share intent", e)
                        Toast.makeText(fragment.requireContext(), "카카오톡 실행에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(fragment.requireContext(), "카카오톡이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareUrls(urls: List<Url>) {
        if (urls.isEmpty()) return

        val shareText = urls.joinToString("\n") { url ->
            url.urlName?.takeIf { it.isNotBlank() } ?: url.url
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "공유할 URL")
        }

        val chooserIntent = Intent.createChooser(sendIntent, "URL 공유")
        fragment.startActivity(chooserIntent)
    }

    private suspend fun savePendingShareBundle(urls: List<Url>): String {
        val shareId = java.util.UUID.randomUUID().toString()
        val payload = createShareData(urls)
        val prefs = fragment.requireContext().getSharedPreferences("pending_share_store", Context.MODE_PRIVATE)
        prefs.edit().putString("share_$shareId", payload).apply()
        try {
            val createdAt = System.currentTimeMillis()
            val senderUid = sessionManager.userSession.first().userId?.takeIf { it.isNotBlank() } ?: ""
            val remotePayload = mapOf(
                "payload" to payload,
                "senderUid" to senderUid,
                "createdAt" to createdAt,
                "expiresAt" to (createdAt + SHARE_TTL_MILLIS),
                "consumed" to false,
                "consumeCount" to 0
            )
            FirebaseDatabase.getInstance().reference
                .child("pending_shares")
                .child(shareId)
                .setValue(remotePayload)
                .await()
        } catch (e: Exception) {
            Log.e("UrlShareUseCase", "Failed to save pending share bundle to Firebase: ${e.message}", e)
        }
        return shareId
    }

    private fun createShareData(urls: List<Url>): String {
        val builder = UrlShareBuilder(fragment.requireContext(), sessionManager)
        val fallbackUserId = runCatching {
            runBlocking { sessionManager.userSession.first().userId }
        }.getOrNull()

        val normalizedUrls = urls.map { builder.normalizeShareMetadata(it, fallbackUserId) }
        val compactUrls = normalizedUrls.map { url ->
            mapOf(
                "url" to url.url,
                "title" to (url.urlName ?: url.url).take(80),
                "memo" to (url.urlMemo ?: "").take(50),
                "imageKey" to (url.imageKey.takeIf { it.isNotBlank() } ?: ""),
                "imagePath" to (url.imagePath ?: ""),
                "senderUid" to (url.senderUid ?: ""),
                "imgUri" to (url.imgUri.takeIf { it.isNotBlank() } ?: "")
            )
        }

        return org.json.JSONObject().apply {
            put("type", "urlbox_share")
            put("count", compactUrls.size)
            put("urls", org.json.JSONArray().apply {
                compactUrls.forEach { item -> put(org.json.JSONObject(item)) }
            })
            put("timestamp", System.currentTimeMillis())
        }.toString()
    }
}
