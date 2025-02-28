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
    private val filterListener: OnTagFilterSelectedListener
) : RecyclerView.Adapter<RvTagAdapter.MyViewHolder>(), OnTagTouchHelperListener {

    private var tagList = listOf<Tag>()
    private val context = ctx
    private var selectedPosition: Int = RecyclerView.NO_POSITION // 현재 선택된 버튼 위치 저장
    private var isBackup = false

    @SuppressLint("NotifyDataSetChanged")
    fun setTagData(tagDataList: List<Tag>) {

        val fixedTag = Tag(tag = "전체")
        // "전체"로 고정된 Tag 생성
        tagList = listOf(fixedTag) + tagDataList  // 첫 번째 아이템은 "전체"로 추가하고 나머지 데이터 추가

        tagList.sortedByDescending { it.timeStamp }.distinct()

        notifyDataSetChanged()
    }

    /** 태그 데이터를 룸에 저장해서 앱이 실행됐을 때 빠르게 데이터를 ui에 보여줌 **/
    @SuppressLint("NotifyDataSetChanged")
    fun setTagBackupData(tag: List<TagBackupEntity>, isLoginBackup: Boolean) {

        isBackup = isLoginBackup

        val fixedTag = Tag(tag = "전체")

        tagList = listOf(fixedTag) + tag.map {
            Tag(
                tag = it.tag,
                timeStamp = it.timeStamp,
                urlList = it.urlList
            )
        }.sortedByDescending { it.timeStamp }.distinct()

        notifyDataSetChanged()

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
            } else {
                binding.txTag.text = tag.tag
            }
            // 선택된 아이템이면 클릭된 배경, 아니면 기본 배경 적용
            if (selectedPosition == position) {
                binding.btnTag.setBackgroundResource(R.drawable.border_url_tag_clicked)
            } else {
                binding.btnTag.setBackgroundResource(R.drawable.border_url_tag)
            }

            binding.btnTag.setOnClickListener {
                val previousSelected = selectedPosition
                selectedPosition = position

                // 이전 선택된 버튼과 현재 선택된 버튼만 UI 갱신
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)

                // 선택된 태그 필터링 실행
//                filterListener.onTagFiltered(tag.tag.toString())

                if (position == 0) {
                    // "전체" 버튼 클릭 시 "전체" 텍스트를 강제로 전달
                    filterListener.onTagFiltered(emptyList(), "전체")
                } else {
                    // 선택된 태그의 URL 리스트 전달
                    tagList[selectedPosition].urlList?.let { urlList ->
                        filterListener.onTagFiltered(urlList, tag.tag.toString())
                    }
                }
            }
        }
    }

    override fun onItemMove(from: Int, to: Int) {
        if (from == 0 || to == 0) return // "전체" 태그는 이동 불가

        val updatedList = tagList.toMutableList()
        Collections.swap(updatedList, from, to)
        tagList = updatedList

        notifyItemMoved(from, to)
    }


}
