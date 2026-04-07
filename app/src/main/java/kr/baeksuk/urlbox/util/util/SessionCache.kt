package kr.baeksuk.urlbox.util.util

import kr.baeksuk.urlbox.model.UserSession

object SessionCache {
    var current: UserSession? = null

    fun isLoggedIn(): Boolean {
        return current?.autoLogin == true
    }
}