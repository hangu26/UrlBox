package kr.baeksuk.urlbox.view.admin

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsControllerCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityFeedbackAdminBinding
import kr.baeksuk.urlbox.model.FeedbackReport
import kr.baeksuk.urlbox.util.adapter.RvFeedbackAdminAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.viewmodel.admin.AdminFeedbackUiState
import kr.baeksuk.urlbox.viewmodel.admin.FeedbackAdminViewModel
import org.koin.android.ext.android.inject
import kotlinx.coroutines.launch

class FeedbackAdminActivity : BaseActivity() {

    private lateinit var binding: ActivityFeedbackAdminBinding
    private val viewModel: FeedbackAdminViewModel by inject()
    private lateinit var adapter: RvFeedbackAdminAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_feedback_admin)
        applyStatusBarStyle()

        adapter = RvFeedbackAdminAdapter(
            onItemClick = { report -> showReportDetail(report) },
            onStatusClick = { report -> viewModel.updateStatus(report) }
        )

        binding.rvFeedbackAdmin.layoutManager = LinearLayoutManager(this)
        binding.rvFeedbackAdmin.adapter = adapter

        observeState()
    }

    private fun applyStatusBarStyle() {
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        AdminFeedbackUiState.Loading -> {
                            binding.progressFeedbackAdmin.isVisible = true
                            binding.rvFeedbackAdmin.isVisible = false
                            binding.txFeedbackAdminEmpty.isVisible = false
                        }

                        AdminFeedbackUiState.Unauthorized -> {
                            finish()
                        }

                        AdminFeedbackUiState.Empty -> {
                            binding.progressFeedbackAdmin.isVisible = false
                            binding.rvFeedbackAdmin.isVisible = false
                            binding.txFeedbackAdminEmpty.isVisible = true
                        }

                        is AdminFeedbackUiState.Success -> {
                            binding.progressFeedbackAdmin.isVisible = false
                            binding.txFeedbackAdminEmpty.isVisible = false
                            binding.rvFeedbackAdmin.isVisible = true
                            adapter.submitList(state.reports)
                        }

                        is AdminFeedbackUiState.Error -> {
                            binding.progressFeedbackAdmin.isVisible = false
                            binding.txFeedbackAdminEmpty.isVisible = true
                            binding.txFeedbackAdminEmpty.text = state.message
                            binding.rvFeedbackAdmin.isVisible = false
                        }
                    }
                }
            }
        }
    }

    private fun showReportDetail(report: FeedbackReport) {
        val message = buildString {
            appendLine("유형: ${report.type.ifBlank { "-" }}")
            appendLine("제목: ${report.title.ifBlank { "-" }}")
            appendLine()
            appendLine("내용")
            appendLine(report.content.ifBlank { "-" })
            appendLine()
            appendLine("이메일: ${report.email.ifBlank { "-" }}")
            appendLine("앱 버전: ${report.appVersion.ifBlank { "-" }}")
            appendLine("기기 정보: ${report.deviceInfo.ifBlank { "-" }}")
            appendLine("상태: ${report.status.ifBlank { "-" }}")
            appendLine("생성 시각: ${report.createdAt}")
            if (report.screenshotUrl.isNotBlank()) {
                appendLine()
                appendLine("스크린샷 URL: ${report.screenshotUrl}")
            }
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("피드백 상세")
            .setMessage(message)
            .setNegativeButton("닫기", null)
            .setPositiveButton("상태 변경") { _, _ ->
                viewModel.updateStatus(report)
            }

        if (report.screenshotUrl.isNotBlank()) {
            builder.setNeutralButton("스크린샷 열기") { _, _ ->
                openScreenshot(report.screenshotUrl)
            }
        }

        builder.show()
    }

    private fun openScreenshot(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        try {
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // 브라우저가 없으면 무시
        }
    }
}


