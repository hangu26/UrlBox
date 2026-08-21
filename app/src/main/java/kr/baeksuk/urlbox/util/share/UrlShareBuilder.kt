package kr.baeksuk.urlbox.util.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.util.UserSessionManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL

/**
 * Helper class to prepare share payloads: pre-upload images for logged-in users and build JSON payload.
 * Keep URL fragment / UI code thin by delegating image handling here.
 */
class UrlShareBuilder(private val context: Context, private val sessionManager: UserSessionManager) {

    private val TAG = "UrlShareBuilder"

    suspend fun prepareShareableUrls(urls: List<Url>): List<Url> = withContext(Dispatchers.IO) {
       Log.e("UrlShareBuilder", "before encode count=${urls.size}")
       val session = sessionManager.userSession.first()
       val userId = (session.userId ?: FirebaseAuth.getInstance().currentUser?.uid)?.takeIf { it.isNotBlank() }
       val result = urls.map { normalizeShareMetadata(it, userId) }.toMutableList()

       if (userId != null) {
           for (i in result.indices) {
               val u = result[i]
               try {
                   val inferred = inferSenderStorageMetadata(u, userId)
                   val updated = u.copy(
                       senderUid = inferred.senderUid?.takeIf { it.isNotBlank() } ?: u.senderUid,
                       imagePath = inferred.imagePath?.takeIf { it.isNotBlank() } ?: u.imagePath
                   )
                   result[i] = updated

                   Log.d("SHARE_THUMBNAIL_DEBUG", "[BEFORE_SHARE] imageKey=${updated.imageKey}, imagePath=${updated.imagePath ?: ""}, senderUid=${updated.senderUid ?: ""}, imgUri=${updated.imgUri}")
                   Log.d("SHARE_THUMBNAIL_DEBUG", "senderUid=${updated.senderUid ?: ""}, imagePath=${updated.imagePath ?: ""}, imageKey=${updated.imageKey}, imgUri=${updated.imgUri}")
               } catch (e: Exception) {
                   Log.w(TAG, "Pre-upload failed for ${u.url}: ${e.message}")
               }
           }
       }

       result
    }

    fun normalizeShareMetadata(url: Url, fallbackUserId: String? = null): Url {
       val firebaseStoragePath = extractStoragePathFromFirebaseUrl(url.imgUri)
       val imagePathFromFirebase = firebaseStoragePath ?: url.imagePath?.takeIf { it.isNotBlank() }
       val senderUidFromPath = imagePathFromFirebase?.let { parseSenderUidFromImagePath(it) }
       val resolvedSenderUid = url.senderUid?.takeIf { it.isNotBlank() }
           ?: senderUidFromPath
           ?: fallbackUserId
           ?: ""
       val resolvedImagePath = imagePathFromFirebase
           ?: url.imagePath?.takeIf { it.isNotBlank() }
           ?: if (resolvedSenderUid.isNotBlank() && url.imageKey.isNotBlank()) "images/$resolvedSenderUid/${url.imageKey}.png" else ""

       return url.copy(
           senderUid = resolvedSenderUid.takeIf { it.isNotBlank() },
           imagePath = resolvedImagePath.takeIf { it.isNotBlank() }
       )
    }

    private fun inferSenderStorageMetadata(url: Url, currentUserId: String): Url {
       val current = normalizeShareMetadata(url, currentUserId)
       if (!current.senderUid.isNullOrBlank() && !current.imagePath.isNullOrBlank()) {
           return current
       }

       val imageKey = current.imageKey.takeIf { it.isNotBlank() }
       if (imageKey != null && currentUserId.isNotBlank()) {
           return current.copy(
               senderUid = currentUserId,
               imagePath = "images/$currentUserId/${imageKey}.png"
           )
       }

       return current
    }

    private fun extractStoragePathFromFirebaseUrl(rawUrl: String): String? {
        if (rawUrl.isBlank()) return null
        return try {
            val decodedUrl = java.net.URL(rawUrl)
            val path = decodedUrl.path
            val startIndex = path.indexOf("/o/")
            if (startIndex < 0) return null
            val encoded = path.substring(startIndex + 3).substringBefore("?")
            java.net.URLDecoder.decode(encoded, Charsets.UTF_8.name())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract storage path from Firebase URL: ${e.message}")
            null
        }
    }

    private fun parseSenderUidFromImagePath(imagePath: String): String? {
        if (imagePath.isBlank()) return null
        val segments = imagePath.split('/').filter { it.isNotBlank() }
        if (segments.size < 2) return null
        return if (segments[0] == "images") segments[1] else null
    }

    fun resolveKakaoImageUrl(urls: List<Url>): String {
        val remoteImageUrl = urls.asSequence()
            .map { it.imgUri }
            .firstOrNull { uri ->
                uri.startsWith("http://") || uri.startsWith("https://")
            }
        return remoteImageUrl ?: DEFAULT_KAKAO_SHARE_IMAGE_URL
    }

    fun createShareData(urls: List<Url>): String {
        val compactUrls = urls.map { url ->
            val shareUrl = normalizeShareMetadata(url, FirebaseAuth.getInstance().currentUser?.uid)
            val sourceForImage = when {
                shareUrl.imgUri.isNotBlank() -> shareUrl.imgUri
                shareUrl.imageKey.isNotBlank() -> {
                    val candidate = File(context.filesDir, "${shareUrl.imageKey}.png")
                    if (candidate.exists()) candidate.absolutePath else ""
                }
                else -> ""
            }

            val base64Image = if (sourceForImage.isNotBlank()) {
                try {
                    val encoded = compressAndEncodeImageToBase64(sourceForImage)
                    Log.d(TAG, "encodeImage src=${sourceForImage.take(120)} base64Length=${encoded.length}")
                    encoded
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to encode image: ${e.message}")
                    ""
                }
            } else {
                ""
            }

            val payload = mapOf(
                "url" to shareUrl.url,
                "title" to (shareUrl.urlName ?: shareUrl.url).take(80),
                "memo" to (shareUrl.urlMemo ?: "").take(50),
                "imageKey" to (shareUrl.imageKey.takeIf { it.isNotBlank() } ?: ""),
                "imagePath" to (shareUrl.imagePath ?: ""),
                "senderUid" to (shareUrl.senderUid ?: ""),
                "imgBase64" to base64Image,
                "imgUri" to (shareUrl.imgUri.takeIf { it.isNotBlank() } ?: "")
            )
            Log.d("SHARE_THUMBNAIL_DEBUG", "[SHARE_PAYLOAD_DEBUG] json=${org.json.JSONObject(payload).toString()}")
            Log.d("SHARE_THUMBNAIL_DEBUG", "json=${org.json.JSONObject(payload).toString()}")
            payload
        }
        val result = org.json.JSONObject().apply {
            put("type", "urlbox_share")
            put("count", compactUrls.size)
            put("urls", org.json.JSONArray().apply {
                compactUrls.forEach { item ->
                    put(org.json.JSONObject(item))
                }
            })
            put("timestamp", System.currentTimeMillis())
        }.toString()
        Log.d("SHARE_THUMBNAIL_DEBUG", "fullJson=$result")
        return result
    }

    companion object {
        const val DEFAULT_KAKAO_SHARE_IMAGE_URL =
            "https://play-lh.googleusercontent.com/9H4M7h2R2qV0XK8H4VfLzajN-0K_3i1s9yM7sG5cQWk9vx-0r4A6Nw7gqJkWw9QYlA"
    }

    private fun compressAndEncodeImageToBase64(imgSource: String): String {
        if (imgSource.isBlank()) return ""

        val bitmap: Bitmap? = when {
            imgSource.startsWith("http://") || imgSource.startsWith("https://") -> {
                try {
                    URL(imgSource).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load remote image for base64: ${e.message}")
                    null
                }
            }
            imgSource.startsWith("content://") -> {
                try {
                    val uri = Uri.parse(imgSource)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load content uri image for base64: ${e.message}")
                    null
                }
            }
            else -> {
                val file = File(imgSource)
                if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
            }
        }

        if (bitmap == null) return ""

        val scaled = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        val bytes = stream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
