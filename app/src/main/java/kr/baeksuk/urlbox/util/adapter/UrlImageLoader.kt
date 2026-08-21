package kr.baeksuk.urlbox.util.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.bumptech.glide.Glide
import kr.baeksuk.urlBox.R
import kr.baeksuk.urlbox.model.Url
import java.io.File

object UrlImageLoader {
    /** load */
    fun load(
        context: Context,
        url: Url,
        imageView: ImageView,
        position: Int,
        isBackup: Boolean,
        imgUriList: List<String>
    ) {
        val directSource = url.imgUri.takeIf { it.isNotBlank() }
        if (!directSource.isNullOrBlank()) {
            Glide.with(context)
                .load(directSource)
                .into(imageView)
            return
        }

        val file = File(context.filesDir, "${url.imageKey}.png")
        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imageView.setImageBitmap(bitmap)
            return
        }

        val fallbackSource = imgUriList.getOrNull(position)
            ?.takeIf { it.isNotBlank() }
            ?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

        if (!fallbackSource.isNullOrBlank()) {
            Glide.with(context)
                .load(fallbackSource)
                .into(imageView)
            return
        }

        imageView.setImageResource(R.drawable.urlbox_icon)
    }
}



