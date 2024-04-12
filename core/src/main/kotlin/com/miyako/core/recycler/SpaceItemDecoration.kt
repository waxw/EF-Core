package com.miyako.core.recycler

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * 可叠加生效，不会改变 ViewHolder 最终大小。
 * 比如 RV 纵向列表宽度为 400px，item_layout 写死宽度为 320px，
 * 设置 horizontal 为 30，则最终 left 为 30，right 为 400-320-30 = 50，且布局靠左，不受 rv的 gravity 影响
 * 设置 horizontal 为 40，则最终 left 为 40，right 为 400-320-40 = 40
 * 设置 horizontal 为 50，则最终 left 为 50，right 为 400-320-50 = 30
 */
class SpaceItemDecoration(
  private val spaceRect: Rect,
  private val start: Int = 0,
  private val end: Int = 0,
) : RecyclerView.ItemDecoration() {

  constructor(space: Int, start: Int = 0, end: Int = 0) : this(
    Rect(space, space, space, space),
    start,
    end,
  )

  constructor(
    horizontal: Int = 0,
    vertical: Int = 0,
    start: Int = 0,
    end: Int = 0,
  ) : this(
    Rect(horizontal, vertical, horizontal, vertical),
    start,
    end,
  )

  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    super.getItemOffsets(outRect, view, parent, state)
    parent.adapter?.let {
      (parent.layoutManager as? LinearLayoutManager)?.let { layoutManger ->
        val isHorizontal = layoutManger.canScrollHorizontally()
        val idx = parent.getChildAdapterPosition(view)
        outRect.top = if (isHorizontal.not() && idx == 0) start else spaceRect.top
        outRect.bottom = if (isHorizontal.not().not() && idx == it.itemCount - 1) end else spaceRect.bottom
        outRect.left = if (isHorizontal && idx == 0) start else spaceRect.left
        outRect.right = if (isHorizontal && idx == it.itemCount - 1) end else spaceRect.right
      }
    }
  }
}
