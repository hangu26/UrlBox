package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemUrlListBinding
import kr.baeksuk.urlbox.data.local.entity.TagBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.GuestModeHandler
import kr.baeksuk.urlbox.model.LoggedInModeHandler
import kr.baeksuk.urlbox.model.ModeHandler
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.UserTags
import kr.baeksuk.urlbox.util.util.ImgUriListData
import kr.baeksuk.urlbox.util.util.UrlDiffCallback
import kr.baeksuk.urlbox.view.urldetail.UrlDetailActivity

class RvUrlAdapter(ctx: Context, act: Activity) :
    RecyclerView.Adapter<RvUrlAdapter.MyViewHolder>() {

    private val context = ctx
    private val activity = act
    private var urlList = listOf<Url>()
    private var imgUriList = listOf<String>()
    private var tagFilteredList = listOf<Url>()
    private var isBackup = false

    // 필터링된 리스트만 갱신
    /**
    @SuppressLint("NotifyDataSetChanged")
    fun filterByTag(tagUrl: List<String>, tag : String) {

    tagFilteredList = if (tag == "전체") {
    urlList // "전체"가 선택되면 모든 데이터를 표시
    } else {
    urlList.filter { it.url in tagUrl } // 선택된 태그에 해당하는 데이터만 필터링
    }


    notifyDataSetChanged() // RecyclerView 갱신
    }
     **/

    fun filterByTag(tagUrl: List<String>, tag: String, recyclerview : RecyclerView) {
        val newList = if (tag == "전체") {
            urlList
        } else {
            urlList.filter { it.url in tagUrl }
        }

        val diffCallback = UrlDiffCallback(tagFilteredList, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        tagFilteredList = newList
        diffResult.dispatchUpdatesTo(this) // 애니메이션 적용

        recyclerview.scheduleLayoutAnimation() // 추가된 코드 (레이아웃 애니메이션 실행)
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setGuestData(url: List<UrlEntity>) {
        urlList = url.map { urlEntity ->
            Url(
                url = urlEntity.urlLink,
                imageKey = urlEntity.imageKey,
                favorite = urlEntity.favorite,
                timeStamp = urlEntity.timeStamp,
                urlName = urlEntity.urlName,
                urlMemo = urlEntity.urlMemo,
                tag = listOf(UserTags(tag = urlEntity.tag, timeStamp = urlEntity.timeStamp))
            )
        }
        tagFilteredList = urlList
        notifyDataSetChanged()

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUserBackupData(url: List<UrlBackupEntity>, isLoginBackup: Boolean) {
        isBackup = isLoginBackup
        val newUrlList = url.sortedByDescending { it.timeStamp }
            .map { urlBackupEntity ->
                Url(
                    url = urlBackupEntity.urlLink,
                    imageKey = urlBackupEntity.imageKey,
                    imgUri = urlBackupEntity.imgUri,
                    favorite = urlBackupEntity.favorite,
                    timeStamp = urlBackupEntity.timeStamp,
                    urlName = urlBackupEntity.urlName,
                    urlMemo = urlBackupEntity.urlMemo,
                    tag = urlBackupEntity.tag
                )
            }

        // 데이터가 변경되었을 때만 notifyDataSetChanged() 호출
        if (newUrlList != urlList) {
            urlList = newUrlList
            tagFilteredList = newUrlList
            notifyDataSetChanged()
        }
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
                    urlMemo = url.urlMemo,
                    tag = url.tag
                )
            }

        this.imgUriList = urlDataList.sortedByDescending { it.timeStamp }
            .map {
                it.imgUri
            }
        tagFilteredList = urlList

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
                timeStamp = urlEntity.timeStamp,
                urlName = urlEntity.urlName,
                urlMemo = urlEntity.urlMemo
            )
        }.filter { it.favorite } // 필터링된 결과를 urlList에 다시 할당
        tagFilteredList = urlList
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
                urlName = urlBackupEntity.urlName,
                urlMemo = urlBackupEntity.urlMemo,
                tag = urlBackupEntity.tag
            )
        }.filter { it.favorite } // 필터링된 결과를 urlList에 다시 할당
        tagFilteredList = urlList
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
        holder.bind(tagFilteredList[position])
    }

    override fun getItemCount(): Int {
        return tagFilteredList.size
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

            val spannableString = SpannableString(url.urlName.toString())
            spannableString.setSpan(
                UnderlineSpan(),
                0,
                url.urlName.toString().length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            txUrl.text = spannableString

            urlLink = url.url
//            txUrl.text = url.urlName
            imageKey = url.imageKey
            isFavorite = url.favorite
            imgUri = url.imgUri
            timeStamp = url.timeStamp.toString()
            urlName = url.urlName.toString()
            urlMemo = url.urlMemo.toString()

            if (isFavorite) {
                iconFavorite.visibility = View.VISIBLE
            } else {
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
            Log.e("태그 링크 데이터1",tagFilteredList.toString())

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

            imgView.setOnLongClickListener {

                val clipboard: ClipboardManager =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("label", urlLink)

                clipboard.setPrimaryClip(clip)

                Toast.makeText(context, "클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()

                return@setOnLongClickListener true
            }

            txUrl.setOnClickListener {

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlLink))
                context.startActivity(intent)
            }

        }


    }

}