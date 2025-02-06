package kr.baeksuk.urlbox.util.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import kr.baeksuk.urlBox.databinding.ItemUrlClipBinding
import kr.baeksuk.urlbox.data.local.entity.UrlBackupEntity
import kr.baeksuk.urlbox.data.local.entity.UrlEntity
import kr.baeksuk.urlbox.model.Url

class RvClipAdapter(ctx : Context, act: Activity): RecyclerView.Adapter<RvClipAdapter.MyViewHolder>() {

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
    fun setUserData(url : List<UrlBackupEntity>){
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
        val binding = ItemUrlClipBinding.inflate(LayoutInflater.from(parent.context))
        return MyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RvClipAdapter.MyViewHolder, position: Int) {
        holder.bind(clipList[position])
    }

    override fun getItemCount(): Int {
        return clipList.size
    }

    inner class MyViewHolder(binding : ItemUrlClipBinding) : RecyclerView.ViewHolder(binding.root){

        private var txUrl = binding.txUrl
        private val btnUrl = binding.btnUrl
        fun bind(url : Url) {

            txUrl.text = url.url

        }

        init {

            btnUrl.setOnClickListener {

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(txUrl.text.toString()))
                context.startActivity(intent)

            }

            btnUrl.setOnLongClickListener {

                val clipboard: ClipboardManager = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("label", txUrl.text.toString())

                clipboard.setPrimaryClip(clip)

                Toast.makeText(context, "클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show()

                return@setOnLongClickListener(true)
            }

        }

    }

}