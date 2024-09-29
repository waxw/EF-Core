package com.miyako.core.recycler

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.StaggeredGridLayoutManager

class EdgeSpace(
  val mainAxisSpace: Int,
  val crossAxisSpace: Int = mainAxisSpace,
  val edgeSpace: Rect
) {

  companion object {
    const val NO_SPACING = 0
  }

  constructor(mainAxisSpace: Int, crossAxisSpace: Int = mainAxisSpace, vertical: Int = 0, horizontal: Int = 0) : this(
    mainAxisSpace,
    crossAxisSpace,
    Rect(horizontal, vertical, horizontal, vertical)
  )

  constructor(space: Int, left: Int = 0, top: Int = 0, right: Int = 0, bottom: Int = 0) : this(
    space,
    space,
    Rect(left, top, right, bottom)
  )
}

class EdgeSpaceItemDecoration(
  val space: EdgeSpace
) : ItemDecoration() {

  override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
    // Separate layout type
    when (val layoutManager = parent.layoutManager) {
      is GridLayoutManager -> {
        GridLayoutSpacing.makeLayoutSpacing(
          space,
          outRect,
          parent.getChildAdapterPosition(view),
          state.itemCount,
          layoutManager.orientation,
          layoutManager.spanCount,
          layoutManager.reverseLayout
        )
      }

      is LinearLayoutManager -> {
        val linearOrientation = layoutManager.orientation

        // Flag whether item positioning is reversed (more like flipped) or not. So, if normally item is
        // written from left to right (horizontally), then it will be right to left (whatever item index is)
        // and if item is written from top to bottom (vertically), then it will be from bottom to top.
        val isReversed = layoutManager.reverseLayout xor layoutManager.stackFromEnd
        LinearLayoutSpacing.makeLayoutSpacing(
          space, outRect,
          parent.getChildAdapterPosition(view),
          state.itemCount,
          linearOrientation,
          isReversed
        )
      }

      is StaggeredGridLayoutManager -> { // Equals to GridLayoutManager but isn't the same on function level
        GridLayoutSpacing.makeLayoutSpacing(
          space,
          outRect,
          parent.getChildAdapterPosition(view),
          state.itemCount,
          layoutManager.orientation,
          layoutManager.spanCount,
          layoutManager.reverseLayout
        )
      }
    }
  }
}

object LinearLayoutSpacing {

  fun makeLayoutSpacing(
    edgeSpace: EdgeSpace,
    outRect: Rect,
    position: Int,
    itemCount: Int,
    @RecyclerView.Orientation orientation: Int,
    isReversed: Boolean
  ) {
    val isEnd = position == (itemCount - 1)
    val isStart = position == 0

    when (orientation) {
      RecyclerView.HORIZONTAL -> {
        with(outRect) {
          val startSpace = if (isStart) edgeSpace.edgeSpace.left else edgeSpace.mainAxisSpace
          val endSpace = if (isEnd) edgeSpace.edgeSpace.right else EdgeSpace.NO_SPACING
          left = if (isReversed) endSpace else startSpace
          top = edgeSpace.edgeSpace.top
          right = if (isReversed) startSpace else endSpace
          bottom = edgeSpace.edgeSpace.bottom
        }
      }

      RecyclerView.VERTICAL -> {
        with(outRect) {
          val startSpace = if (isStart) edgeSpace.edgeSpace.top else edgeSpace.mainAxisSpace
          val endSpace = if (isEnd) edgeSpace.edgeSpace.bottom else EdgeSpace.NO_SPACING
          left = edgeSpace.edgeSpace.left
          top = if (isReversed) endSpace else startSpace
          right = edgeSpace.edgeSpace.right
          bottom = if (isReversed) startSpace else endSpace
        }
      }
    }
  }
}

object GridLayoutSpacing {

  fun makeLayoutSpacing(
    edgeSpace: EdgeSpace,
    outRect: Rect,
    position: Int,
    itemCount: Int,
    @RecyclerView.Orientation orientation: Int,
    spanCount: Int,
    isReversed: Boolean
  ) {
    // Opposite of spanCount (find layout depth)
    val subsideCount = if (itemCount % spanCount == 0) {
      itemCount / spanCount
    } else {
      (itemCount / spanCount) + 1
    }

    when (orientation) {
      RecyclerView.HORIZONTAL -> {
        with(outRect) {
          val mainAxisPos = position / spanCount
          val crossAxisPos = position % spanCount

          // Conditions in row and column
          val isStartMain = mainAxisPos == 0
          val isStartCross = crossAxisPos == 0
          val isEndMain = mainAxisPos == subsideCount - 1
          val isEndCross = crossAxisPos == spanCount - 1

          val startSpace = if (isStartMain) edgeSpace.edgeSpace.left else edgeSpace.mainAxisSpace
          val endSpace = if (isEndMain) edgeSpace.edgeSpace.right else EdgeSpace.NO_SPACING
          getCrossAxis(edgeSpace.edgeSpace.top != 0, crossAxisPos, edgeSpace.crossAxisSpace, spanCount).let {
            top = it.first
            bottom = it.second
          }
          left = if (isReversed) endSpace else startSpace
          right = if (isReversed) startSpace else endSpace
        }
      }

      RecyclerView.VERTICAL -> {
        with(outRect) {
          val mainAxisPos = position / spanCount
          val crossAxisPos = position % spanCount

          val isStartMain = mainAxisPos == 0
          val isStartCross = crossAxisPos == 0
          val isEndMain = mainAxisPos == subsideCount
          val isEndCross = crossAxisPos == spanCount - 1

          val startSpace = if (isStartMain) edgeSpace.edgeSpace.top else edgeSpace.mainAxisSpace
          val endSpace = if (isEndMain) edgeSpace.edgeSpace.bottom else EdgeSpace.NO_SPACING

          getCrossAxis(edgeSpace.edgeSpace.left != 0, crossAxisPos, edgeSpace.crossAxisSpace, spanCount).let {
            left = it.first
            right = it.second
          }
          top = if (isReversed) endSpace else startSpace
          bottom = if (isReversed) startSpace else endSpace
        }
      }
    }
  }

  /**
   * 开始结束间距为 0，Li = i * d / n，Ri = (n - i - 1) * d / n其中，i表示第 i 项（从 0 开始），d 表示中间间隔，n 表示共 n 项
   * 开始结束间距为 d，Li = (n - i) * d / n，Ri = (i + 1) * d / n其中，i表示第 i 项（从 0 开始），d 表示中间间隔，n 表示共 n 项
   */
  private fun getCrossAxis(enableEdge: Boolean, idx: Int, space: Int, spanCount: Int): Pair<Int, Int> {
    return if (enableEdge) {
      (spanCount - idx) * space / spanCount to (idx + 1) * space / spanCount
    } else {
      idx * space / spanCount to (spanCount - idx - 1) * space / spanCount
    }
  }
}
