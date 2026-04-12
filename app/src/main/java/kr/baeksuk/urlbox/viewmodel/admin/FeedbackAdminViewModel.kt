package kr.baeksuk.urlbox.viewmodel.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    private val _uiState = MutableStateFlow<AdminFeedbackUiState>(AdminFeedbackUiState.Loading)
    val uiState: StateFlow<AdminFeedbackUiState> = _uiState.asStateFlow()

    init {
        loadFeedbackReports()
    }

    fun loadFeedbackReports() {
        viewModelScope.launch {
            _uiState.value = AdminFeedbackUiState.Loading

            if (!checkAdminAccessUseCase()) {
                _uiState.value = AdminFeedbackUiState.Unauthorized
                return@launch
            }

            observeFeedbackReportsUseCase().collect { reports ->
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
}

