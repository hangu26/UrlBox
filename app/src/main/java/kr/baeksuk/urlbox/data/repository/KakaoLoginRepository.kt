package kr.baeksuk.urlbox.data.repository

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.User
import kr.baeksuk.urlbox.viewmodel.login.LoginViewModel

class KakaoLoginRepository(application: Application) : AndroidViewModel(application) {

    var userData : User = User(
        userName = "",
        userEmail = "",
        userId = ""
    )

    fun kakaoLogin(context: Context, viewModel: LoginViewModel, callback: (Boolean) -> Unit) : User {

        val loginCallback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e("카카오 로그인", "카카오계정으로 로그인 실패", error)
                callback(false)
            } else if (token != null) {
                Log.i("카카오 로그인", "카카오계정으로 로그인 성공 ${token.accessToken}")
                callback(true)

            }
        }

        // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(context)) {

            UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
                if (error != null) {

                    Log.e("카카오 로그인", "카카오톡으로 로그인 실패", error)

                    // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                    // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {

                        return@loginWithKakaoTalk

                    }

                    // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    UserApiClient.instance.loginWithKakaoAccount(context, callback = loginCallback)

                } else if (token != null) {

                    UserApiClient.instance.me { user, error ->

                        if (error != null) {
                            Log.e("카카오 회원정보 가져오기", "실패")
                        } else if (user != null) {
                            Log.e("카카오 회원정보 가져오기", "성공")

                            userData = User(
                                userId = user.id.toString(),
                                userEmail = user.kakaoAccount?.email.toString(),
                                userName = user.kakaoAccount?.profile?.nickname.toString(),
                                profileImage = user.kakaoAccount?.profile?.thumbnailImageUrl
                            )

                            viewModel.setKakaoUserData(userData)

                        }

                    }

                    Log.i("카카오 로그인", "카카오톡으로 로그인 성공 ${token.accessToken}")
                    Log.i("카카오 로그인", userData.userId)

                    callback(true)

                }
            }
        } else {

            UserApiClient.instance.loginWithKakaoAccount(context, callback = loginCallback)

        }

        return userData

    }



}