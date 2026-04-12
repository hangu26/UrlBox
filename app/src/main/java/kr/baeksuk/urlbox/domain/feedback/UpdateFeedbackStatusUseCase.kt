package kr.baeksuk.urlbox.domain.feedback

class UpdateFeedbackStatusUseCase(
    private val feedbackRepository: FeedbackRepository
) {
    suspend operator fun invoke(reportId: String, status: String) {
        feedbackRepository.updateFeedbackStatus(reportId, status)
    }
}

