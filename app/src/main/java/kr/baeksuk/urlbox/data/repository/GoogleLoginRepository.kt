package kr.baeksuk.urlbox.data.repository

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.UrlData

class GoogleLoginRepository(application: Application) : AndroidViewModel(application) {

    fun signGoogle(context: Context, callback: (Boolean) -> Unit) {
        Log.d("SignIn", "signGoogle called")

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId(context.getString(R.string.default_web_client_id))
            .build()

        val credentialRequest: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.e("SignIn", "Requesting credentials")
                val result = credentialManager.getCredential(
                    request = credentialRequest,
                    context = context,
                )
                Log.e("SignIn", "Credentials received")
                handleSignIn(result) { isSuccess ->
                    callback(isSuccess)
                }
            } catch (e: GetCredentialException) {
                Log.e("SignIn", "Error getting credentials", e)
                handleFailure(e) { isFail ->
                    callback(false)
                }
            }
        }
    }

    /** isUpload -> 계정에 동기화 체크 시, url 데이터 보내기 **/
    private fun handleSignIn(result: GetCredentialResponse, callback: (Boolean) -> Unit) {
        Log.d("SignIn", "handleSignIn called")
        when (val credential = result.credential) {

            // Google ID Token 자격 증명 처리
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    Log.d("SignIn", "Processing Google ID Token")

                    try {
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken

                        // Firebase 인증을 위해 ID Token 사용
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
                            .addOnCompleteListener { task ->

                                if (task.isSuccessful) {
                                    InitUrlDataCount.clear()
                                    UrlData.clear()
                                    Log.e("SignIn", "Firebase 로그인 성공")
                                    callback(true)  // 로그인 성공 시 true 전달
                                } else {
                                    Log.e("SignIn", "Firebase 로그인 실패", task.exception)
                                    callback(false)
                                }
                            }

                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("SignIn", "유효하지 않은 구글 ID 토큰", e)
                        callback(false)
                    }
                } else {
                    Log.e("SignIn", "인식되지 않은 자격 증명 타입")
                    callback(false)
                }
            }

            else -> {
                Log.e("SignIn", "인식되지 않은 자격 증명 타입")
                callback(false)
            }
        }
    }

    private fun handleFailure(e: GetCredentialException, callback: (Boolean) -> Unit) {
        Log.e("SignIn", "Credential error: ${e.localizedMessage}", e)
        callback(false)  // 실패 시 false 전달
    }
}