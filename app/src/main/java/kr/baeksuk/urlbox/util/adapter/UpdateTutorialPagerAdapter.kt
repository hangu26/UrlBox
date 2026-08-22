package kr.baeksuk.urlbox.util.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.core.content.res.ResourcesCompat
import com.airbnb.lottie.FontAssetDelegate
import com.airbnb.lottie.LottieAnimationView
import kr.baeksuk.urlBox.R

class UpdateTutorialPagerAdapter(
    private val pageLayouts: List<Int>
) : RecyclerView.Adapter<UpdateTutorialPagerAdapter.UpdateTutorialViewHolder>() {

    override fun getItemViewType(position: Int): Int = pageLayouts[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateTutorialViewHolder =
        UpdateTutorialViewHolder(
            LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        )

    override fun onBindViewHolder(holder: UpdateTutorialViewHolder, position: Int) {
        val lottieView =
            holder.itemView.findViewById<LottieAnimationView?>(R.id.anim_get_share_lottie) ?: return
        val fallbackFont = ResourcesCompat.getFont(holder.itemView.context, R.font.pretendard_bold)
            ?: Typeface.DEFAULT_BOLD

        lottieView.setFontAssetDelegate(object : FontAssetDelegate() {
            override fun fetchFont(fontFamily: String?): Typeface {
                return fallbackFont
            }
        })
    }

    override fun getItemCount(): Int = pageLayouts.size

    class UpdateTutorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
