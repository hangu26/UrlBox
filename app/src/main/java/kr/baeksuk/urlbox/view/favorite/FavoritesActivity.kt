package kr.baeksuk.urlbox.view.favorite

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.core.app.ActivityOptionsCompat
import androidx.core.util.Pair
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityFavoritesBinding
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.adapter.RvUrlAdapter
import kr.baeksuk.urlbox.util.base.BaseActivity
import kr.baeksuk.urlbox.util.util.BackPressedCallback
import kr.baeksuk.urlbox.view.main.MainActivity
import kr.baeksuk.urlbox.viewmodel.favorite.FavoriteViewModel
import kr.baeksuk.urlbox.view.urldetail.UrlDetailActivity
import kr.baeksuk.urlbox.util.util.UserSessionManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import org.koin.android.ext.android.inject

class FavoritesActivity : BaseActivity() {

    private lateinit var fBinding: ActivityFavoritesBinding
    private val fViewModel: FavoriteViewModel by inject()
    private val sessionManager: UserSessionManager by inject()
    private lateinit var adapter: RvUrlAdapter
    private val backPressedCallback = BackPressedCallback(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fBinding =
            DataBindingUtil.setContentView(this@FavoritesActivity, R.layout.activity_favorites)
        adapter = RvUrlAdapter(
            this,
            onDetailClick = { url, txUrl, imgView -> openUrlDetail(url, txUrl, imgView) }
        )

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
        lifecycleScope.launch {
            val autoLogin = sessionManager.autoLogin.first()

            if (autoLogin) {
                vm.getUrlBackup().observe(this@FavoritesActivity) { url ->
                    adapter.setUserFavoriteData(url)
                    adapter.notifyDataSetChanged()
                }
            } else {
                vm.getGuestUrl().observe(this@FavoritesActivity) { url ->
                    adapter.setFavoriteData(url)
                    adapter.notifyDataSetChanged()
                }
            }
        }

        vm.btnCloseState.observe(this@FavoritesActivity) {
            if (it) {

                finishToMyPage(this@FavoritesActivity)

            }
        }

    }

    private fun openUrlDetail(url: Url, txUrl: View, imgView: View) {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            this,
            Pair.create(txUrl, "titleTran"),
            Pair.create(imgView, "imageTran")
        )

        val intent = Intent(this, UrlDetailActivity::class.java).apply {
            putExtra("title", url.url)
            putExtra("imgUri", url.imgUri)
            putExtra("imageKey", url.imageKey)
            putExtra("isFavorite", url.favorite)
            putExtra("timeStamp", url.timeStamp.toString())
            putExtra("urlName", url.urlName)
            putExtra("urlMemo", url.urlMemo)
        }

        startActivity(intent, options.toBundle())
    }


}