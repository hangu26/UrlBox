package kr.baeksuk.urlbox.util.util

import kr.baeksuk.urlbox.model.Url

object UrlData {
    var urlList: List<Url>? = null
    var selectedPosition: Int = 0

    fun clear(){
        urlList = null
        selectedPosition = 0
    }

}