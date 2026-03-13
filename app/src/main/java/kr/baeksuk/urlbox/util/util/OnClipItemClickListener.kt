package kr.baeksuk.urlbox.util.util

import kr.baeksuk.urlbox.model.Url

interface OnClipItemClickListener {
    fun onDeleteClick(url: String, imageKey : String, position: Int)
}