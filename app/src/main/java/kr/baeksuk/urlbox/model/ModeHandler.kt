package kr.baeksuk.urlbox.model

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.File

// ModeHandler.kt
interface ModeHandler {
    fun loadImage(imgUri: String, imgView: ImageView, context: Context, isBackup: Boolean)

    fun intentUrlToDetail(
        intent: Intent,
        txUrl: String,
        imgUri: String,
        isFavorite: Boolean,
        imageKey: String,
        timeStamp : String,
        urlName : String? = null,
        urlMemo : String? = null
    )

}

/** 로그인 했을 때 이미지를 받아오는 로직 **/
class LoggedInModeHandler(private val imgUriList: List<String>, private val layoutPosition: Int) :
    ModeHandler {
    override fun loadImage(
        imgUri: String,
        imgView: ImageView,
        context: Context,
        isBackup: Boolean
    ) {

        if (isBackup) {
            Glide.with(context)
                .load(imgUri)
                .into(imgView)
        } else {
            Glide.with(context)
                .load(imgUriList[layoutPosition])
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imgView)
        }
    }

    override fun intentUrlToDetail(
        intent: Intent,
        txUrl: String,
        imgUri: String,
        isFavorite: Boolean,
        imageKey: String,
        timeStamp : String,
        urlName: String?,
        urlMemo : String?
    ) {
        intent.putExtra("title", txUrl)
        intent.putExtra("imgUri", imgUri)
        intent.putExtra("imageKey", imageKey)
        intent.putExtra("timeStamp", timeStamp)
        intent.putExtra("isFavorite", isFavorite)
        intent.putExtra("urlName", urlName)
        intent.putExtra("urlMemo", urlMemo)
    }
}

/** 게스트 모드일 때 이미지를 받아오는 로직 **/
class GuestModeHandler(private val imageKey: String, private val context: Context) : ModeHandler {
    override fun loadImage(
        imgUri: String,
        imgView: ImageView,
        context: Context,
        isBackup: Boolean
    ) {
        // 로컬 파일에서 이미지 불러오기
        val directory = context.filesDir
        val filePath = "$directory/$imageKey.png"
        val file = File(filePath)
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)

        if (file.exists()) {
            imgView.setImageBitmap(bitmap)
        } else {
            Log.e("파일 없음", "없음")
        }
    }

    override fun intentUrlToDetail(
        intent: Intent,
        txUrl: String,
        imgUri: String,
        isFavorite: Boolean,
        imageKey: String,
        timeStamp : String,
        urlName: String?,
        urlMemo : String?
    ) {
        intent.putExtra("title", txUrl)
        intent.putExtra("image", imageKey)
        intent.putExtra("isFavorite", isFavorite)
    }
}
