package kr.baeksuk.urlbox.domain.feedback

import kotlinx.coroutines.flow.Flow
import kr.baeksuk.urlbox.model.FeedbackReport

class ObserveFeedbackReportsUseCase(
    private val feedbackRepository: FeedbackRepository
) {
    operator fun invoke(): Flow<List<FeedbackReport>> = feedbackRepository.observeFeedbackReports()
}

