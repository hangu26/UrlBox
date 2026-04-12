package kr.baeksuk.urlbox.domain.feedback

class CheckAdminAccessUseCase(
    private val adminAccessRepository: AdminAccessRepository
) {
    suspend operator fun invoke(): Boolean = adminAccessRepository.isAdminUser()
}

