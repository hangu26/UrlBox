package kr.baeksuk.urlbox.data.repository

import com.google.firebase.auth.FirebaseAuth
import kr.baeksuk.urlbox.domain.feedback.AdminAccessRepository

class FirebaseAdminAccessRepository : AdminAccessRepository {

    private val adminEmails = setOf(
        "fpemgusrn1103@gmail.com",
        "fpemgusrn@gmail.com"
    )

    override suspend fun isAdminUser(): Boolean {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return false
        return email in adminEmails
    }
}

