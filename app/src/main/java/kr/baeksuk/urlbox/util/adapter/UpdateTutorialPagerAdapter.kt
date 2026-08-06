package kr.baeksuk.urlbox.util.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class UpdateTutorialPagerAdapter(
    private val pageLayouts: List<Int>
) : RecyclerView.Adapter<UpdateTutorialPagerAdapter.UpdateTutorialViewHolder>() {

    override fun getItemViewType(position: Int): Int = pageLayouts[position]

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateTutorialViewHolder =
        UpdateTutorialViewHolder(
            LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        )

    override fun onBindViewHolder(holder: UpdateTutorialViewHolder, position: Int) = Unit

    override fun getItemCount(): Int = pageLayouts.size

    class UpdateTutorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
