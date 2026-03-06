package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemTagInCaptureListBinding
import kr.baeksuk.urlBox.databinding.ItemTagInSetTagListBinding
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.util.util.OnTagDeleteSelectedListener
import kr.baeksuk.urlbox.util.util.OnTagSelectedListener

class RvTagInCaptureAdapter(
    private val onTagDeleteSelectedListener: OnTagDeleteSelectedListener
) : RecyclerView.Adapter<RvTagInCaptureAdapter.MyViewHolder>() {

    private var tagList = listOf<Tag>()

    @SuppressLint("NotifyDataSetChanged")
    fun setTagData(tagDataList: List<Tag>) {
        this.tagList = tagDataList.distinct().filter { it.tag?.isNotEmpty() == true }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        // ViewBinding 활용
        val binding = ItemTagInSetTagListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(tagList[position])
    }

    override fun getItemCount(): Int = tagList.size

    inner class MyViewHolder(private val binding: ItemTagInSetTagListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: Tag) {
            binding.txTag.text = tag.tag

            // 클릭 시 리스너 호출
            binding.iconClose.setOnClickListener {
                tag.tag?.let { tagName ->
                    onTagDeleteSelectedListener.onTagDeleteClicked(tagName)
                }
            }
        }
    }
}
