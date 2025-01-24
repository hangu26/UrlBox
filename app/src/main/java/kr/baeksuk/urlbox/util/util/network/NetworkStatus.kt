package kr.baeksuk.urlbox.util.util.network

sealed class NetworkStatus {

    object Available : NetworkStatus()

    object Unavailable : NetworkStatus()
}