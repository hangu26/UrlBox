package kr.baeksuk.urlbox.util.util

object InitUrlDataCount {
    var linkCount : Int = 0
    var favorite : Int = 0
    var tagCount : Int = 0

    fun clear(){
        linkCount = 0
        favorite = 0
        tagCount = 0
    }

}