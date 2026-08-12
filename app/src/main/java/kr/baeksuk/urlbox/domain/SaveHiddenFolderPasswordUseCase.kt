package kr.baeksuk.urlbox.domain

import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.repository.UserRepository
import kr.baeksuk.urlbox.util.util.UserSessionManager

sealed class SaveHiddenFolderPasswordResult {
    object Success : SaveHiddenFolderPasswordResult()
    data class Failure(val message: String) : SaveHiddenFolderPasswordResult()
}

class SaveHiddenFolderPasswordUseCase(
    private val userRepository: UserRepository,
    private val sessionManager: UserSessionManager
) {

    suspend operator fun invoke(password: String): SaveHiddenFolderPasswordResult {
        if (!password.matches(Regex("^\\d{4}$"))) {
            return SaveHiddenFolderPasswordResult.Failure("PIN은 4자리 숫자만 가능합니다.")
        }

        val session = sessionManager.userSession.first()
        if (!session.autoLogin) {
            return SaveHiddenFolderPasswordResult.Failure("로그인 후 비밀번호를 설정해주세요.")
        }

        val userId = session.userId?.takeIf { it.isNotBlank() }
            ?: return SaveHiddenFolderPasswordResult.Failure("사용자 정보를 찾을 수 없습니다.")

        return runCatching {
            userRepository.saveHiddenFolderPassword(userId, password)
        }.fold(
            onSuccess = { SaveHiddenFolderPasswordResult.Success },
            onFailure = {
                SaveHiddenFolderPasswordResult.Failure(
                    it.message ?: "비밀번호 저장에 실패했습니다."
                )
            }
        )
    }
}
