package kr.baeksuk.urlbox.util.adapter
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import kr.baeksuk.urlBox.databinding.ItemNativeAdBinding
import kr.baeksuk.urlBox.databinding.ItemUrlListBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.GuestModeHandler
import kr.baeksuk.urlbox.model.LoggedInModeHandler
import kr.baeksuk.urlbox.model.ModeHandler
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.UserTags
import kr.baeksuk.urlbox.util.util.ImgUriListData
import kr.baeksuk.urlbox.view.urldetail.UrlDetailActivity
class RvUrlAdapter(ctx: Context, act: Activity) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_URL = 0
        private const val VIEW_TYPE_AD = 1
        private const val AD_INTERVAL = 4
        private const val NATIVE_AD_UNIT_ID = "ca-app-pub-6498037779961709/3189583387"
    }
    data class IndexedUrl(
        val url: Url,
        val originalIndex: Int
    )
    private sealed class DisplayItem {
        data class UrlItem(val indexedUrl: IndexedUrl) : DisplayItem()
        object AdItem : DisplayItem()
    }
    private val context = ctx
    private val activity = act
    private var urlList = listOf<Url>()
    private var imgUriList = listOf<String>()
    private var filteredIndexedUrls = listOf<IndexedUrl>()
    private var displayItems = listOf<DisplayItem>()
    private var isBackup = false
    private fun rebuildDisplayItems() {
        val items = mutableListOf<DisplayItem>()
        filteredIndexedUrls.forEachIndexed { index, indexedUrl ->
            items += DisplayItem.UrlItem(indexedUrl)
            if ((index + 1) % AD_INTERVAL == 0) {
                items += DisplayItem.AdItem
            }
        }
        displayItems = items
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun updateFilteredUrls(newList: List<IndexedUrl>) {
        filteredIndexedUrls = newList
        rebuildDisplayItems()
        notifyDataSetChanged()
    }
    fun filterByTag(tagUrl: List<String>, tag: String, recyclerview: RecyclerView) {
        val newList = when (tag) {
            "전체" -> urlList.mapIndexed { index, url -> IndexedUrl(url, index) }
            "즐겨찾기" -> urlList.mapIndexed { index, url -> IndexedUrl(url, index) }
                .filter { it.url.favorite }
            else -> urlList.mapIndexed { index, url -> IndexedUrl(url, index) }
                .filter { it.url.url in tagUrl }
        }
        updateFilteredUrls(newList)
        recyclerview.scheduleLayoutAnimation()
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
        updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
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
        if (newUrlList != urlList) {
            urlList = newUrlList
            updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    fun setLoginData(urlDataList: List<Url>, imgUriList: List<String>, isLoginBackup: Boolean) {
        isBackup = isLoginBackup
        urlList = urlDataList
            .sortedByDescending { it.timeStamp }
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
            .distinct()
        this.imgUriList = imgUriList
        ImgUriListData.imgUriListData = this.imgUriList
        updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
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
        }.filter { it.favorite }
        updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
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
        }.filter { it.favorite }
        imgUriList = urlList.map { it.imgUri }
        updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_AD -> AdViewHolder(
                ItemNativeAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
            else -> MyViewHolder(
                ItemUrlListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is DisplayItem.UrlItem -> (holder as MyViewHolder).bind(item.indexedUrl)
            DisplayItem.AdItem -> (holder as AdViewHolder).bind()
        }
    }
    override fun getItemCount(): Int = displayItems.size
    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is DisplayItem.UrlItem -> VIEW_TYPE_URL
            DisplayItem.AdItem -> VIEW_TYPE_AD
        }
    }
    inner class MyViewHolder(binding: ItemUrlListBinding) : RecyclerView.ViewHolder(binding.root) {
        private var imageKey = ""
        private var isFavorite = false
        private var imgUri = ""
        private var urlLink = ""
        private val txUrl = binding.txUrl
        private val imgView = binding.imgThumbnail
        private val iconFavorite = binding.iconFavorite
        private var timeStamp = ""
        private var urlName = ""
        private var urlMemo = ""
        private var currentUrlIndex = 0
        private val pref = context.getSharedPreferences("User", Context.MODE_PRIVATE)
        private val autoLogin = pref.getBoolean("auto login", false)
        fun bind(indexedUrl: IndexedUrl) {
            val url = indexedUrl.url
            currentUrlIndex = indexedUrl.originalIndex
            val spannableString = SpannableString(url.urlName.toString())
            spannableString.setSpan(
                UnderlineSpan(),
                0,
                url.urlName.toString().length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            txUrl.text = spannableString
            urlLink = url.url
            imageKey = url.imageKey
            isFavorite = url.favorite
            imgUri = url.imgUri
            timeStamp = url.timeStamp.toString()
            urlName = url.urlName.toString()
            urlMemo = url.urlMemo.toString()
            iconFavorite.visibility = if (isFavorite) View.VISIBLE else View.GONE
            val modeHandler: ModeHandler = if (autoLogin) {
                LoggedInModeHandler(imgUriList, currentUrlIndex)
            } else {
                GuestModeHandler(imageKey, context)
            }
            modeHandler.loadImage(url.imgUri, imgView, context, isBackup)
        }
        init {
            Log.e("태그 링크 데이터1", urlList.toString())
            imgView.setOnClickListener {
                val options = ActivityOptions.makeSceneTransitionAnimation(
                    activity,
                    Pair.create(txUrl, "titleTran"),
                    Pair.create(imgView, "imageTran")
                )
                val intent = Intent(context, UrlDetailActivity::class.java)
                val modeHandler: ModeHandler = if (autoLogin) {
                    LoggedInModeHandler(imgUriList, currentUrlIndex)
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
                true
            }
            txUrl.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, urlLink.toUri())
                context.startActivity(intent)
            }
        }
    }
    inner class AdViewHolder(private val binding: ItemNativeAdBinding) : RecyclerView.ViewHolder(binding.root) {
        private var nativeAd: NativeAd? = null
        private var isLoading = false
        private var isReleased = false
        fun bind() {
            isReleased = false
            binding.nativeAdView.visibility = View.VISIBLE
            if (nativeAd != null) {
                populateNativeAd(nativeAd!!)
                return
            }
            if (!isLoading) {
                loadNativeAd()
            }
        }
        private fun loadNativeAd() {
            isLoading = true
            Thread {
                val adLoader = AdLoader.Builder(context, NATIVE_AD_UNIT_ID)
                    .forNativeAd { ad ->
                        binding.root.post {
                            if (isReleased) {
                                ad.destroy()
                                isLoading = false
                                return@post
                            }
                            nativeAd?.destroy()
                            nativeAd = ad
                            isLoading = false
                            populateNativeAd(ad)
                        }
                    }
                    .withAdListener(object : AdListener() {
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            binding.root.post {
                                Log.e("NativeAd", "광고 로드 실패: ${error.message}")
                                isLoading = false
                                binding.nativeAdView.visibility = View.VISIBLE
                            }
                        }
                    })
                    .build()
                adLoader.loadAd(AdRequest.Builder().build())
            }.start()
        }
        private fun populateNativeAd(ad: NativeAd) {
            binding.nativeAdView.headlineView = binding.adHeadline
            binding.nativeAdView.mediaView = binding.adMedia
            binding.adHeadline.text = ad.headline
            ad.mediaContent?.let { binding.adMedia.setMediaContent(it) }
            binding.nativeAdView.setNativeAd(ad)
            binding.nativeAdView.visibility = View.VISIBLE
        }
        fun recycle() {
            isReleased = true
            isLoading = false
            nativeAd?.destroy()
            nativeAd = null
        }
    }
    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is AdViewHolder) {
            holder.recycle()
        }
        super.onViewRecycled(holder)
    }
}
