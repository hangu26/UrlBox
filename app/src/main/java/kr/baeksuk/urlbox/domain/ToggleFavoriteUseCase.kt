package kr.baeksuk.urlbox.domain

import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.util.util.UserSessionManager

class ToggleFavoriteUseCase(
    private val urlRepository: UrlRepository,
    private val sessionManager: UserSessionManager
) {

    suspend operator fun invoke(url: String, isFavorite : Boolean) {

        val isLoggedIn = sessionManager.userSession.first().autoLogin
        val userId = sessionManager.userId.first() ?: ""

        if (isLoggedIn && userId.isNotBlank()){
            urlRepository.updateUserFavorite(url, isFavorite, userId)
        } else {
            urlRepository.updateFavorite(url, isFavorite)
        }

    }

}