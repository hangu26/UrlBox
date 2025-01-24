package kr.baeksuk.urlbox.util.util.network

sealed class MyState {

    object Fetched : MyState()

    object Error : MyState()

}