package kr.baeksuk.urlbox.util.adapter

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemUrlClipBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Url
import kr.baeksuk.urlbox.util.util.OnClipItemClickListener

class RvClipAdapter(ctx: Context, act: Activity, private val listener: OnClipItemClickListener) :
    RecyclerView.Adapter<RvClipAdapter.MyViewHolder>() {

    private val context = ctx
    private val activity = act
    private var clipList = listOf<Url>()

    @SuppressLint("NotifyDataSetChanged")
    fun setGuestData(url: List<UrlEntity>) {
        clipList = url.map { urlEntity ->
            Url(
                url = urlEntity.urlLink,
                imageKey = urlEntity.imageKey,
                timeStamp = urlEntity.timeStamp
            )
        }

        notifyDataSetChanged()

    }

    @SuppressLint("NotifyDataSetChanged")
    fun setUserData(url: List<UrlBackupEntity>) {
        clipList = url.map { urlBackupEntity ->
            Url(
                url = urlBackupEntity.urlLink,
                imageKey = urlBackupEntity.imageKey,
                timeStamp = urlBackupEntity.timeStamp,
                imgUri = urlBackupEntity.imgUri
            )
        }
        notifyDataSetChanged()

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvClipAdapter.MyViewHolder {
        val binding = ItemUrlClipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RvClipAdapter.MyViewHolder, position: Int) {
        holder.bind(clipList[position])
    }

    override fun getItemCount(): Int {
        return clipList.size
    }

    inner class MyViewHolder(binding: ItemUrlClipBinding) : RecyclerView.ViewHolder(binding.root) {

        private var txUrl = binding.txUrl
        private val btnCopy = binding.icClipCopy
        private val btnUrl = binding.btnUrl
        private val btnDelete = binding.icClipDelete
        private val accentLine = binding.accentLine // 액센트 라인 추가
        var imageKey = ""

        fun bind(url: Url) {
            txUrl.text = url.url
            imageKey = url.imageKey
        }

        init {
            // 터치 애니메이션 추가 (기존 기능 유지)
            setupTouchAnimation()

            // 기존 클릭 리스너 (그대로 유지)
            btnUrl.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(txUrl.text.toString()))
                context.startActivity(intent)
            }

            // 기존 롱클릭 리스너 (그대로 유지)
            btnUrl.setOnLongClickListener {
                val clipboard: ClipboardManager =
                    context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("label", txUrl.text.toString())

                clipboard.setPrimaryClip(clip)

                Toast.makeText(context, "클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()

                return@setOnLongClickListener (true)
            }

            btnCopy.setOnClickListener {
                val clipboard: ClipboardManager =
                    context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("label", txUrl.text.toString())

                clipboard.setPrimaryClip(clip)

                Toast.makeText(context, "클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()
            }

            btnDelete.setOnClickListener {
                listener.onDeleteClick(txUrl.text.toString(),imageKey, bindingAdapterPosition)
            }

        }

        // 터치 애니메이션 설정
        @SuppressLint("ClickableViewAccessibility")
        private fun setupTouchAnimation() {
            btnUrl.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 눌렀을 때
                        animateAccentLine(show = true)
                        animateElevation(12f)
                    }

                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        // 뗐을 때
                        animateAccentLine(show = false)
                        animateElevation(4f)
                    }
                }
                false
            }
        }

        // 하단 액센트 라인 애니메이션
        private fun animateAccentLine(show: Boolean) {
            accentLine.animate()
                .scaleX(if (show) 1f else 0f)
                .setDuration(300)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        // Elevation 애니메이션
        private fun animateElevation(targetElevation: Float) {
            ObjectAnimator.ofFloat(btnUrl, "elevation", targetElevation).apply {
                duration = 300
                interpolator = DecelerateInterpolator()
                start()
            }
        }

    }

}