package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.util.Pair
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemThumbnailListBinding
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.util.UrlData
import kr.baeksuk.urlbox.view.urldetail.UrlDetailActivity
import java.io.File

class RvThumbnailAdapter(ctx : Context, act: Activity): RecyclerView.Adapter<RvThumbnailAdapter.MyViewHolder>() {

    private val context = ctx
    private val activity = act
    private var thumbnailList = listOf<Url>()

    @SuppressLint("NotifyDataSetChanged")
    fun setGuestData(url: List<UrlEntity>) {
        thumbnailList = url.map { urlEntity ->
            Url(
                url = urlEntity.urlLink,
                imageKey = urlEntity.imageKey,
                favorite = urlEntity.favorite
            )
        }

        notifyDataSetChanged()

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding = ItemThumbnailListBinding.inflate(LayoutInflater.from(parent.context))
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RvThumbnailAdapter.MyViewHolder, position: Int) {
        holder.bind(thumbnailList[position])
    }

    override fun getItemCount(): Int {
        return thumbnailList.size
    }

    inner class MyViewHolder(binding : ItemThumbnailListBinding) : RecyclerView.ViewHolder(binding.root){

        private var thumbnail = binding.imgThumbnail
        private var imageKey = ""
        private var txUrl = ""
        private var isFavorite = false

        fun bind(url : Url){
            imageKey = url.imageKey
            txUrl = url.url
            isFavorite = url.favorite

            val directory = context.filesDir // UrlFragment에서 context 사용
            val filePath = "$directory/$imageKey.png"
            val file = File(filePath)
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)

            if (file.exists()) {

                thumbnail.setImageBitmap(bitmap)

            } else {
                // 기본 이미지 설정 (이미지가 없는 경우)
                Log.d("파일 없음", "없음")
            }

        }

        init {

            thumbnail.setOnClickListener {

                val options = ActivityOptions.makeSceneTransitionAnimation(
                    activity,
                    Pair.create(thumbnail, "imageTran")
                )

                val intent = Intent(context, UrlDetailActivity::class.java)
                intent.putExtra("title", txUrl)
                intent.putExtra("image", imageKey)
                intent.putExtra("isFavorite", isFavorite)

                UrlData.urlList = thumbnailList
                UrlData.selectedPosition = layoutPosition

                Log.d("포지션", layoutPosition.toString())
                context.startActivity(intent, options.toBundle())
            }

        }

    }

}