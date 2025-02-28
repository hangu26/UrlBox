package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemTagInSetTagListBinding
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.util.util.OnTagDeleteSelectedListener
import kr.baeksuk.urlbox.util.util.OnTagSelectedListener

class RvCurrentTagAdapter(
    ctx: Context,
    act: Activity,
    private val onTagDeleteSelectedListener: OnTagDeleteSelectedListener
) : RecyclerView.Adapter<RvCurrentTagAdapter.MyViewHolder>() {

    private var tagList = listOf<Tag>()
    private val context = ctx

    @SuppressLint("NotifyDataSetChanged")
    fun setTagData(tagDataList: List<Tag>) {

        tagList = tagDataList.distinct().filter { it.tag!!.isNotEmpty() }

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvCurrentTagAdapter.MyViewHolder {
        val binding = ItemTagInSetTagListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RvCurrentTagAdapter.MyViewHolder, position: Int) {
        holder.bind(tagList[position])
    }

    override fun getItemCount(): Int = tagList.size

    inner class MyViewHolder(private val binding: ItemTagInSetTagListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val txTag = binding.txTag

        fun bind(tag: Tag) {

            txTag.text = tag.tag

            binding.iconClose.setOnClickListener {

                onTagDeleteSelectedListener.onTagDeleteClicked(txTag.text.toString())

            }
        }

    }
}