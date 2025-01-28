package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemUrlListBinding
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Url
import java.io.File

class RvUrlAdapter(ctx: Context) : RecyclerView.Adapter<RvUrlAdapter.MyViewHolder>() {

    private val context = ctx
    private var urlList = listOf<Url>()

    @SuppressLint("NotifyDataSetChanged")
    fun setGuestData(url: List<UrlEntity>) {
        urlList = url.map { urlEntity ->
            Url(
                url = urlEntity.urlLink,
                imageKey = urlEntity.imageKey
            )
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
        private val txUrl = binding.txUrl
        private val imgView = binding.imgThumbnail
        private val directory: File = context.filesDir
        fun bind(url: Url) {
            txUrl.text = url.url
            imageKey = url.imageKey

            val directory = context.filesDir // UrlFragment에서 context 사용
            val filePath = "$directory/$imageKey.png"
            val file = File(filePath)

            if (file.exists()) {
                // BitmapFactory로 파일을 Bitmap으로 변환
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                // ImageView에 설정
                imgView.setImageBitmap(bitmap)
            } else {
                // 기본 이미지 설정 (이미지가 없는 경우)
                Log.d("파일 없음", "없음")
            }
        }


    }

}