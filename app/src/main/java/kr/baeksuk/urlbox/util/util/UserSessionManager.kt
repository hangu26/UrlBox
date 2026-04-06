package kr.baeksuk.urlbox.util.util

import android.content.Context
import androidx.datastore.preferences.core.stringPreferencesKey

class UserSessionManager(private val context : Context) {

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
    }

}