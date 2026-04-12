package kr.baeksuk.urlbox.util.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kr.baeksuk.urlbox.model.UserSession
import kotlin.text.get
import kotlin.text.set

private val Context.dataStore by preferencesDataStore(name = "user_session")

class UserSessionManager(private val context: Context) {

    companion object {

        private val KEY_HOME_SYNC_DONE = booleanPreferencesKey("home_sync_done")
        private val KEY_USER_ID = stringPreferencesKey("userId")
        private val KEY_USER_EMAIL = stringPreferencesKey("userEmail")
        private val KEY_USER_NAME = stringPreferencesKey("userName")
        private val KEY_USER_PROFILE = stringPreferencesKey("userProfile")
        private val KEY_AUTO_LOGIN = booleanPreferencesKey("auto_login")
        private val KEY_IS_FIRST = booleanPreferencesKey("isFirst")

        /** 튜토리얼 종료 변수 **/
        private val KEY_IS_CLEAR = booleanPreferencesKey("isClearIntent")
    }

    val userId: Flow<String?> = context.dataStore.data.map { pref ->
        pref[KEY_USER_ID]
    }

    val userEmail: Flow<String?> = context.dataStore.data.map { pref ->
        pref[KEY_USER_EMAIL] ?: ""
    }

    val userName : Flow<String?> = context.dataStore.data.map { pref ->
        pref[KEY_USER_NAME] ?: ""
    }

    val userProfile : Flow<String?> = context.dataStore.data.map { pref ->
        pref[KEY_USER_PROFILE]
    }

    val autoLogin: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[KEY_AUTO_LOGIN] ?: false
    }

    val isFirst: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[KEY_IS_FIRST] ?: true
    }

    val homeSyncDone: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[UserSessionManager.Companion.KEY_HOME_SYNC_DONE] ?: false
    }

    val isTutorialClear = context.dataStore.data
        .map { pref -> pref[KEY_IS_CLEAR] ?: false }

    val userSession: Flow<UserSession> = combine(
        userId,
        userEmail,
        userName,
        userProfile,
        autoLogin
    ) { id, email, name, profile, auto ->

        UserSession(
            userId = id,
            userEmail = email,
            userName = name,
            userProfile = profile,
            autoLogin = auto
        )
    }

    suspend fun setHomeSyncDone() {
        context.dataStore.edit { pref ->
            pref[UserSessionManager.Companion.KEY_HOME_SYNC_DONE] = true
        }
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

    suspend fun setFirst(){
        context.dataStore.edit { pref ->
            pref[KEY_IS_FIRST] = true
        }
    }

    suspend fun setTutorialClearDone() {
        context.dataStore.edit { pref ->
            pref[KEY_IS_CLEAR] = true
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