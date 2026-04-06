package kr.baeksuk.urlbox.util.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_session")

class UserSessionManager(private val context: Context) {

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("userId")
        private val KEY_USER_EMAIL = stringPreferencesKey("userEmail")
        private val KEY_USER_NAME = stringPreferencesKey("userName")
        private val KEY_USER_PROFILE = stringPreferencesKey("userProfile")
        private val KEY_AUTO_LOGIN = booleanPreferencesKey("auto_login")
        private val KEY_IS_FIRST = booleanPreferencesKey("isFirst")
    }

    val userId: Flow<String?> = context.dataStore.data.map { pref ->
        pref[KEY_USER_ID]
    }

    val autoLogin: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[KEY_AUTO_LOGIN] ?: false
    }

    val isFirst: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[KEY_IS_FIRST] ?: true
    }

    suspend fun saveLogin(
        userId: String,
        userEmail: String,
        userName: String,
        userProfile: String,
        autoLogin: Boolean
    ) {
        context.dataStore.edit { pref ->
            pref[KEY_USER_ID] = userId
            pref[KEY_USER_EMAIL] = userEmail
            pref[KEY_USER_NAME] = userName
            pref[KEY_USER_PROFILE] = userProfile
            pref[KEY_AUTO_LOGIN] = autoLogin
        }
    }

    suspend fun setFirstDone() {
        context.dataStore.edit { pref ->
            pref[KEY_IS_FIRST] = false
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { pref ->
            pref.remove(KEY_USER_ID)
            pref.remove(KEY_USER_EMAIL)
            pref.remove(KEY_USER_NAME)
            pref.remove(KEY_USER_PROFILE)
            pref[KEY_AUTO_LOGIN] = false
        }
    }

}