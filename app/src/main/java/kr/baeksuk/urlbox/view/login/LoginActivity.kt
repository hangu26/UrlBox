package kr.baeksuk.urlbox.view.login

import android.content.Context
import android.content.Intent
import androidx.credentials.CredentialManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.credentials.CustomCredential
import androidx.databinding.DataBindingUtil
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityLoginBinding
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.login.LoginViewModel
import org.koin.android.ext.android.inject
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.User
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.UrlData
import java.io.File

class LoginActivity : BaseActivity() {

    private lateinit var lBinding: ActivityLoginBinding
    private val lViewModel: LoginViewModel by inject()
    private val backPressedCallback = BackPressedCallback(this)
    private lateinit var auth: FirebaseAuth
    private var isUpload = false
    private var url = listOf<Url>()
    private var imgFileList = listOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lBinding = DataBindingUtil.setContentView(this@LoginActivity, R.layout.activity_login)
        lBinding.apply {
            activity = this@LoginActivity
            viewmodel = lViewModel
            lifecycleOwner = this@LoginActivity
        }
        auth = Firebase.auth

        observe()
        backPressedCallback.addCallbackFragment(this, MainActivity::class.java)

    }

    private fun observe() = lViewModel.let { vm ->

        vm.getGuestUrl().observe(this@LoginActivity) { it ->
            val directory = this.filesDir

            // 1. url 리스트 저장
            url = it.map { data ->
                Url(
                    url = data.urlLink,
                    imageKey = data.imageKey,
                    favorite = data.favorite,
                    timeStamp = data.timeStamp
                )
            }

            // 2. imgFileList 에 이미지 파일 리스트 저장
            imgFileList = it.map { data ->
                File(directory, "${data.imageKey}.png")
            }
            Log.e("이미지 저장", imgFileList.toString())
        }


        vm.btnCloseState.observe(this@LoginActivity) {

            if (it) {

                finishToMyPage()

            }

        }

        vm.btnGoogleState.observe(this@LoginActivity) {

            signGoogle()

        }

        vm.insertComplete.observe(this@LoginActivity) {
            if (it) {

                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivityAnimation(intent, this@LoginActivity)
                finish()

            }
        }

        vm.isDataSyncEnabled.observe(this@LoginActivity) {

            isUpload = it

        }

    }

    private fun signGoogle() {
        Log.d("SignIn", "signGoogle called")

        val credentialManager = CredentialManager.create(this)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setServerClientId(this.getString(R.string.default_web_client_id))
            .build()

        val credentialRequest: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("SignIn", "Requesting credentials")
                val result = credentialManager.getCredential(
                    request = credentialRequest,
                    context = this@LoginActivity,
                )
                Log.d("SignIn", "Credentials received")
                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Log.e("SignIn", "Error getting credentials", e)
                handleFailure(e)
            }
        }
    }

    /** isUpload -> 계정에 동기화 체크 시, url 데이터 보내기 **/
    private fun handleSignIn(result: GetCredentialResponse) {
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

                                    uploadData(isUpload)
                                    InitUrlDataCount.clear()
                                    UrlData.clear()

                                    Log.e("SignIn", "Firebase 로그인 성공")

                                } else {
                                    // 로그인 실패 처리
                                    Log.e("SignIn", "Firebase 로그인 실패", task.exception)
                                }

                            }

                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("SignIn", "유효하지 않은 구글 ID 토큰", e)
                    }
                } else {
                    Log.e("SignIn", "인식되지 않은 자격 증명 타입")
                }
            }

            else -> {
                Log.e("SignIn", "인식되지 않은 자격 증명 타입")
            }
        }
    }

    private fun handleFailure(e: GetCredentialException) {

        Log.e("SignIn", "Credential error: ${e.localizedMessage}", e)
        // 오류에 따라 사용자에게 알리거나 추가 처리를 할 수 있습니다.
    }


    private fun finishToMyPage() {
        val intent = Intent(this, MainActivity::class.java)
            .putExtra("TARGET_FRAGMENT", "MyPage")
        startActivityAnimation(intent, this@LoginActivity)
        finish()
    }

    private fun uploadData(isUpload: Boolean) {

        val firebaseUser = auth.currentUser
        val name = firebaseUser?.displayName
        val userId = User(
            userId = firebaseUser?.uid!!,
            userEmail = firebaseUser.email!!,
            userName = name!!,
        )

        if (isUpload) {

            lViewModel.insertAllData(userId = userId, url = url, imgFileList = imgFileList)

        } else {

            lViewModel.insertUserId(userId = userId)

        }


        getSharedPreferences("User", Context.MODE_PRIVATE).edit()
            .apply {
                putString("userId", firebaseUser.uid)
                putString("userEmail", firebaseUser.email)
                putString("userName", name)
                putBoolean("auto login", true)
                apply()
            }

    }

}