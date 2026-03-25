package kr.baeksuk.urlbox.view.nav

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.launch
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentMyPageBinding
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.StartActivityAnimation
import kr.baeksuk.urlbox.view.favorite.FavoritesActivity
import kr.baeksuk.urlbox.view.login.LoginActivity
import kr.baeksuk.urlbox.view.savedlink.SavedLinkActivity
import kr.baeksuk.urlbox.view.tag.TagActivity
import kr.baeksuk.urlbox.viewmodel.nav.MyPageViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import org.koin.android.ext.android.inject
import androidx.core.content.edit

class MyPageFragment : Fragment() {
    private lateinit var mBinding: FragmentMyPageBinding
    private val mViewModel: MyPageViewModel by inject()
    private var urlList = listOf<UrlEntity>()
    private val startActivityAnimation = StartActivityAnimation()
    private val credentialManager = activity?.let { CredentialManager.create(it) }
    private lateinit var auth: FirebaseAuth

    companion object {
        private var adView: AdView? = null  // 광고 뷰를 재사용
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mBinding = FragmentMyPageBinding.inflate(inflater, container, false)
        mBinding.apply {
            viewmodel = mViewModel
        }
        auth = Firebase.auth

        setupAdView()
        initView()
        observe()
        return mBinding.root
    }

    private fun initView() {

        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)
        val userId = pref.getString("userId", "")
        val userEmail = pref.getString("userEmail", "")
        val userName = pref.getString("userName", "")
        val userProfile = pref.getString("userProfile", "")

        if (autoLogin) {

            mBinding.txName.text = userName
            mBinding.txGuestEmail.text = userEmail
            mBinding.btnLogin.visibility = View.GONE
            mBinding.btnLogout.visibility = View.VISIBLE
            Glide.with(requireContext())
                .load(userProfile)
                .into(mBinding.imgProfile)

            mViewModel.getUrlBackup()
                .observe(viewLifecycleOwner, Observer<List<UrlBackupEntity>> { url ->

                    mBinding.txLinkCount.text = url.size.toString()
                    mBinding.txFavoriteCount.text = url.filter { it.favorite }.size.toString()

                })

            mViewModel.getUserTagBackup()
                .observe(viewLifecycleOwner, Observer<List<TagBackupEntity>> { tag ->

                    mBinding.txTagCount.text = tag.mapNotNull { it.tag }.distinct().size.toString()

                })

        } else {

            mBinding.txLinkCount.text = InitUrlDataCount.linkCount.toString()
            mBinding.txFavoriteCount.text = InitUrlDataCount.favorite.toString()

        }


    }

    private fun observe() = mViewModel.let { vm ->

        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)

        vm.btnTagState.observe(viewLifecycleOwner) {
            if (it) {

                if (autoLogin) {

                    val intent = Intent(context, TagActivity::class.java)
                    startActivityAnimation.startActivityAnimation(intent, requireContext())
                    activity?.finish()

                } else {

                    Toast.makeText(context, "로그인이 필요한 기능입니다.", Toast.LENGTH_SHORT).show()

                }


            }
        }

        vm.btnEditState.observe(viewLifecycleOwner) {
            if (it) {

                val intent = Intent(context, LoginActivity::class.java)
                startActivityAnimation.startActivityAnimation(intent, requireContext())
                activity?.finish()

            }
        }

        vm.btnSavedLinkState.observe(viewLifecycleOwner) {
            if (it) {

                val intent = Intent(context, SavedLinkActivity::class.java)
                startActivityAnimation.startActivityAnimation(intent, requireContext())
                activity?.finish()
            }
        }

        vm.btnFavoriteState.observe(viewLifecycleOwner) {
            if (it) {

                val intent = Intent(context, FavoritesActivity::class.java)
                startActivityAnimation.startActivityAnimation(intent, requireContext())
                activity?.finish()

            }
        }

        /** 로그아웃 시, 룸에 저장된 백업 데이터 삭제 -> 다른 계정으로 로그인 시 데이터 겹치는 문제 방지 **/
        vm.btnOutState.observe(viewLifecycleOwner) {
            if (it) {

                auth.signOut()

                kakaoLogout()

                pref.edit().putInt("isFirst", 0).apply()
                lifecycleScope.launch {
                    try {
                        credentialManager?.clearCredentialState(ClearCredentialStateRequest())

                        vm.deleteUserBackup()
                        vm.deleteUserTagBackup()
                        restartApp(requireContext())

                    } catch (e: Exception) {
                        e.printStackTrace() // 로그 출력 (에러 확인용)
                    }
                }

            }
        }

    }

    // 카카오 로그아웃 처리
    private fun kakaoLogout() {
        UserApiClient.instance.logout { error ->
            if (error != null) {
                Log.e("카카오 로그아웃", "카카오 로그아웃 실패", error)
            } else {
                Log.i("카카오 로그아웃", "카카오 로그아웃 성공")
            }
        }
    }

    private fun setupAdView() {
        if (adView == null) {  // 기존 광고 뷰가 없으면 새로 생성
            adView = AdView(requireContext()).apply {
//                adUnitId = "ca-app-pub-6498037779961709/3334253119" // 이건 프로덕션때 사용해야할 실제 id
                adUnitId = "ca-app-pub-3940256099942544/9214589741" // 테스트 id

                setAdSize(AdSize.LARGE_BANNER)
                loadAd(AdRequest.Builder().build())
            }
        } else {
            (adView?.parent as? ViewGroup)?.removeView(adView) // 기존 광고가 있으면 부모에서 제거 후 재사용
        }
        mBinding.adContainer.addView(adView) // 프래그먼트에 광고 추가
    }

    private fun restartApp(context: Context) {
        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)

        pref.edit(commit = true) {
            clear()
                .putInt("isClearIntent", 1)
        }

        // 앱 재시작
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        adView?.destroy()
    }

}