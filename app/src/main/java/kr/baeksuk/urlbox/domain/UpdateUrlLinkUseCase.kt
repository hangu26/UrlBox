package kr.baeksuk.urlbox.domain

import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.util.util.UserSessionManager

class UpdateUrlLinkUseCase(
    private val urlRepository: UrlRepository,
    private val sessionManager: UserSessionManager
) {

    suspend operator fun invoke(oldUrl: String, newUrl: String) {

        val isLoggedIn = sessionManager.userSession.first().autoLogin
        val userId = sessionManager.userId.first() ?: ""

        if (isLoggedIn && userId.isNotBlank() && userId.isNotEmpty()) {
            // 새로운 URL과 기존 URL이 다르고, 새로운 URL이 이미 존재하는지 확인
            if (oldUrl != newUrl && urlRepository.hasBackupUrl(newUrl)) {
                throw IllegalStateException("이미 존재하는 URL입니다.")
            }
            urlRepository.updateUrlLink(oldUrl, newUrl, userId)
        } else {
            // Guest 사용자도 동일하게 검증
            if (oldUrl != newUrl && urlRepository.hasGuestUrl(newUrl)) {
                throw IllegalStateException("이미 존재하는 URL입니다.")
            }
            urlRepository.updateGuestUrlLink(oldUrl, newUrl)
        }

    }
}
