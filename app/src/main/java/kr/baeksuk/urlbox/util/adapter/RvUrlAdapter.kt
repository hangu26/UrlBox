package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.text.SpannableString
import android.text.Spanned
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.TypedValue
import android.view.Gravity
import android.graphics.drawable.ColorDrawable
import android.graphics.Color
import android.widget.ImageView
import android.widget.TextView
import android.widget.PopupWindow
import android.widget.Toast
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlBox.databinding.ItemNativeAdBinding
import kr.baeksuk.urlBox.databinding.ItemUrlListBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.model.UserTags
import kr.baeksuk.urlbox.util.util.AppEvent
import kr.baeksuk.urlbox.util.util.ImgUriListData
import kr.baeksuk.urlbox.util.util.secretLog

class RvUrlAdapter(
    ctx: Context,
    private val onDetailClick: (Url, View, View) -> Unit,
    private val onHideClick: (Url) -> Unit = {},
    private val onDeleteClick: (Url) -> Unit = {},
    private val imageLoader: (Context, Url, ImageView, Int, Boolean, List<String>) -> Unit =
        UrlImageLoader::load
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_URL = 0
        private const val VIEW_TYPE_AD = 1
        private const val AD_INTERVAL = 4
        private const val MAX_NATIVE_AD_CACHE = 2
        private const val NATIVE_AD_UNIT_ID = "ca-app-pub-6498037779961709/3189583387"
    }

    data class IndexedUrl(
        val url: Url,
        val originalIndex: Int
    )

    private sealed class DisplayItem {
        data class UrlItem(val indexedUrl: IndexedUrl) : DisplayItem()
        data class AdItem(val adIndex: Int) : DisplayItem()
    }

    private val context = ctx
    private var urlList = listOf<Url>()
    private var imgUriList = listOf<String>()
    private var filteredIndexedUrls = listOf<IndexedUrl>()
    private var displayItems = listOf<DisplayItem>()
    private var isBackup = false
    private var isGuestMode = false
    private var isHiddenMode = false
    private var hiddenUrlsList = listOf<Url>()
    private val nativeAdCache =
        object : LinkedHashMap<Int, NativeAd>(MAX_NATIVE_AD_CACHE, 0.75f, true) {}
    private val loadingAdSlots = mutableSetOf<Int>()
    private var adSlotCount = 0
    private fun rebuildDisplayItems() {
        val items = mutableListOf<DisplayItem>()
        var adIndex = 0
        filteredIndexedUrls.forEachIndexed { index, indexedUrl ->
            items += DisplayItem.UrlItem(indexedUrl)
            if ((index + 1) % AD_INTERVAL == 0) {
                items += DisplayItem.AdItem(adIndex++)
            }
        }
        displayItems = items
        adSlotCount = adIndex
        clearUnusedAds()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateFilteredUrls(newList: List<IndexedUrl>) {
        filteredIndexedUrls = newList
        rebuildDisplayItems()
        notifyDataSetChanged()
    }

    private fun clearUnusedAds() {
        val removedSlots = nativeAdCache.keys.filter { it >= adSlotCount }
        removedSlots.forEach { slot ->
            nativeAdCache.remove(slot)?.destroy()
        }
        loadingAdSlots.removeAll { it >= adSlotCount }
    }

    private fun getAdPosition(adIndex: Int): Int {
        return displayItems.indexOfFirst { it is DisplayItem.AdItem && it.adIndex == adIndex }
    }

    private fun loadNativeAd(adIndex: Int) {
        if (!loadingAdSlots.add(adIndex)) return

        val adLoader = AdLoader.Builder(context, NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                Handler(Looper.getMainLooper()).post {
                    loadingAdSlots.remove(adIndex)
                    if (adIndex >= adSlotCount) {
                        ad.destroy()
                        return@post
                    }

                    nativeAdCache[adIndex]?.destroy()
                    nativeAdCache[adIndex] = ad
                    trimNativeAdCache()

                    val position = getAdPosition(adIndex)
                    if (position != -1) {
                        notifyItemChanged(position)
                    }
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Handler(Looper.getMainLooper()).post {
                        loadingAdSlots.remove(adIndex)
                        Log.e("NativeAd", "광고 로드 실패: ${error.message}")
                    }
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    private fun trimNativeAdCache() {
        while (nativeAdCache.size > MAX_NATIVE_AD_CACHE) {
            val eldestKey = nativeAdCache.entries.iterator().next().key
            nativeAdCache.remove(eldestKey)?.destroy()
        }
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
        isHiddenMode = false
        isBackup = false
        imgUriList = emptyList()
        isGuestMode = true
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

    fun setGuestMode(isGuest: Boolean) {
        isGuestMode = isGuest
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUserBackupData(url: List<UrlBackupEntity>, isLoginBackup: Boolean) {
        isHiddenMode = false
        isBackup = isLoginBackup
        isGuestMode = false
        val newUrlList = url
                .let { list -> if (AppEvent.showHiddenState.value) list else list.filter { !it.hidden } }
            .sortedByDescending { it.timeStamp }
            .map { urlBackupEntity ->
                Url(
                    url = urlBackupEntity.urlLink,
                    imageKey = urlBackupEntity.imageKey,
                    imgUri = urlBackupEntity.imgUri,
                    favorite = urlBackupEntity.favorite,
                    hidden = urlBackupEntity.hidden,
                    timeStamp = urlBackupEntity.timeStamp,
                    urlName = urlBackupEntity.urlName,
                    urlMemo = urlBackupEntity.urlMemo,
                    tag = urlBackupEntity.tag
                )
            }
        imgUriList = if (newUrlList.isNotEmpty()) newUrlList.map { it.imgUri } else emptyList()
        if (newUrlList != urlList) {
            urlList = newUrlList
            updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setLoginData(urlDataList: List<Url>, imgUriList: List<String>, isLoginBackup: Boolean) {
        isHiddenMode = false
        isBackup = isLoginBackup
        urlList = urlDataList
            .filter { AppEvent.showHiddenState.value || !it.hidden }
            .sortedByDescending { it.timeStamp }
            .map { url ->
                Url(
                    url = url.url,
                    imageKey = url.imageKey,
                    imgUri = url.imgUri,
                    favorite = url.favorite,
                    hidden = url.hidden,
                    timeStamp = url.timeStamp,
                    urlName = url.urlName,
                    urlMemo = url.urlMemo,
                    tag = url.tag
                )
            }
            .distinct()
        this.imgUriList = if (urlList.isNotEmpty() && urlList.any { it.imgUri.isNotBlank() }) {
            urlList.map { it.imgUri }
        } else {
            if (urlList.isNotEmpty()) imgUriList else emptyList()
        }
        ImgUriListData.imgUriListData = this.imgUriList
        updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setFavoriteData(url: List<UrlEntity>) {
        isHiddenMode = false
        isBackup = false
        imgUriList = emptyList()
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
        isHiddenMode = false
        isBackup = true
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
        imgUriList = if (urlList.isNotEmpty()) urlList.map { it.imgUri } else emptyList()
        updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setHiddenData(urlList: List<Url>) {
        isHiddenMode = true
        isBackup = true
        this.urlList = urlList
        imgUriList = if (urlList.isNotEmpty()) urlList.map { it.imgUri } else emptyList()
        updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addHiddenUrls(hiddenUrls: List<Url>) {
        if (hiddenUrls.isEmpty()) {
            secretLog("RvUrlAdapter - addHiddenUrls: No hidden URLs to add")
            return
        }
        secretLog("RvUrlAdapter - addHiddenUrls: Current urlList size: ${urlList.size}, Hidden URLs: ${hiddenUrls.size}")
        hiddenUrlsList = hiddenUrls
        // 기존 리스트와 hidden URL을 합치되, 중복을 제거
        val combinedList = urlList + hiddenUrls
        val uniqueUrls = combinedList.distinctBy { it.url }.sortedByDescending { it.timeStamp }
        urlList = uniqueUrls
        Log.e("addHiddenUrls", "New urlList size: ${urlList.size}")
        imgUriList = if (uniqueUrls.isNotEmpty()) uniqueUrls.map { it.imgUri } else emptyList()
        updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeHiddenUrls() {
        if (hiddenUrlsList.isEmpty()) return
        val newUrlList = urlList.filter { url -> 
            !hiddenUrlsList.any { it.url == url.url } 
        }
        hiddenUrlsList = emptyList()
        urlList = newUrlList
        imgUriList = if (newUrlList.isNotEmpty()) newUrlList.map { it.imgUri } else emptyList()
        updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
    }

    @SuppressLint("NotifyDataSetChanged")
    fun removeUrlFromList(urlToRemove: String) {
        val newUrlList = urlList.filter { it.url != urlToRemove }
        val newHiddenList = hiddenUrlsList.filter { it.url != urlToRemove }
        urlList = newUrlList
        hiddenUrlsList = newHiddenList
        imgUriList = if (newUrlList.isNotEmpty()) newUrlList.map { it.imgUri } else emptyList()
        updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateUrlHiddenStatus(urlToUpdate: String, hidden: Boolean) {
        urlList = urlList.map { url ->
            if (url.url == urlToUpdate) {
                url.copy(hidden = hidden)
            } else {
                url
            }
        }
        hiddenUrlsList = hiddenUrlsList.filter { it.url != urlToUpdate }
        updateFilteredUrls(urlList.mapIndexed { index, item -> IndexedUrl(item, index) })
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_AD -> AdViewHolder(
                ItemNativeAdBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )

            else -> {
                val layoutId = if (isHiddenMode) R.layout.item_url_list_hidden else R.layout.item_url_list
                val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
                MyViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = displayItems[position]) {
            is DisplayItem.UrlItem -> (holder as MyViewHolder).bind(item.indexedUrl)
            is DisplayItem.AdItem -> (holder as AdViewHolder).bind(item.adIndex)
        }
    }

    override fun getItemCount(): Int = displayItems.size
    override fun getItemViewType(position: Int): Int {
        return when (displayItems[position]) {
            is DisplayItem.UrlItem -> VIEW_TYPE_URL
            is DisplayItem.AdItem -> VIEW_TYPE_AD
        }
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txUrl: TextView = itemView.findViewById(R.id.tx_url)
        private val imgView: ImageView = itemView.findViewById(R.id.img_thumbnail)
        private val iconFavorite: ImageView = itemView.findViewById(R.id.icon_favorite)
        private val iconHidden: ImageView = itemView.findViewById(R.id.icon_hidden)


        fun bind(indexedUrl: IndexedUrl) {
            val url = indexedUrl.url
            val spannableString = SpannableString(url.urlName.toString()).apply {
                setSpan(
                    UnderlineSpan(),
                    0,
                    url.urlName.toString().length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            txUrl.text = spannableString
            iconFavorite.visibility = if (url.favorite) View.VISIBLE else View.GONE
            iconHidden.visibility = if (url.hidden) View.VISIBLE else View.GONE
            imageLoader(context, url, imgView, indexedUrl.originalIndex, isBackup, imgUriList)

            imgView.setOnClickListener {
                onDetailClick(url, txUrl, imgView)
            }
            
            imgView.setOnLongClickListener {
                showContextMenu(imgView, url)
                true
            }
            
            txUrl.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, url.url.toUri())
                context.startActivity(intent)
            }
            
            txUrl.setOnLongClickListener { v ->
                showContextMenu(v, url)
                true
            }
        }
        
        private fun dpToPx(dp: Int): Int =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), context.resources.displayMetrics).toInt()

        private fun showContextMenu(view: View, url: Url) {
            val popupView = LayoutInflater.from(context).inflate(R.layout.popup_url_actions, null)

            val actionCopy = popupView.findViewById<View>(R.id.action_copy)
            val actionHide = popupView.findViewById<View>(R.id.action_hide)
            val actionHideIcon = popupView.findViewById<ImageView>(R.id.action_hide_icon)
            val actionHideText = popupView.findViewById<TextView>(R.id.action_hide_text)
            val actionDelete = popupView.findViewById<View>(R.id.action_delete)

            // Decide label per item: if the item is already hidden -> show '복원하기', else '숨기기'
            if (url.hidden) {
                actionHideText?.text = "복원하기"
                actionHideIcon?.setImageResource(R.drawable.ic_close_tag)
                actionHide.contentDescription = "복원하기"
            } else {
                actionHideText?.text = "숨기기"
                actionHideIcon?.setImageResource(R.drawable.ic_lock)
                actionHide.contentDescription = "숨기기"
            }

            // 게스트 모드일 때 숨기기 버튼 비활성화
            if (isGuestMode) {
                actionHide.alpha = 0.5f
                actionHide.isEnabled = false
                actionHide.setOnClickListener(null)
            }

            val popupWidth = dpToPx(100)
            val popupWindow = PopupWindow(popupView, popupWidth, ViewGroup.LayoutParams.WRAP_CONTENT, true)
            popupWindow.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            popupWindow.isOutsideTouchable = true
            try {
                popupWindow.elevation = 12f
            } catch (_: Throwable) {}

            // Measure to compute height
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(popupWidth, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.UNSPECIFIED
            )
            val popupHeight = popupView.measuredHeight

            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val anchorX = location[0]
            val anchorY = location[1]

            val screenWidth = context.resources.displayMetrics.widthPixels
            val margin = dpToPx(8)

            // position popup near the right-middle of the view (slightly overlapping)
            var x = anchorX + view.width - popupWidth - margin
            // ensure popup stays within screen bounds
            if (x + popupWidth + margin > screenWidth) x = screenWidth - popupWidth - margin
            if (x < margin) x = margin

            val y = anchorY + view.height / 2 - popupHeight / 2

            popupWindow.showAtLocation(view.rootView, Gravity.NO_GRAVITY, x, y)

            actionCopy.setOnClickListener {
                val clipboard: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("label", url.url)
                clipboard.setPrimaryClip(clip)
                
                val rootView = (context as? android.app.Activity)?.window?.decorView?.findViewById<android.view.View>(android.R.id.content)
                if (rootView != null) {
                    com.google.android.material.snackbar.Snackbar.make(rootView, "클립보드에 복사되었습니다.", com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()
                }
                popupWindow.dismiss()
            }

            if (!isGuestMode) {
                actionHide.setOnClickListener {
                    onHideClick(url)
                    popupWindow.dismiss()
                }
            }

            actionDelete.setOnClickListener {
                onDeleteClick(url)
                popupWindow.dismiss()
            }
        }
    }

    inner class AdViewHolder(private val binding: ItemNativeAdBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(adIndex: Int) {
            binding.nativeAdView.visibility = View.VISIBLE
            val cachedAd = nativeAdCache[adIndex]
            if (cachedAd != null) {
                populateNativeAd(cachedAd)
            } else if (!loadingAdSlots.contains(adIndex)) {
                loadNativeAd(adIndex)
            }
        }

        private fun populateNativeAd(ad: NativeAd) {
            binding.nativeAdView.headlineView = binding.adHeadline
            binding.nativeAdView.mediaView = binding.adMedia
            binding.adHeadline.text = ad.headline
            ad.mediaContent?.let { binding.adMedia.setMediaContent(it) }
            binding.nativeAdView.setNativeAd(ad)
            binding.nativeAdView.visibility = View.VISIBLE
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        nativeAdCache.values.forEach { it.destroy() }
        nativeAdCache.clear()
        loadingAdSlots.clear()
        super.onDetachedFromRecyclerView(recyclerView)
    }
}
