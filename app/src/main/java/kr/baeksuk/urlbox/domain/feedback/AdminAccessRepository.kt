package kr.baeksuk.urlbox.domain.feedback

interface AdminAccessRepository {
    suspend fun isAdminUser(): Boolean
}

