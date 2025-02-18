package kr.baeksuk.urlbox.view.favorite

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityFavoritesBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.adapter.RvUrlAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.base.NavigationMenu
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.favorite.FavoriteViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import org.koin.android.ext.android.inject

class FavoritesActivity : BaseActivity() {

    private lateinit var fBinding: ActivityFavoritesBinding
    private val fViewModel: FavoriteViewModel by inject()
    private lateinit var adapter: RvUrlAdapter
    private val backPressedCallback = BackPressedCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding =
            DataBindingUtil.setContentView(this@FavoritesActivity, R.layout.activity_favorites)
        adapter = RvUrlAdapter(this, this) // adapter 초기화

        fBinding.apply {
            activity = this@FavoritesActivity
            viewmodel = fViewModel
            lifecycleOwner = this@FavoritesActivity
            rvUrl.layoutManager =
                GridLayoutManager(this@FavoritesActivity, 2, GridLayoutManager.VERTICAL, false)
            rvUrl.adapter = adapter // adapter 할당
        }

        backPressedCallback.addCallbackFragment(this, MainActivity::class.java)

        observe()

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() = fViewModel.let { vm ->

        val pref = getSharedPreferences("User", Context.MODE_PRIVATE)
        val autoLogin = pref.getBoolean("auto login", false)

        if (autoLogin) {

            vm.getUrlBackup().observe(this, Observer<List<UrlBackupEntity>> { url ->

                adapter.setUserFavoriteData(url)
                adapter.notifyDataSetChanged()

            })

        } else {

            vm.getGuestUrl().observe(this, Observer<List<UrlEntity>> { url ->

                adapter.setFavoriteData(url)
                adapter.notifyDataSetChanged()
            })

        }

        vm.btnCloseState.observe(this@FavoritesActivity) {
            if (it) {

                finishToMyPage(this@FavoritesActivity)

            }
        }

    }


}