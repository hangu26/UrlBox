package kr.baeksuk.urlbox.util.util

import androidx.recyclerview.widget.DiffUtil
import kr.baeksuk.urlbox.model.Url

class UrlDiffCallback(
    private val oldList: List<Url>,
    private val newList: List<Url>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
    override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].url == newList[newItemPosition].url
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
