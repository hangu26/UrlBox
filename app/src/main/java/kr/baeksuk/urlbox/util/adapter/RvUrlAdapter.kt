package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.baeksuk.urlBox.databinding.ItemUrlListBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.GuestModeHandler
import kr.baeksuk.urlbox.model.LoggedInModeHandler
import kr.baeksuk.urlbox.model.ModeHandler
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.util.ImgUriListData
import kr.baeksuk.urlbox.view.urldetail.UrlDetailActivity
import java.io.File

class RvUrlAdapter(ctx: Context, act: Activity) :
    RecyclerView.Adapter<RvUrlAdapter.MyViewHolder>() {

    private val context = ctx
    private val activity = act
    private var urlList = listOf<Url>()
    private var imgUriList = listOf<String>()
    private var isBackup = false

    @SuppressLint("NotifyDataSetChanged")
    fun setGuestData(url: List<UrlEntity>) {
        urlList = url.map { urlEntity ->
            Url(
                url = urlEntity.urlLink,
                imageKey = urlEntity.imageKey,
                favorite = urlEntity.favorite,
                timeStamp = urlEntity.timeStamp,
                urlName = urlEntity.urlName
            )
        }

        notifyDataSetChanged()

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUserBackupData(url: List<UrlBackupEntity>, isLoginBackup: Boolean) {
        isBackup = isLoginBackup
        urlList = url.sortedByDescending { it.timeStamp }
            .map { urlBackupEntity ->
                Url(
                    url = urlBackupEntity.urlLink,
                    imageKey = urlBackupEntity.imageKey,
                    imgUri = urlBackupEntity.imgUri,
                    favorite = urlBackupEntity.favorite,
                    timeStamp = urlBackupEntity.timeStamp,
                    urlName = urlBackupEntity.urlName,
                    urlMemo = urlBackupEntity.urlMemo
                )

            }

        notifyDataSetChanged()

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setLoginData(urlDataList: List<Url>, imgUriList: List<String>, isLoginBackup: Boolean) {
        isBackup = isLoginBackup
        urlList = urlDataList
            .sortedByDescending { it.timeStamp } // timeStamp 기준 오름차순 정렬
            .map { url ->
                Url(
                    url = url.url,
                    imageKey = url.imageKey,
                    imgUri = url.imgUri,
                    favorite = url.favorite,
                    timeStamp = url.timeStamp,
                    urlName = url.urlName,
                    urlMemo = url.urlMemo
                )
            }

        this.imgUriList = urlDataList.sortedByDescending { it.timeStamp }
            .map {
                it.imgUri
            }

        ImgUriListData.imgUriListData = imgUriList

        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setFavoriteData(url: List<UrlEntity>) {
        urlList = url.map { urlEntity ->
            Url(
                url = urlEntity.urlLink,
                imageKey = urlEntity.imageKey,
                favorite = urlEntity.favorite,
                timeStamp = urlEntity.timeStamp
            )
        }.filter { it.favorite } // 필터링된 결과를 urlList에 다시 할당

        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUserFavoriteData(url: List<UrlBackupEntity>) {
        urlList = url.map { urlBackupEntity ->
            Url(
                url = urlBackupEntity.urlLink,
                imageKey = urlBackupEntity.imageKey,
                imgUri = urlBackupEntity.imgUri,
                favorite = urlBackupEntity.favorite,
                timeStamp = urlBackupEntity.timeStamp,
                urlName = urlBackupEntity.urlName
            )
        }.filter { it.favorite } // 필터링된 결과를 urlList에 다시 할당

        imgUriList = urlList.filter { it.favorite }.map {
            it.imgUri
        }

        notifyDataSetChanged()
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = ItemUrlListBinding.inflate(LayoutInflater.from(parent.context))
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RvUrlAdapter.MyViewHolder, position: Int) {
        holder.bind(urlList[position])
    }

    override fun getItemCount(): Int {
        return urlList.size
    }

    inner class MyViewHolder(binding: ItemUrlListBinding) : RecyclerView.ViewHolder(binding.root) {
        private var imageKey = ""
        private var isFavorite = false
        private var imgUri = ""
        private var urlLink = ""
        private val txUrl = binding.txUrl
        private val imgView = binding.imgThumbnail
        private val iconFavorite = binding.iconFavorite
        private val btnUrl = binding.btnUrl
        private var timeStamp = ""
        private var urlName = ""
        private var urlMemo = ""
        val pref = context.getSharedPreferences("User", Context.MODE_PRIVATE)
        private val autoLogin = pref.getBoolean("auto login", false)

        fun bind(url: Url) {
            urlLink = url.url
            txUrl.text = url.urlName
            imageKey = url.imageKey
            isFavorite = url.favorite
            imgUri = url.imgUri
            timeStamp = url.timeStamp.toString()
            urlName = url.urlName.toString()
            urlMemo = url.urlMemo.toString()

            if (isFavorite){
                iconFavorite.visibility = View.VISIBLE
            }else{
                iconFavorite.visibility = View.GONE
            }

            val modeHandler: ModeHandler = if (autoLogin) {
                LoggedInModeHandler(imgUriList, layoutPosition)
            } else {
                GuestModeHandler(imageKey, context)

            }

            /** 인터페이스를 통해 로그인 모드와 게스트 모드 로직 분리 구현 **/
            modeHandler.loadImage(url.imgUri, imgView, context, isBackup)

            /** isBackup -> 파이어베이스에서 받아온 데이터를 ui에 빠르게 처리하기 위해 Room에 백업 처리.  **/

            /**
            if (autoLogin) {

            if (isBackup) {
            Glide.with(context)
            .load(imgUri)
            .into(imgView)

            } else {
            Glide.with(context)
            .load(imgUriList[layoutPosition])
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(imgView)

            }


            } else {
            val directory = context.filesDir // UrlFragment에서 context 사용
            val filePath = "$directory/$imageKey.png"
            val file = File(filePath)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            if (file.exists()) {

            imgView.setImageBitmap(bitmap)

            } else {
            // 기본 이미지 설정 (이미지가 없는 경우)
            Log.d("파일 없음", "없음")
            }
            }
             **/

        }

        init {

            imgView.setOnClickListener {

                val options = ActivityOptions.makeSceneTransitionAnimation(
                    activity,
                    Pair.create(txUrl, "titleTran"),
                    Pair.create(imgView, "imageTran")
                )

                val intent = Intent(context, UrlDetailActivity::class.java)

                /** ModeHandler 인터페이스를 통해 로그인, 게스트 모드 로직 분리 **/

                val modeHandler: ModeHandler = if (autoLogin) {
                    LoggedInModeHandler(imgUriList, layoutPosition)
                } else {
                    GuestModeHandler(imageKey, context)
                }

                modeHandler.intentUrlToDetail(
                    intent,
                    urlLink,
                    imgUri,
                    isFavorite,
                    imageKey,
                    timeStamp,
                    urlName,
                    urlMemo
                )

                context.startActivity(intent, options.toBundle())
            }

            txUrl.setOnClickListener {

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlLink))
                context.startActivity(intent)
            }

        }


    }

}