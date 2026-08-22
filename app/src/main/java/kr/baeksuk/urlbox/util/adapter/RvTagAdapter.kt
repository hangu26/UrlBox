package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ItemUrlTagBinding
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.util.util.OnTagFilterSelectedListener
import kr.baeksuk.urlbox.util.util.OnTagTouchHelperListener
import java.util.Collections

class RvTagAdapter(
    ctx: Context,
    act: Activity,
    private val filterListener: OnTagFilterSelectedListener,
    private val saveOrder: ((List<String>) -> Unit)? = null
) : RecyclerView.Adapter<RvTagAdapter.MyViewHolder>(), OnTagTouchHelperListener {

    private var tagList = listOf<Tag>()
    private val context = ctx
    private var selectedPosition: Int = RecyclerView.NO_POSITION
    private var isBackup = false

    @SuppressLint("NotifyDataSetChanged")
    fun setTagData(tagDataList: List<Tag>) {
        val fixedTag = Tag(tag = "전체")
        val favoriteTag = Tag(tag = "즐겨찾기")
        tagList = listOf(fixedTag, favoriteTag) + tagDataList
        tagList = tagList.sortedByDescending { it.timeStamp }.distinctBy { it.id ?: it.tag }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setTagBackupData(tag: List<TagBackupEntity>, isLoginBackup: Boolean) {
        isBackup = isLoginBackup
        val fixedTag = Tag(tag = "전체")
        val favoriteTag = Tag(tag = "즐겨찾기")

        val tagListFromBackup = tag.map {
            Tag(
                tag = it.tag,
                timeStamp = it.timeStamp,
                urlList = it.urlList,
                id = it.firebaseTagId
            )
        }

        val orderedTags = if (tag.isNotEmpty()) {
            val savedOrder = tag.firstNotNullOfOrNull { it.tagOrder }?.filter { it.isNotBlank() }
            if (!savedOrder.isNullOrEmpty()) {
                val indexMap = tagListFromBackup.associateBy { it.id ?: it.tag ?: "" }
                val ordered = mutableListOf<Tag>()
                savedOrder.forEach { savedId ->
                    val match = indexMap[savedId]
                    if (match != null) ordered.add(match)
                }
                val leftovers = tagListFromBackup.filterNot { item -> ordered.any { it.id == item.id || (it.id == null && it.tag == item.tag) } }
                ordered.addAll(leftovers)
                ordered
            } else {
                tagListFromBackup.sortedByDescending { it.timeStamp }.distinctBy { it.id ?: it.tag }
            }
        } else {
            emptyList()
        }

        tagList = listOf(fixedTag, favoriteTag) + orderedTags
        notifyDataSetChanged()
    }

    fun clearSelection() {
        val previousSelected = selectedPosition
        selectedPosition = RecyclerView.NO_POSITION
        if (previousSelected != RecyclerView.NO_POSITION) {
            notifyItemChanged(previousSelected)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvTagAdapter.MyViewHolder {
        val binding = ItemUrlTagBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RvTagAdapter.MyViewHolder, position: Int) {
        holder.bind(tagList[position], position)
    }

    override fun getItemCount(): Int = tagList.size

    inner class MyViewHolder(private val binding: ItemUrlTagBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: Tag, position: Int) {
            if (position == 0) {
                binding.txTag.text = "전체"
            } else if (position == 1) {
                binding.txTag.text = "즐겨찾기"
            } else {
                binding.txTag.text = tag.tag
            }

            if (selectedPosition == position) {
                binding.btnTag.setBackgroundResource(R.drawable.border_url_tag_clicked)
            } else {
                binding.btnTag.setBackgroundResource(R.drawable.border_url_tag)
            }

            binding.btnTag.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = position
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)

                when (position) {
                    0 -> filterListener.onTagFiltered(emptyList(), "전체")
                    1 -> filterListener.onTagFiltered(emptyList(), "즐겨찾기")
                    else -> tagList.getOrNull(selectedPosition)?.urlList?.let { urlList ->
                        filterListener.onTagFiltered(urlList, tag.tag.toString())
                    }
                }
            }
        }
    }

    override fun onItemMove(from: Int, to: Int) {
        if (from == 0 || to == 0) return
        if (from == 1 || to == 1) return

        val updatedList = tagList.toMutableList()
        Collections.swap(updatedList, from, to)
        tagList = updatedList

        val oldSelectedPosition = selectedPosition
        if (selectedPosition == from) {
            selectedPosition = to
        } else if (from < to && selectedPosition > from && selectedPosition <= to) {
            selectedPosition--
        } else if (from > to && selectedPosition < from && selectedPosition >= to) {
            selectedPosition++
        }

        notifyItemMoved(from, to)
        notifyItemChanged(from)
        notifyItemChanged(to)

        if (oldSelectedPosition != selectedPosition) {
            notifyItemChanged(oldSelectedPosition)
            notifyItemChanged(selectedPosition)
        }
    }

    override fun onDragEnd() {
        val finalOrder = tagList
            .filterIndexed { index, _ -> index > 1 }
            .mapNotNull { tag ->
                tag.id?.takeIf { id -> id.isNotBlank() }
                    ?: tag.timeStamp
                        ?.toLongOrNull()
                        ?.let { "tag$it" }
            }
            .filter { it.isNotBlank() }

        if (finalOrder.isEmpty()) {
            notifyDataSetChanged()
            return
        }

        saveOrder?.invoke(finalOrder)
        notifyDataSetChanged()
    }
}
