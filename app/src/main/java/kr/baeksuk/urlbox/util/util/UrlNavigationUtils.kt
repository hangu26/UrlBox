package kr.baeksuk.urlbox.util.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.core.net.toUri

object UrlNavigationUtils {
    fun normalizeUrl(input: String?): String? {
        val trimmed = input?.trim().orEmpty()
        if (trimmed.isBlank()) return null

        val withScheme = if (
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }

        val uri = Uri.parse(withScheme)
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host.orEmpty()
        val hasWebPattern = Patterns.WEB_URL.matcher(withScheme).matches()

        return if ((scheme == "http" || scheme == "https") && host.isNotBlank() && hasWebPattern) {
            withScheme
        } else {
            null
        }
    }

    fun openUrl(context: Context, rawUrl: String?): Boolean {
        val normalizedUrl = normalizeUrl(rawUrl) ?: return false
        val intent = Intent(Intent.ACTION_VIEW, normalizedUrl.toUri()).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
