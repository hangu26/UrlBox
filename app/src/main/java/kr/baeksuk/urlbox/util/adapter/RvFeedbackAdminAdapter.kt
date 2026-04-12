package kr.baeksuk.urlbox.util.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemFeedbackAdminBinding
import kr.baeksuk.urlbox.model.FeedbackReport

class RvFeedbackAdminAdapter(
    private val onItemClick: (FeedbackReport) -> Unit,
    private val onStatusClick: (FeedbackReport) -> Unit
) : ListAdapter<FeedbackReport, RvFeedbackAdminAdapter.FeedbackViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeedbackViewHolder {
        val binding = ItemFeedbackAdminBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FeedbackViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeedbackViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FeedbackViewHolder(
        private val binding: ItemFeedbackAdminBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(report: FeedbackReport) {
            binding.report = report
            binding.btnFeedbackStatus.text = when (report.status.uppercase()) {
                "NEW" -> "읽음으로 변경"
                "READ" -> "완료로 변경"
                else -> "새로 받음"
            }
            binding.btnFeedbackStatus.setOnClickListener { onStatusClick(report) }
            binding.root.setOnClickListener { onItemClick(report) }
            binding.root.isClickable = true
            binding.executePendingBindings()
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<FeedbackReport>() {
        override fun areItemsTheSame(oldItem: FeedbackReport, newItem: FeedbackReport): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FeedbackReport, newItem: FeedbackReport): Boolean {
            return oldItem == newItem
        }
    }
}

