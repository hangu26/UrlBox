package kr.baeksuk.urlbox.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.Query
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kr.baeksuk.urlbox.domain.feedback.FeedbackRepository
import kr.baeksuk.urlbox.model.FeedbackReport

class FirebaseFeedbackRepository : FeedbackRepository {

    private companion object {
        private const val TAG = "FirebaseFeedbackRepo"
    }

    private val feedbackRef = FirebaseDatabase.getInstance()
        .reference
        .child("feedback_responses")

    override fun observeFeedbackReports(): Flow<List<FeedbackReport>> = callbackFlow {
        val query: Query = feedbackRef.orderByChild("createdAt")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val reports = snapshot.children.mapNotNull { child ->
                    child.getValue(FeedbackReport::class.java)?.copy(
                        id = child.key.orEmpty()
                    )
                }.sortedByDescending { it.createdAt }

                trySend(reports)
            }

            override fun onCancelled(error: DatabaseError) {
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    Log.i(TAG, "Feedback listener closed after permission was revoked")
                } else {
                    Log.w(TAG, "Feedback listener cancelled: ${error.message}")
                }

                close()
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    override suspend fun updateFeedbackStatus(reportId: String, status: String) {
        if (reportId.isBlank()) return
        feedbackRef.child(reportId).child("status").setValue(status).await()
    }
}
