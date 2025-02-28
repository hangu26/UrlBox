package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemTagInTagListBinding
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.util.util.OnTagLongTouchListener
import kr.baeksuk.urlbox.util.util.OnTagSelectedListener

class RvTagInTagAdapter(
    ctx: Context,
    act: Activity,
    tagTouchListener : OnTagLongTouchListener
) : RecyclerView.Adapter<RvTagInTagAdapter.MyViewHolder>() {

    private var tagList = listOf<Tag>()
    private val context = ctx
    private var selectedPosition: Int = RecyclerView.NO_POSITION // 현재 선택된 버튼 위치 저장
    private val longTouchListener = tagTouchListener

    @SuppressLint("NotifyDataSetChanged")
    fun setUserTagData(tagDataList: List<TagBackupEntity>) {

        tagList = tagDataList.map { tagBackupEntity ->
            Tag(
                tag = tagBackupEntity.tag,
                timeStamp = tagBackupEntity.timeStamp
            )
        }.sortedByDescending { it.timeStamp }.distinct()

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvTagInTagAdapter.MyViewHolder {
        val binding = ItemTagInTagListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RvTagInTagAdapter.MyViewHolder, position: Int) {
        holder.bind(tagList[position])
    }

    override fun getItemCount(): Int = tagList.size

    inner class MyViewHolder(private val binding: ItemTagInTagListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val txTag = binding.txTag

        fun bind(tag: Tag) {

            txTag.text = tag.tag

            binding.btnTag.setOnClickListener {



            }

            binding.btnTag.setOnLongClickListener {

                longTouchListener.onTagLongTouched(txTag.text.toString())

                return@setOnLongClickListener true

            }
        }
    }
}
