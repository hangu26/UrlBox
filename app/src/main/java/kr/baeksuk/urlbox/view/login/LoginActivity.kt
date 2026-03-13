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
import kotlinx.coroutines.withContext
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.UrlToLogin
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
    private var url = listOf<UrlToLogin>()
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

        // 카카오 로그인
        // 카카오계정으로 로그인 공통 callback 구성
        // 카카오톡으로 로그인 할 수 없어 카카오계정으로 로그인할 경우 사용됨

        observe()
        backPressedCallback.addCallbackFragment(this, MainActivity::class.java)

    }

    private fun observe() = lViewModel.let { vm ->
        val txMemo = resources.getString(R.string.tx_memo)

        vm.getGuestUrl().observe(this@LoginActivity) { it ->
            val directory = this.filesDir

            // 1. url 리스트 저장
            url = it.map { data ->
                UrlToLogin(
                    url = data.urlLink,
                    imageKey = data.imageKey,
                    favorite = data.favorite,
                    timeStamp = data.timeStamp,
                    urlName = data.urlName,
                    urlMemo = data.urlMemo,
                )
            }

            Log.e("로그인 시, 데이터 처리", url.toString())

            // 2. imgFileList 에 이미지 파일 리스트 저장
            imgFileList = it.map { data ->
                File(directory, "${data.imageKey}.png")
            }
            Log.e("이미지 저장", imgFileList.toString())
        }


        vm.btnCloseState.observe(this@LoginActivity) {

            if (it) {

                finishToMyPage(this)

            }

        }

        vm.btnGuestState.observe(this@LoginActivity) {

            if (it) {

                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                startActivityAnimation(intent, this@LoginActivity)
                finish()

            }

        }

        /**
        vm.googleLoginState.observe(this@LoginActivity) { isSuccess ->

        if (isSuccess){
        uploadData(isSuccess)
        restartApp(this@LoginActivity)
        }else{
        Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
        }
        }
         **/

        vm.loadingBar.observe(this) { show ->
            lBinding.loadingBarSkeleton.visibility = if (show) View.VISIBLE else View.GONE
        }

        vm.loginSelectLoading.observe(this@LoginActivity) { show ->
            lBinding.loadingBarLottie.visibility = if (show) View.VISIBLE else View.GONE
        }

        vm.kakaoLoginState.observe(this@LoginActivity) { isSuccess ->
            val pref = getSharedPreferences("User", Context.MODE_PRIVATE)

            if (isSuccess) {

                vm.userData.observe(this@LoginActivity){
                    uploadData(isUpload,it)
                }
                pref.edit().putInt("isFirst",1).apply()

                InitUrlDataCount.clear()

                UrlData.clear()

            } else {

                Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()

            }

        }

        vm.btnKakaoState.observe(this@LoginActivity) {

            if (it) {

                vm.kakaoLogin(this)

            }

        }

        vm.btnGoogleState.observe(this@LoginActivity) {

            if (it) {
                vm.setLoadingBar(true)
                signGoogle()
            }
        }

        vm.insertComplete.observe(this) { complete ->
            if (complete) {
                // 데이터 모두 동기화 완료 → MainActivity로 이동
                val intent = Intent(this, MainActivity::class.java)
                startActivityAnimation(intent, this)
                finish()
            }
        }

        vm.isDataSyncEnabled.observe(this@LoginActivity) {

            isUpload = it

            val message = if (isUpload) "계정에 동기화 중입니다.." else "로그인 중입니다"
            lBinding.txIsUploading.text = message

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

                withContext(Dispatchers.Main) {
                    lViewModel.changeLoadingBar()
                }

                handleSignIn(result)
            } catch (e: GetCredentialException) {
                Log.e("SignIn", "Error getting credentials", e)
                withContext(Dispatchers.Main) {
                    handleFailure(e)
                }
            }
        }
    }

    /** isUpload -> 계정에 동기화 체크 시, url 데이터 보내기 **/
    private fun handleSignIn(result: GetCredentialResponse) {

        val pref = getSharedPreferences("User", Context.MODE_PRIVATE)

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

                                    val firebaseUser = auth.currentUser
                                    val name = firebaseUser?.displayName
                                    val userId = User(
                                        userId = firebaseUser?.uid!!,
                                        userEmail = firebaseUser.email!!,
                                        userName = name!!,
                                        profileImage = firebaseUser.photoUrl.toString()
                                    )

                                    uploadData(isUpload, userId)
                                    InitUrlDataCount.clear()
                                    UrlData.clear()
                                    pref.edit().putInt("isFirst",1).apply()
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
        // 오류 메시지를 통해 사용자가 취소했는지 확인
        if (e.localizedMessage?.contains("cancelled") == true) {
            lViewModel.setLoadingBar(false)
        } else {
            lViewModel.setLoadingBar(false)
            Log.e("SignIn", "Credential error: ${e.localizedMessage}", e)
        }
    }

    private fun uploadData(isUpload: Boolean, user: User) {
        if (isUpload) {
            lViewModel.insertAllData(user, url, imgFileList)
        } else {
            lViewModel.insertUserId(user)
        }

        getSharedPreferences("User", Context.MODE_PRIVATE).edit()
            .apply {
                putString("userId", user.userId)
                putString("userEmail", user.userEmail)
                putString("userName", user.userName)
                putString("userProfile", user.profileImage)
                putBoolean("auto login", true)
                apply()
            }

    }

}