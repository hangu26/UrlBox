package kr.baeksuk.urlbox.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.util.UserSessionManager

class LoadDetailDataUseCase(
    private val repository: UrlRepository,
    private val sessionManager: UserSessionManager
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDetailUrls(): Flow<List<Url>> {
        return sessionManager.userSession.flatMapLatest { session ->
            if (session.autoLogin) {
                repository.getUserUrlBackupFlow().map { backups ->
                    backups.sortedByDescending { it.timeStamp }.map { entity ->
                        Url(
                            url = entity.urlLink,
                            imageKey = entity.imageKey,
                            imgUri = entity.imgUri,
                            favorite = entity.favorite,
                            timeStamp = entity.timeStamp,
                            urlName = entity.urlName,
                            urlMemo = entity.urlMemo,
                            tag = entity.tag
                        )
                    }
                }
            } else {
                repository.getGuestUrlFlow().map { urls ->
                    urls.map { entity ->
                        Url(
                            url = entity.urlLink,
                            imageKey = entity.imageKey,
                            favorite = entity.favorite,
                            timeStamp = entity.timeStamp,
                            urlName = entity.urlName,
                            urlMemo = entity.urlMemo
                        )
                    }
                }
            }
        }
    }
}