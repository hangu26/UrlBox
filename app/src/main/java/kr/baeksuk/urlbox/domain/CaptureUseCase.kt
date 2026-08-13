package kr.baeksuk.urlbox.domain

import kotlinx.coroutines.flow.first
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.data.repository.UrlRepository
import kr.baeksuk.urlbox.model.UserTags
import kr.baeksuk.urlbox.util.util.UserSessionManager
import java.io.File

enum class CaptureContentAction {
    SAVE,
    SKIP
}

data class CaptureSaveRequest(
    val action: CaptureContentAction,
    val url: String,
    val imageKey: String,
    val file: File,
    val tags: List<UserTags> = emptyList(),
    val isEdit: Boolean = false,
    val txMemo: String = ""
)

sealed class CaptureSaveResult {
    object Success : CaptureSaveResult()
    object Duplicate : CaptureSaveResult()
    data class Failure(val throwable: Throwable) : CaptureSaveResult()
}

class CaptureLoginStateUseCase(
    private val sessionManager: UserSessionManager
) {
    suspend operator fun invoke(): Boolean {
        return sessionManager.userSession.first().autoLogin
    }
}

class CaptureSaveUseCase(
    private val urlRepository: UrlRepository,
    private val sessionManager: UserSessionManager
) {

    suspend operator fun invoke(request: CaptureSaveRequest): CaptureSaveResult {
        return try {
            val session = sessionManager.userSession.first()
            val isLoggedIn = session.autoLogin
            val userId = session.userId.orEmpty().takeIf { it.isNotBlank() }
            val normalizedTags = request.tags.filter { !it.tag.isNullOrBlank() }

            if (isLoggedIn && userId != null) {
                handleLoggedInRequest(
                    request = request,
                    userId = userId,
                    tags = normalizedTags
                )
            } else {
                handleGuestRequest(
                    request = request
                )
            }

            CaptureSaveResult.Success
        } catch (_: DuplicateUrlException) {
            CaptureSaveResult.Duplicate
        } catch (throwable: Throwable) {
            CaptureSaveResult.Failure(throwable)
        }
    }

    /** handleLoggedInRequest */
    private suspend fun handleLoggedInRequest(
        request: CaptureSaveRequest,
        userId: String,
        tags: List<UserTags>
    ) {
        when (request.action) {
            CaptureContentAction.SKIP -> {
                if (!request.isEdit && urlRepository.hasBackupUrl(request.url)) {
                    throw DuplicateUrlException()
                }

                val backupEntity = buildBackupEntity(
                    url = request.url,
                    imageKey = request.imageKey,
                    file = request.file,
                    urlMemo = request.txMemo,
                    tags = if (tags.isNotEmpty()) tags else listOf(
                        UserTags(tag = "", timeStamp = System.currentTimeMillis())
                    )
                )

                if (request.isEdit) {
                    urlRepository.updateBackup(backupEntity, request.file, userId)
                } else {
                    urlRepository.insertBackup(backupEntity, request.file, "", userId)
                }
            }

            CaptureContentAction.SAVE -> {
                if (!request.isEdit && urlRepository.hasBackupUrl(request.url)) {
                    throw DuplicateUrlException()
                }

                val backupEntity = buildBackupEntity(
                    url = request.url,
                    imageKey = request.imageKey,
                    file = request.file,
                    urlMemo = request.txMemo,
                    tags = tags
                )

                if (request.isEdit) {
                    urlRepository.updateBackup(backupEntity, request.file, userId)
                } else {
                    urlRepository.insertBackupMultipleTags(
                        urlBackupEntity = backupEntity,
                        file = request.file,
                        tags = tags,
                        userId = userId
                    )
                }
            }
        }
    }

    /** handleGuestRequest */
    private suspend fun handleGuestRequest(request: CaptureSaveRequest) {
        when (request.action) {
            CaptureContentAction.SKIP -> {
                if (!request.isEdit && urlRepository.hasGuestUrl(request.url)) {
                    throw DuplicateUrlException()
                }

                val guestEntity = buildGuestEntity(
                    url = request.url,
                    imageKey = request.imageKey,
                    urlMemo = request.txMemo,
                    tags = emptyList()
                )

                if (request.isEdit) {
                    urlRepository.update(guestEntity)
                } else {
                    urlRepository.insert(guestEntity)
                }
            }

            CaptureContentAction.SAVE -> {
                if (!request.isEdit && urlRepository.hasGuestUrl(request.url)) {
                    throw DuplicateUrlException()
                }

                val tagString = request.tags
                    .mapNotNull { it.tag?.takeIf(String::isNotBlank) }
                    .joinToString(",")

                val guestEntity = buildGuestEntity(
                    url = request.url,
                    imageKey = request.imageKey,
                    urlMemo = request.txMemo,
                    tags = request.tags,
                    tagString = tagString
                )

                if (request.isEdit) {
                    urlRepository.update(guestEntity)
                } else {
                    urlRepository.insert(guestEntity)
                }
            }
        }
    }

    /** buildBackupEntity */
    private fun buildBackupEntity(
        url: String,
        imageKey: String,
        file: File,
        urlMemo: String,
        tags: List<UserTags>
    ): UrlBackupEntity {
        return UrlBackupEntity(
            urlLink = url,
            imageKey = imageKey,
            imgUri = file.absolutePath,
            favorite = false,
            timeStamp = System.currentTimeMillis(),
            urlName = url,
            urlMemo = urlMemo,
            tag = tags
        )
    }

    /** buildGuestEntity */
    private fun buildGuestEntity(
        url: String,
        imageKey: String,
        urlMemo: String,
        tags: List<UserTags>,
        tagString: String = tags
            .mapNotNull { it.tag?.takeIf(String::isNotBlank) }
            .joinToString(",")
    ): UrlEntity {
        return UrlEntity(
            urlLink = url,
            imageKey = imageKey,
            favorite = false,
            timeStamp = System.currentTimeMillis(),
            urlName = url,
            urlMemo = urlMemo,
            tag = tagString
        )
    }

    private class DuplicateUrlException : IllegalStateException("이미 존재하는 URL입니다.")
}




