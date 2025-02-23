package kr.baeksuk.urlbox.util.util

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class TagTouchCallback(private val listener: OnTagTouchHelperListener) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {

        // 드래그 방향
        val dragFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        // 스와이프 방향
        val swipeFlags = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        // 드래그 이동을 만드는 함수
        return makeMovementFlags(dragFlags, swipeFlags)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        listener.onItemMove(viewHolder.layoutPosition, target.layoutPosition)
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {



    }
}