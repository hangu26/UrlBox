package kr.baeksuk.urlbox.util.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemThumbnailPageBinding
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.util.UrlData
import kr.baeksuk.urlbox.util.util.ViewPagerPosition
import java.io.File

class ImgPagerRvAdapter(private val urlList : List<Url>, ctx : Context, act: Activity) : RecyclerView.Adapter<ImgPagerRvAdapter.MyViewHolder>() {

    private val context = ctx
    private val activity = act

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ImgPagerRvAdapter.MyViewHolder {
        val binding = ItemThumbnailPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImgPagerRvAdapter.MyViewHolder, position: Int) {
        holder.bind(urlList[position], position)
    }

    override fun getItemCount(): Int {
        return urlList.size
    }

    fun getViewHolderAtPosition(recyclerView: RecyclerView, position: Int): MyViewHolder? {
        return recyclerView.findViewHolderForAdapterPosition(position) as? MyViewHolder
    }

    inner class MyViewHolder(binding: ItemThumbnailPageBinding) : RecyclerView.ViewHolder(binding.root){

        private var thumbnail = binding.imgUrl
        private var imageKey = ""
        private var txUrl = binding.txUrl
        private var isFavorite = false

        fun getThumbnail(): ImageView {
            return thumbnail
        }

        fun bind(url : Url, position : Int){
            imageKey = url.imageKey
            txUrl.text = url.url.toString()
            isFavorite = url.favorite

            ViewPagerPosition.thumbnail = thumbnail

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

        fun updateTransitionName(newPosition: Int) {
            thumbnail.transitionName = "imageTran_$newPosition"
            Log.e("Transition Name 업데이트", "imageTran_$newPosition")
        }

        init {

            val itemPosition = UrlData.selectedPosition

            thumbnail.transitionName = "imageTran_$itemPosition"
            Log.e("아이템 번호 뷰페이저", "imageTran_$itemPosition")

            txUrl.setOnClickListener {

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(txUrl.text.toString()))
                context.startActivity(intent)

            }

        }

    }

}