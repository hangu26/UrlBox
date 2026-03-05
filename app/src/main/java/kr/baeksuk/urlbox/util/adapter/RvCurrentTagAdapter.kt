package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import kr.baeksuk.urlBox.databinding.ItemAddTagFirstBinding
import kr.baeksuk.urlBox.databinding.ItemTagInSetTagListBinding
import kr.baeksuk.urlbox.model.Tag
import kr.baeksuk.urlbox.util.util.AddTagDialogFragment
import kr.baeksuk.urlbox.util.util.OnTagDeleteSelectedListener

class RvCurrentTagAdapter(
    ctx: Context,
    private val fragmentManager: FragmentManager,
    private val onTagDeleteSelectedListener: OnTagDeleteSelectedListener
) : RecyclerView.Adapter<RvCurrentTagAdapter.MyViewHolder>() {

    private var tagList = listOf<Tag>()
    private val context = ctx

    @SuppressLint("NotifyDataSetChanged")
    fun setTagData(tagDataList: List<Tag>) {
        tagList = tagDataList.distinct().filter { it.tag!!.isNotEmpty() }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        val binding = if (viewType == 0) {
            ItemAddTagFirstBinding.inflate(inflater, parent, false)
        } else {
            ItemTagInSetTagListBinding.inflate(inflater, parent, false)
        }

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {

        if (position == 0) {
            holder.bindFirst()
        } else {
            holder.bind(tagList[position - 1])
        }
    }

    override fun getItemCount(): Int = tagList.size + 1

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) 0 else 1
    }

    inner class MyViewHolder(private val binding: ViewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /** 태그 추가 버튼 **/
        fun bindFirst() {
            if (binding is ItemAddTagFirstBinding) {

                binding.txTag.text = "태그 추가"

                binding.btnTag.setOnClickListener {

                    val dlg = AddTagDialogFragment()
                    dlg.show(fragmentManager, "AddTagDialog")

                }
            }
        }

        fun bind(tag: Tag) {
            if (binding is ItemTagInSetTagListBinding) {

                binding.txTag.text = tag.tag

                binding.iconClose.setOnClickListener {
                    onTagDeleteSelectedListener.onTagDeleteClicked(tag.tag!!)
                    Log.e("확인용", "${tag.tag}")
                }
            }
        }
    }
}