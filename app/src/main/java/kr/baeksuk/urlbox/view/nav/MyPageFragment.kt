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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.FragmentMyPageBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.util.InitUrlDataCount
import kr.baeksuk.urlbox.util.util.StartActivityAnimation
import kr.baeksuk.urlbox.view.favorite.FavoritesActivity
import kr.baeksuk.urlbox.view.login.LoginActivity
import kr.baeksuk.urlbox.view.savedlink.SavedLinkActivity
import kr.baeksuk.urlbox.viewmodel.nav.MyPageViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import org.koin.android.ext.android.inject

class MyPageFragment : Fragment() {
    private lateinit var mBinding: FragmentMyPageBinding
    private val mViewModel: MyPageViewModel by inject()
    private var urlList = listOf<UrlEntity>()
    private val startActivityAnimation = StartActivityAnimation()
    private val credentialManager = activity?.let { CredentialManager.create(it) }
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        mBinding = FragmentMyPageBinding.inflate(inflater, container, false)
        mBinding.apply {
            viewmodel = mViewModel
        }
        auth = Firebase.auth

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



        if (autoLogin) {

            mBinding.txName.text = userName
            mBinding.txGuestEmail.text = userEmail
            mBinding.btnLogin.visibility = View.GONE
            mBinding.btnLogout.visibility = View.VISIBLE

            mViewModel.getUrlBackup()
                .observe(viewLifecycleOwner, Observer<List<UrlBackupEntity>> { url ->

                    mBinding.txLinkCount.text = url.size.toString()
                    mBinding.txFavoriteCount.text = url.filter { it.favorite }.size.toString()

                })

        } else {

            mBinding.txLinkCount.text = InitUrlDataCount.linkCount.toString()
            mBinding.txFavoriteCount.text = InitUrlDataCount.favorite.toString()

        }


    }

    private fun observe() = mViewModel.let { vm ->

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

                lifecycleScope.launch {
                    try {
                        credentialManager?.clearCredentialState(ClearCredentialStateRequest())

                        vm.deleteUserBackup()

                        restartApp(requireContext())

                    } catch (e: Exception) {
                        e.printStackTrace() // 로그 출력 (에러 확인용)
                    }
                }

            }
        }

    }

    private fun restartApp(context: Context) {
        val pref = requireContext().getSharedPreferences("User", Context.MODE_PRIVATE)

        pref.edit().clear().commit() // 동기적으로 적용


        // 앱 재시작
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            context.startActivity(intent)
            Runtime.getRuntime().exit(0)
        }
    }

}