package kr.baeksuk.urlbox.view.nav

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.se.omapi.Session
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.launch
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentMyPageBinding
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.StartActivityAnimation
import kr.baeksuk.urlbox.model.UserSession
import kr.baeksuk.urlbox.util.util.SessionCache
import kr.baeksuk.urlbox.view.favorite.FavoritesActivity
import kr.baeksuk.urlbox.view.login.LoginActivity
import kr.baeksuk.urlbox.view.savedlink.SavedLinkActivity
import kr.baeksuk.urlbox.view.tag.TagActivity
import kr.baeksuk.urlbox.viewmodel.nav.MyPageViewModel
import org.koin.android.ext.android.inject
import kr.baeksuk.urlbox.util.util.UserSessionManager

class MyPageFragment : Fragment() {
    private lateinit var mBinding: FragmentMyPageBinding
    private val mViewModel: MyPageViewModel by inject()

    private val sessionManager: UserSessionManager by inject()

    private var currentSession: UserSession? = null
    private var currentUrlBackup: List<UrlBackupEntity> = emptyList()
    private var currentTagBackup: List<TagBackupEntity> = emptyList()

    private val startActivityAnimation = StartActivityAnimation()
    private val credentialManager by lazy { CredentialManager.create(requireActivity()) }
    private lateinit var auth: FirebaseAuth

    companion object {
        private var adView: AdView? = null  // 광고 뷰를 재사용
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        mBinding = FragmentMyPageBinding.inflate(inflater, container, false)
        mBinding.apply {
            viewmodel = mViewModel
            lifecycleOwner = viewLifecycleOwner
            fragment = this@MyPageFragment
        }
        auth = Firebase.auth

        setupAdView()
        initView()
        observeSession()
        observeBackupData()
        observe()
        return mBinding.root
    }

    private fun initView() {
        val cached = SessionCache.current

        when {
            cached == null -> showLoadingState()
            cached.autoLogin -> renderLoggedInUi(cached)
            else -> renderLoggedOutUi()
        }
    }

    private fun showLoadingState() {
        mBinding.btnLogin.visibility = View.INVISIBLE
        mBinding.btnLogout.visibility = View.INVISIBLE
        mBinding.txName.text = ""
        mBinding.txGuestEmail.text = ""
        mBinding.imgProfile.setImageResource(R.drawable.account_circle_24px)

        mBinding.txLinkCount.text = ""
        mBinding.txFavoriteCount.text = ""
        mBinding.txTagCount.text = ""
    }

    private fun observeSession() {
        viewLifecycleOwner.lifecycleScope.launch {
            sessionManager.userSession.collect { session ->
                currentSession = session
                SessionCache.current = session

                if (session.autoLogin) {
                    renderLoggedInUi(session)
                    renderBackupCounts()
                } else {
                    renderLoggedOutUi()
                }
            }
        }
    }

    private fun observeBackupData() {
        mViewModel.getUrlBackup().observe(viewLifecycleOwner) { url ->
            currentUrlBackup = url
            renderBackupCounts()
        }

        mViewModel.getUserTagBackup().observe(viewLifecycleOwner) { tag ->
            currentTagBackup = tag
            renderBackupCounts()
        }
    }

    private fun renderLoggedInUi(session: UserSession) {
        mBinding.txName.text = session.userName
        mBinding.txGuestEmail.text = session.userEmail
        mBinding.btnLogin.visibility = View.GONE
        mBinding.btnLogout.visibility = View.VISIBLE

        Glide.with(requireContext())
            .load(session.userProfile)
            .into(mBinding.imgProfile)
    }

    private fun renderLoggedOutUi() {
        mBinding.btnLogin.visibility = View.VISIBLE
        mBinding.btnLogout.visibility = View.GONE
        mBinding.imgProfile.setImageResource(R.drawable.account_circle_24px)

        mBinding.txLinkCount.text = InitUrlDataCount.linkCount.toString()
        mBinding.txFavoriteCount.text = InitUrlDataCount.favorite.toString()
        mBinding.txTagCount.text = "0"
    }

    private fun renderBackupCounts() {
        if (currentSession?.autoLogin != true) return

        mBinding.txLinkCount.text = currentUrlBackup.size.toString()
        mBinding.txFavoriteCount.text = currentUrlBackup.filter { it.favorite }.size.toString()
        val distinctTags = mutableSetOf<String>()
        currentTagBackup.forEach { distinctTags.add(it.tag) }
        mBinding.txTagCount.text = distinctTags.size.toString()
    }

    private fun observe() = mViewModel.let { vm ->

        vm.btnTagState.observe(viewLifecycleOwner) { clicked ->

            if (!clicked) return@observe

            viewLifecycleOwner.lifecycleScope.launch {

                if (vm.isLoggedIn()) {

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
        vm.btnOutState.observe(viewLifecycleOwner) { clicked ->

            if (!clicked) return@observe

            auth.signOut()

            kakaoLogout()

            viewLifecycleOwner.lifecycleScope.launch {

                try {
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    sessionManager.clearSession()
                    SessionCache.current = null
                    vm.deleteUserBackup()
                    vm.deleteUserTagBackup()
                    vm.deletePassword()
                    restartApp(requireContext())

                } catch (e: Exception) {
                    e.printStackTrace() // 로그 출력 (에러 확인용)
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
                adUnitId = "ca-app-pub-6498037779961709/6861235089" // 프로덕션용 id

                setAdSize(AdSize.LARGE_BANNER)
                loadAd(AdRequest.Builder().build())
            }
        } else {
            (adView?.parent as? ViewGroup)?.removeView(adView) // 기존 광고가 있으면 부모에서 제거 후 재사용
        }
        mBinding.adContainer.addView(adView) // 프래그먼트에 광고 추가
    }

    private fun restartApp(context: Context) {

        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        adView?.destroy()
        adView = null
        super.onDestroyView()
    }

}