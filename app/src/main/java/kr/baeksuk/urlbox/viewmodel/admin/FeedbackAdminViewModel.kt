package kr.baeksuk.urlbox.viewmodel.admin

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kr.baeksuk.urlbox.domain.feedback.CheckAdminAccessUseCase
import kr.baeksuk.urlbox.domain.feedback.ObserveFeedbackReportsUseCase
import kr.baeksuk.urlbox.domain.feedback.UpdateFeedbackStatusUseCase
import kr.baeksuk.urlbox.model.FeedbackReport

sealed class AdminFeedbackUiState {
    object Loading : AdminFeedbackUiState()
    object Unauthorized : AdminFeedbackUiState()
    object Empty : AdminFeedbackUiState()
    data class Success(val reports: List<FeedbackReport>) : AdminFeedbackUiState()
    data class Error(val message: String) : AdminFeedbackUiState()
}

class FeedbackAdminViewModel(
    private val checkAdminAccessUseCase: CheckAdminAccessUseCase,
    private val observeFeedbackReportsUseCase: ObserveFeedbackReportsUseCase,
    private val updateFeedbackStatusUseCase: UpdateFeedbackStatusUseCase
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private var observeJob: Job? = null

    private val _uiState = MutableStateFlow<AdminFeedbackUiState>(AdminFeedbackUiState.Loading)
    val uiState: StateFlow<AdminFeedbackUiState> = _uiState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser == null) {
            observeJob?.cancel()
            observeJob = null
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
        loadFeedbackReports()
    }

    fun loadFeedbackReports() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            _uiState.value = AdminFeedbackUiState.Loading

            if (!checkAdminAccessUseCase()) {
                _uiState.value = AdminFeedbackUiState.Unauthorized
                return@launch
            }

            observeFeedbackReportsUseCase()
                .catch { throwable ->
                    if (throwable is CancellationException) throw throwable
                    _uiState.value = AdminFeedbackUiState.Error(
                        throwable.message ?: "피드백 데이터를 불러오지 못했습니다."
                    )
                }
                .collect { reports ->
                    _uiState.value = if (reports.isEmpty()) {
                        AdminFeedbackUiState.Empty
                    } else {
                        AdminFeedbackUiState.Success(reports)
                    }
                }
        }
    }

    fun updateStatus(report: FeedbackReport) {
        viewModelScope.launch {
            if (report.id.isBlank()) return@launch
            if (!checkAdminAccessUseCase()) {
                _uiState.value = AdminFeedbackUiState.Unauthorized
                return@launch
            }

            val nextStatus = when (report.status.uppercase()) {
                "NEW" -> "READ"
                "READ" -> "DONE"
                else -> "NEW"
            }

            updateFeedbackStatusUseCase(report.id, nextStatus)
        }
    }

    override fun onCleared() {
        auth.removeAuthStateListener(authStateListener)
        observeJob?.cancel()
        observeJob = null
        super.onCleared()
    }
}
