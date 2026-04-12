package kr.baeksuk.urlbox.domain

import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.util.util.UserSessionManager

class UpdateUrlMemoUseCase(
    private val urlRepository: UrlRepository,
    private val sessionManager: UserSessionManager
) {

    suspend operator fun invoke(url: String, urlMemo: String) {

        val isLoggedIn = sessionManager.userSession.first().autoLogin
        val userId = sessionManager.userId.first() ?: ""

        if (isLoggedIn && userId.isNotBlank() && userId.isNotBlank()) {
            urlRepository.updateUrlMemo(url, urlMemo, userId)
        } else {
            urlRepository.updateGuestUrlMemo(url, urlMemo)
        }

    }

}