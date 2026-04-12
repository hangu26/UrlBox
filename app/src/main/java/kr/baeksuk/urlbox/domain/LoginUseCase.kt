package kr.baeksuk.urlbox.domain

import kr.baeksuk.urlbox.data.repository.UserRepository
import kr.baeksuk.urlbox.model.UrlToLogin
import kr.baeksuk.urlbox.model.User
import kr.baeksuk.urlbox.model.UserSession
import kr.baeksuk.urlbox.util.util.SessionCache
import kr.baeksuk.urlbox.util.util.UserSessionManager
import java.io.File

data class LoginRequest(
    val user : User,
    val isUpload : Boolean,
    val urlList : List<UrlToLogin> = emptyList(),
    val imgFileList: List<File> = emptyList(),
    val autoLogin: Boolean = true
)

sealed class LoginResult{
    object Success : LoginResult()
    data class Failure(val throwable: Throwable) : LoginResult()
}

class LoginUseCase(
    private val userRepository: UserRepository,
    private val sessionManager: UserSessionManager
) {

    suspend operator fun invoke(request : LoginRequest) : LoginResult {

        return runCatching {
            require(request.user.userId.isNotEmpty()) { "아이디를 입력해주세요."}

            if (request.isUpload){
                userRepository.insertAllDataSuspend(
                    user = request.user,
                    urlList = request.urlList,
                    imgFileList = request.imgFileList
                )
            }else {
                userRepository.insertUserIdSuspend(request.user)
            }

            val session = UserSession(
                userId = request.user.userId,
                userEmail = request.user.userEmail,
                userName = request.user.userName,
                userProfile = request.user.profileImage ?: "",
                autoLogin = request.autoLogin
            )

            sessionManager.saveLogin(
                userId = session.userId ?: "",
                userEmail = session.userEmail ?: "",
                userName = session.userName ?: "",
                userProfile = session.userProfile ?: "",
                autoLogin = session.autoLogin
            )

            SessionCache.current = session

        }.fold(
            onSuccess = { LoginResult.Success },
            onFailure = { throwable -> LoginResult.Failure(throwable) }
        )

    }

}