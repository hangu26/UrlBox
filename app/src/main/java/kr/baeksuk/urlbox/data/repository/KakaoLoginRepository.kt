package kr.baeksuk.urlbox.data.repository

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.android.gms.tasks.RuntimeExecutionException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.User
import kr.baeksuk.urlbox.viewmodel.login.LoginViewModel

class KakaoLoginRepository(private val application: Application) : AndroidViewModel(application) {

    var userData: User = User(userName = "", userEmail = "", userId = "")

    // 카카오 로그인 처리
    fun kakaoLogin(context: Context, viewModel: LoginViewModel, callback: (Boolean) -> Unit): User {

        val loginCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("카카오 로그인", "카카오계정으로 로그인 실패", error)
                callback(false)
            } else if (token != null) {
                Log.i("카카오 로그인", "카카오계정으로 로그인 성공 ${token.accessToken}")
                handleKakaoLoginSuccess(token.accessToken, viewModel, callback)
            }
        }

        // 카카오톡으로 로그인 시도
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {
            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {
                    Log.e("카카오 로그인", "카카오톡으로 로그인 실패", error)
                    handleLoginError(error)
                } else if (token != null) {
                    handleKakaoLoginSuccess(token.accessToken, viewModel, callback)
                }
            }
        } else {
            // 카카오톡이 없다면 카카오 계정으로 로그인 시도
            loginWithKaKaoAccount(application, loginCallback)
        }

        return userData
    }

    // 카카오 계정으로 로그인
    private fun loginWithKaKaoAccount(context: Context, callback: (OAuthToken?, Throwable?) -> Unit) {
        UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
            callback(token, error)
        }
    }

    // 카카오 로그인 성공 처리
    private fun handleKakaoLoginSuccess(accessToken: String, viewModel: LoginViewModel, callback: (Boolean) -> Unit) {
        getCustomToken(accessToken) { success ->
            if (success) {
                fetchKakaoUserData(viewModel)
                callback(true)
            } else {
                callback(false)
            }
        }
    }

    // 카카오 로그인 실패 처리
    private fun handleLoginError(error: Throwable) {
        // 사용자 취소 등 의도적인 에러 처리 가능
        if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
            Log.i("카카오 로그인", "사용자가 로그인 취소")
        } else {
            Log.e("카카오 로그인", "카카오톡 로그인 실패", error)
        }
    }

    // 카카오 사용자 정보 가져오기
    private fun fetchKakaoUserData(viewModel: LoginViewModel) {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e("카카오 회원정보 가져오기", "실패", error)
            } else if (user != null) {
                Log.i("카카오 회원정보 가져오기", "성공")
                userData = User(
                    userId = user.id.toString(),
                    userEmail = user.kakaoAccount?.email.toString(),
                    userName = user.kakaoAccount?.profile?.nickname.toString(),
                    profileImage = user.kakaoAccount?.profile?.thumbnailImageUrl
                )
                viewModel.setKakaoUserData(userData)
            }
        }
    }

    // Firebase Functions에 배포한 kakaoCustomAuth 호출
    private fun getCustomToken(accessToken: String, callback: (Boolean) -> Unit) {
        val functions: FirebaseFunctions = Firebase.functions("asia-northeast3")
        val data = hashMapOf("token" to accessToken)

        functions.getHttpsCallable("kakaoCustomAuth")
            .call(data)
            .addOnCompleteListener { task ->
                try {
                    val result = task.result?.data as? Map<*, *>
                    val customToken = result?.get("custom_token") as? String

                    if (customToken != null) {
                        firebaseAuthWithKakao(customToken, callback)
                    } else {
                        Log.e("KakaoAuth", "Custom token is null")
                        callback(false)
                    }
                } catch (e: Exception) {
                    Log.e("KakaoAuth", "Error during callable function", e)
                    callback(false)
                }
            }
    }

    // Firebase Authentication으로 카카오 인증
    private fun firebaseAuthWithKakao(customToken: String, callback: (Boolean) -> Unit) {
        val auth = FirebaseAuth.getInstance()
        auth.signInWithCustomToken(customToken).addOnCompleteListener { result ->
            callback(result.isSuccessful)
        }
    }
}
