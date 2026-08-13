package kr.baeksuk.urlbox.domain.feedback

import kotlinx.coroutines.flow.Flow
import kr.baeksuk.urlbox.model.FeedbackReport
import kr.baeksuk.urlbox.data.repository.FirebaseFeedbackV2Repository

class ObserveFeedbackReportsV2UseCase(
    private val feedbackRepository: FirebaseFeedbackV2Repository
) {
    operator fun invoke(): Flow<List<FeedbackReport>> = feedbackRepository.observeFeedbackReports()
}
