package kr.baeksuk.urlbox.util.util

/** TagActivity 내에서 긴 클릭 시, 삭제 다이얼로그 출력 인터페이스 **/
interface OnTagLongTouchListener {

    fun onTagLongTouched(tag : String)

}