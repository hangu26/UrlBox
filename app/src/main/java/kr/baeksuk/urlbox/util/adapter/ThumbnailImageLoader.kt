package kr.baeksuk.urlbox.util.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.widget.ImageView
import com.bumptech.glide.Glide
import kr.baeksuk.urlbox.model.Url
import java.io.File

object ThumbnailImageLoader {

    fun load(
        context: Context,
        url: Url,
        imageView: ImageView,
        position: Int,
        isBackup: Boolean,
        imgUriList: List<String>
    ) {

        val loginSource = if (isBackup) {
            url.imgUri.takeIf { it.isNotBlank() } ?: imgUriList.getOrNull(position).orEmpty()
        }else {
            url.imgUri.takeIf { it.isNotBlank() } ?: imgUriList.getOrNull(position).orEmpty()
        }
        if (loginSource.isNotBlank()) {
            Glide.with(context)
                .load(loginSource)
                .into(imageView)
            return
        }

        // 게스트면 로컬 파일에서 ImageKey.png 읽기
        val file = File(context.filesDir, "${url.imageKey}.png")
        if (file.exists()){
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            imageView.setImageBitmap(bitmap)
        }

    }

}