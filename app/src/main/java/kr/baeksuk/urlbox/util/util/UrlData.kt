package kr.baeksuk.urlbox.util.util

import kr.baeksuk.urlbox.model.Url

object UrlData {
    var urlList: List<Url>? = null

    fun clear(){
        urlList = null
    }

}