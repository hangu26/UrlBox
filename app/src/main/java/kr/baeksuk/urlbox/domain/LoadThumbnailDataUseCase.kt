package kr.baeksuk.urlbox.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.util.UserSessionManager
import kr.baeksuk.urlbox.view.nav.ThumbnailState

class LoadThumbnailDataUseCase(
    private val repository: UrlRepository,
    private val sessionManager: UserSessionManager
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeThumbnailState(): Flow<ThumbnailState> {
        return sessionManager.userSession.flatMapLatest { session ->
            if (session.autoLogin) {
                combine(
                    repository.getUserUrlBackupFlow(),
                    repository.getUserTagBackupFlow()
                ) { urls: List<UrlBackupEntity>, tags: List<TagBackupEntity> ->
                    ThumbnailState.Login(urls, tags)
                }
            } else {
                repository.getGuestUrlFlow().map { urls: List<UrlEntity> ->
                    ThumbnailState.Guest(urls)
                }
            }
        }
    }
}
