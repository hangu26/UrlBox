package kr.baeksuk.urlbox.domain

import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.util.util.UserSessionManager
import kr.baeksuk.urlbox.view.nav.ThumbnailState

class LoadThumbnailDataUseCase(
    private val repository: UrlRepository,
    private val sessionManager: UserSessionManager
) {
    suspend operator fun invoke(): ThumbnailState {
        val session = sessionManager.userSession.first()

        return if (session.autoLogin) {
            val urls = repository.getUserUrlBackup().value.orEmpty()
            val tags = repository.getUserTagBackup().value.orEmpty()
            ThumbnailState.Login(urls, tags)
        } else {
            val urls = repository.getGuestUrl().value.orEmpty()
            ThumbnailState.Guest(urls)
        }
    }
}
