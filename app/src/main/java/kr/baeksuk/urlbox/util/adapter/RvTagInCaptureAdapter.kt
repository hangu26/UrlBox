package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemTagInCaptureListBinding
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.util.util.OnTagSelectedListener

class RvTagInCaptureAdapter(
    ctx: Context,
    act: Activity,
    private val tagClickedListener: OnTagSelectedListener
) : RecyclerView.Adapter<RvTagInCaptureAdapter.MyViewHolder>() {

    private var tagList = listOf<Tag>()
    private val context = ctx
    private var selectedPosition: Int = RecyclerView.NO_POSITION // 현재 선택된 버튼 위치 저장

    @SuppressLint("NotifyDataSetChanged")
    fun setTagData(tagDataList: List<Tag>) {

        tagList = tagDataList

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvTagInCaptureAdapter.MyViewHolder {
        val binding = ItemTagInCaptureListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RvTagInCaptureAdapter.MyViewHolder, position: Int) {
        holder.bind(tagList[position])
    }

    override fun getItemCount(): Int = tagList.size

    inner class MyViewHolder(private val binding: ItemTagInCaptureListBinding) :
        RecyclerView.ViewHolder(binding.root) {

            private val txTag = binding.txTag

        fun bind(tag: Tag) {

            txTag.text = tag.tag

            binding.btnTag.setOnClickListener {

                tagClickedListener.onTagSelected(txTag.text.toString())

            }
        }
    }
}
