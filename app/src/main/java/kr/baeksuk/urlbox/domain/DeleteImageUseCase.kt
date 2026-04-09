package kr.baeksuk.urlbox.domain

import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.util.util.UserSessionManager

class DeleteImageUseCase(
    private val urlRepository: UrlRepository,
    private val sessionManager: UserSessionManager
) {

    suspend operator fun invoke(url: String, imageKey: String) {

        val isLoggedIn = sessionManager.userSession.first().autoLogin
        val userId = sessionManager.userSession.first().userId ?: ""

        if (isLoggedIn && url.isNotBlank() && userId.isNotBlank()){
            urlRepository.deleteUserData(url, imageKey, userId)
        } else {
            urlRepository.deleteGuestData(url)
        }

    }

}