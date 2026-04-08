package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.core.net.toUri
import kr.baeksuk.urlBox.databinding.ItemThumbnailPageBinding
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.util.UrlData
import kr.baeksuk.urlbox.util.util.ViewPagerPosition

class ImgPagerRvAdapter(private val urlList: List<Url>, ctx: Context) :
    RecyclerView.Adapter<ImgPagerRvAdapter.MyViewHolder>() {

    private val context = ctx

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val binding =
            ItemThumbnailPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        holder.bind(urlList[position], position)
    }

    override fun getItemCount(): Int {
        return urlList.size
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class MyViewHolder(binding: ItemThumbnailPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var thumbnail = binding.imgUrl
        private var txUrl = binding.txUrl
        private var isFavorite = false

        fun bind(url: Url, position: Int) {
            txUrl.text = url.url
            isFavorite = url.favorite

            ViewPagerPosition.thumbnail = thumbnail
            ThumbnailImageLoader.load(context, url, thumbnail, position, true, emptyList())


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

                val intent = Intent(Intent.ACTION_VIEW, txUrl.text.toString().toUri())
                context.startActivity(intent)

            }

            binding.clLink.setOnTouchListener { v, event ->

                setTouchAnimation(v, event)

                if (event?.action == MotionEvent.ACTION_UP) {
                    val intent = Intent(Intent.ACTION_VIEW, txUrl.text.toString().toUri())
                    context.startActivity(intent)
                }

                false
            }

            binding.txUrl.setOnTouchListener { _, event ->

                setTouchAnimation(binding.clLink, event)

                false
            }

        }

    }

    fun setTouchAnimation(view: View, event: MotionEvent?) {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    view.animate().scaleX(0.97f).scaleY(0.97f).translationZ(5f).setDuration(100)
                        .start()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    view.animate().scaleX(1f).scaleY(1f).translationZ(20f).setDuration(100).start()
                }
            }
        }
    }

}