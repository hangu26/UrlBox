package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemTagInSetTagListBinding
import kr.baeksuk.urlBox.databinding.ItemTagInTagListBinding
import kr.baeksuk.urlbox.model.Tag

class AllTagsAdapter(
) : RecyclerView.Adapter<AllTagsAdapter.MyViewHolder>() {

    private var tagList = listOf<Tag>()

    @SuppressLint("NotifyDataSetChanged")
    fun setTagData(tagDataList: List<Tag>) {
        tagList = tagDataList.distinct().filter { it.tag!!.isNotEmpty() }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        val binding = ItemTagInTagListBinding.inflate(inflater, parent, false)

        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(tagList[position])
    }

    override fun getItemCount(): Int = tagList.size

    inner class MyViewHolder(
        private val binding: ItemTagInTagListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tag: Tag) {

            binding.txTag.text = tag.tag

            binding.btnTag.setOnClickListener {

            }

        }
    }
}