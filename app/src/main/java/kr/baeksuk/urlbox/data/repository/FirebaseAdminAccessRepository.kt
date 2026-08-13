package kr.baeksuk.urlbox.data.repository

import com.google.firebase.auth.FirebaseAuth
import kr.baeksuk.urlbox.domain.feedback.AdminAccessRepository
import kr.baeksuk.urlBox.BuildConfig

class FirebaseAdminAccessRepository : AdminAccessRepository {

    private val adminEmails: Set<String> by lazy {
        val cfg = BuildConfig.ADMIN_EMAILS
        if (cfg.isBlank()) {
            setOf("fpemgusrn1103@gmail.com", "fpemgusrn@gmail.com")
        } else {
            cfg.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        }
    }

    override suspend fun isAdminUser(): Boolean {
        val user = FirebaseAuth.getInstance().currentUser ?: return false
        // treat anonymous users and accounts without email as non-admin (guest)
        if (user.isAnonymous) return false
        val email = user.email ?: return false
        return email in adminEmails
    }
}

