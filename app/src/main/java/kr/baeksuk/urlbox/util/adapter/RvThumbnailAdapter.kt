package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemThumbnailListBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Url

class RvThumbnailAdapter(
    ctx: Context,
    private val onItemClick: (Url, View, Int) -> Unit,
    private val imageLoader: (Context, Url, ImageView, Int, Boolean, List<String>) -> Unit =
        ThumbnailImageLoader::load
) :
    RecyclerView.Adapter<RvThumbnailAdapter.MyViewHolder>() {

    private val context = ctx
    private var thumbnailList = listOf<Url>()
    private var imgUriList = listOf<String>()
    private var isBackup = false

    init {
        setHasStableIds(true)
    }

    private fun transitionNameFor(url: Url): String {
        val key = if (url.imageKey.isNotBlank()) url.imageKey else url.url
        return "imageTran_$key"
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setGuestData(url: List<UrlEntity>) {
        thumbnailList = url.map { urlEntity ->
            Url(
                url = urlEntity.urlLink,
                imageKey = urlEntity.imageKey,
                favorite = urlEntity.favorite,
                timeStamp = urlEntity.timeStamp
            )
        }

        notifyDataSetChanged()

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUserBackupData(url: List<UrlBackupEntity>, isLoginBackup: Boolean) {

        isBackup = isLoginBackup
        thumbnailList = url.sortedByDescending { it.timeStamp }
            .map { urlBackupEntity ->
                Url(
                    url = urlBackupEntity.urlLink,
                    imageKey = urlBackupEntity.imageKey,
                    imgUri = urlBackupEntity.imgUri,
                    favorite = urlBackupEntity.favorite,
                    timeStamp = urlBackupEntity.timeStamp
                )

            }
        imgUriList = url.sortedByDescending { it.timeStamp }
            .map { it.imgUri }

        notifyDataSetChanged()

    }

    override fun getItemId(position: Int): Long {
        val item = thumbnailList.getOrNull(position) ?: return RecyclerView.NO_ID
        val key = if (item.imageKey.isNotBlank()) item.imageKey else item.url
        return key.hashCode().toLong()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding = ItemThumbnailListBinding.inflate(LayoutInflater.from(parent.context))
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(thumbnailList[position], position)
    }

    override fun getItemCount(): Int {
        return thumbnailList.size
    }

    inner class MyViewHolder(binding: ItemThumbnailListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        var thumbnail = binding.imgThumbnail
        private val iconFavorite = binding.iconFavorite

        fun bind(url: Url, position: Int) {
            thumbnail.transitionName = transitionNameFor(url)
            iconFavorite.visibility = if (url.favorite) View.VISIBLE else View.GONE

            imageLoader(
                context,
                url,
                thumbnail,
                position,
                isBackup,
                imgUriList
            )

            thumbnail.setOnClickListener {
                onItemClick(url, thumbnail, position)
            }
        }

    }

}