package kr.baeksuk.urlbox.view.favorite

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ActivityFavoritesBinding
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.util.adapter.RvUrlAdapter
import kr.baeksuk.urlbox.viewmodel.favorite.FavoriteViewModel
import kr.baeksuk.urlbox.viewmodel.nav.UrlDataViewModel
import org.koin.android.ext.android.inject

class FavoritesActivity : AppCompatActivity() {

    private lateinit var fBinding: ActivityFavoritesBinding
    private val fViewModel: FavoriteViewModel by inject()
    private lateinit var adapter: RvUrlAdapter
    private var autoLogin = false

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

        observe()

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun observe() = fViewModel.let { vm ->

        if (autoLogin) {

        } else {

            vm.getGuestUrl().observe(this, Observer<List<UrlEntity>> { url ->

                adapter.setFavoriteData(url)
                adapter.notifyDataSetChanged()
            })

        }

        vm.btnCloseState.observe(this@FavoritesActivity){
            if (it){

                finish()

            }
        }

    }

}