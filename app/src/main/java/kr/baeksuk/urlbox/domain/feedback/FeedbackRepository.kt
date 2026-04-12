package kr.baeksuk.urlbox.domain.feedback

import kotlinx.coroutines.flow.Flow
import kr.baeksuk.urlbox.model.FeedbackReport

interface FeedbackRepository {
    fun observeFeedbackReports(): Flow<List<FeedbackReport>>
    suspend fun updateFeedbackStatus(reportId: String, status: String)
}

