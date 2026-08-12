package kr.baeksuk.urlbox.domain

import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.repository.UserRepository
import kr.baeksuk.urlbox.util.util.UserSessionManager

class LoadUserHomeDataUseCase(
    private val userRepository: UserRepository,
    private val sessionManager: UserSessionManager
) {

    suspend fun getUserId(): String? {
        return sessionManager.userId.first()
    }

    fun getUrlData(userId : String) = userRepository.getUrlData(userId)

    fun getTagData(userId: String) = userRepository.getTagData(userId)

    fun getHiddenFolderPassword(userId: String) = userRepository.getHiddenFolderPassword(userId)

}