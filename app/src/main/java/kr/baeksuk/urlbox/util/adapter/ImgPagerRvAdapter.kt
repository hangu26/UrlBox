package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.core.net.toUri
import androidx.core.view.doOnPreDraw
import kr.baeksuk.urlBox.databinding.ItemThumbnailPageBinding
import kr.baeksuk.urlbox.model.Url

class ImgPagerRvAdapter(
    private var urlList: List<Url>,
    private val startPosition: Int,
    ctx: Context,
    private val onCurrentPageReady: ((View) -> Unit)? = null
) :
    RecyclerView.Adapter<ImgPagerRvAdapter.MyViewHolder>() {

    private val context = ctx
    private var enterTransitionStarted = false

    private fun transitionNameFor(url: Url): String {
        val key = if (url.imageKey.isNotBlank()) url.imageKey else url.url
        return "imageTran_$key"
    }

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

    fun updateData(newList: List<Url>) {
        urlList = newList
        notifyDataSetChanged()
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class MyViewHolder(binding: ItemThumbnailPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        val thumbnail = binding.imgUrl
        private var txUrl = binding.txUrl
        private var isFavorite = false

        fun bind(url: Url, position: Int) {
            txUrl.text = url.url
            isFavorite = url.favorite

            thumbnail.transitionName = transitionNameFor(url)
            ThumbnailImageLoader.load(context, url, thumbnail, position, true, emptyList())

            if (!enterTransitionStarted && position == startPosition) {
                thumbnail.doOnPreDraw {
                    enterTransitionStarted = true
                    onCurrentPageReady?.invoke(thumbnail)
                }
            }

        }

        init {


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