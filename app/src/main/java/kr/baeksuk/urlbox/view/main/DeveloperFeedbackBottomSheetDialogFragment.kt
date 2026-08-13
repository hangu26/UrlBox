package kr.baeksuk.urlbox.view.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kr.baeksuk.urlBox.R
import kotlinx.coroutines.launch
import android.content.Intent
import kr.baeksuk.urlbox.domain.feedback.CheckAdminAccessUseCase
import kr.baeksuk.urlbox.domain.feedback.ObserveFeedbackReportsV2UseCase
import kr.baeksuk.urlbox.util.adapter.RvFeedbackAdminAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.android.ext.android.inject
import android.widget.LinearLayout

class DeveloperFeedbackBottomSheetDialogFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "DeveloperFeedbackBottomSheet"
    }

    private var selectedType: String = ""

    // inject use-cases/repositories
    private val checkAdminAccessUseCase: CheckAdminAccessUseCase by inject()
    private val observeFeedbackReportsV2UseCase: ObserveFeedbackReportsV2UseCase by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_developer_feedback_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnClose = view.findViewById<ImageView>(R.id.btn_close)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group)
        val chipFeature = view.findViewById<Chip>(R.id.chip_feature)
        val chipBug = view.findViewById<Chip>(R.id.chip_bug)
        val chipPraise = view.findViewById<Chip>(R.id.chip_praise)
        val chipOther = view.findViewById<Chip>(R.id.chip_other)
        val etFeedback = view.findViewById<EditText>(R.id.et_feedback)
        val btnSend = view.findViewById<TextView>(R.id.btn_send)

        val rvAdmin = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_feedback_admin)
        val txAdminEmpty = view.findViewById<TextView>(R.id.tx_admin_empty)
        val llForm = view.findViewById<LinearLayout>(R.id.ll_feedback_form)

        btnClose.setOnClickListener { dismiss() }

        // prepare adapter for admin list
        val adminAdapter = RvFeedbackAdminAdapter(
            onItemClick = { report ->
                // show detail using same dialog as admin activity
                val ctx = requireContext()
                androidx.appcompat.app.AlertDialog.Builder(ctx)
                    .setTitle("피드백 상세")
                    .setMessage(buildString {
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
                    })
                    .setNegativeButton("닫기", null)
                    .setPositiveButton("상태 변경") { _, _ ->
                        // update status directly
                        // use FirebaseDatabase reference to change status
                        val nextStatus = when (report.status.uppercase()) {
                            "NEW" -> "READ"
                            "READ" -> "DONE"
                            else -> "NEW"
                        }
                        val ref = com.google.firebase.database.FirebaseDatabase.getInstance()
                            .reference.child("feedback_responses_02").child(report.id).child("status")
                        ref.setValue(nextStatus)
                    }
                    .show()
            },
            onStatusClick = { report ->
                // toggle status
                val nextStatus = when (report.status.uppercase()) {
                    "NEW" -> "READ"
                    "READ" -> "DONE"
                    else -> "NEW"
                }
                val ref = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .reference.child("feedback_responses_02").child(report.id).child("status")
                ref.setValue(nextStatus)
            }
        )

        rvAdmin.layoutManager = LinearLayoutManager(requireContext())
        rvAdmin.adapter = adminAdapter

        val chipClickListener = View.OnClickListener { v ->
            chipGroup.clearCheck()
            (v as Chip).isChecked = true
            selectedType = when (v.id) {
                R.id.chip_feature -> "기능제안"
                R.id.chip_bug -> "버그"
                R.id.chip_praise -> "칭찬"
                else -> "기타"
            }
            updateChipVisuals(chipGroup, v.id)
            updateSendState(btnSend, etFeedback.text.toString())
        }

        chipFeature.setOnClickListener(chipClickListener)
        chipBug.setOnClickListener(chipClickListener)
        chipPraise.setOnClickListener(chipClickListener)
        chipOther.setOnClickListener(chipClickListener)

        etFeedback.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateSendState(btnSend, s?.toString() ?: "")
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        btnSend.setOnClickListener {
            val text = etFeedback.text.toString().trim()
            if (text.isEmpty()) {
                Toast.makeText(requireContext(), "내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            // allow guest users: if not logged in, userEmail will be set to "guest" below and feedback will be sent anonymously


            // disable UI while sending
            btnSend.isEnabled = false
            btnSend.text = "전송중..."

            val userEmail = currentUser?.email ?: "guest"
            val dbRef = FirebaseDatabase.getInstance().reference.child("feedback_responses_02")

            val data = HashMap<String, Any?>()
            data["type"] = selectedType.ifBlank { "기타" }
            data["text"] = text
            data["userEmail"] = userEmail
            data["createdAt"] = ServerValue.TIMESTAMP

            dbRef.push().setValue(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "전송되었습니다. 감사합니다!", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
                .addOnFailureListener { ex ->
                    Toast.makeText(requireContext(), "전송 실패: ${ex.message}", Toast.LENGTH_SHORT).show()
                    btnSend.isEnabled = true
                    btnSend.text = "전송하기"
                }
        }

        // default: select first
        chipFeature.performClick()
        updateChipVisuals(chipGroup, chipFeature.id)

        // Check admin and show admin list if admin
        lifecycleScope.launchWhenStarted {
            val isAdmin = try { checkAdminAccessUseCase() } catch (_: Exception) { false }
            if (isAdmin) {
                // show admin recycler
                llForm.visibility = View.GONE
                rvAdmin.visibility = View.VISIBLE
                txAdminEmpty.visibility = View.GONE

                // subscribe to v2 feedbacks
                observeFeedbackReportsV2UseCase()
                    .collect { reports ->
                        if (reports.isEmpty()) {
                            rvAdmin.visibility = View.GONE
                            txAdminEmpty.visibility = View.VISIBLE
                        } else {
                            txAdminEmpty.visibility = View.GONE
                            rvAdmin.visibility = View.VISIBLE
                            adminAdapter.submitList(reports)
                        }
                    }
            } else {
                // ensure form visible for non-admin
                llForm.visibility = View.VISIBLE
                rvAdmin.visibility = View.GONE
                txAdminEmpty.visibility = View.GONE
            }
        }
    }

    private fun updateChipVisuals(chipGroup: ChipGroup, selectedId: Int) {
        val ctx = requireContext()
        val white = ContextCompat.getColor(ctx, android.R.color.white)
        val defaultText = ContextCompat.getColor(ctx, R.color.black)
        for (i in 0 until chipGroup.childCount) {
            val child = chipGroup.getChildAt(i)
            if (child is Chip) {
                if (child.id == selectedId) {
                    child.setChipBackgroundColorResource(R.color.main_color)
                    child.setTextColor(white)
                } else {
                    child.setChipBackgroundColorResource(android.R.color.white)
                    child.setTextColor(defaultText)
                }
            }
        }
    }

    private fun updateSendState(btnSend: TextView, text: String) {
        val enabled = text.isNotBlank() || selectedType.isNotBlank()
        btnSend.isEnabled = enabled
        btnSend.alpha = if (enabled) 1.0f else 0.5f
    }
}
