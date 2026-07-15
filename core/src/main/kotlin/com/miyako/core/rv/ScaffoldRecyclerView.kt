package com.miyako.core.rv

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.RecyclerView
import com.miyako.core.cast
import com.miyako.core.debugLog

class ScaffoldRecyclerView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

  private var isInflated: Boolean = false
  private var isReInflated: Boolean = false

  override fun onFinishInflate() {
    super.onFinishInflate()
    isInflated = true
  }

  fun reInflate() {
    if (isInflated.not()) throw IllegalStateException("reInflate must be call after onFinishInflate")
    var recyclerView: RecyclerView? = null
    for (i in 0 until childCount) {
      val child = getChildAt(i)
      if (child is RecyclerView) {
        recyclerView = child
        break
      }
    }
    if (recyclerView == null || recyclerView.adapter == null) return

    val originAdapter = recyclerView.adapter
    val concatAdapter = ConcatAdapter()
    // concatAdapter.

    for (i in 0 until childCount) {
      val child = getChildAt(i)
      if (child !is RecyclerView) {
        if (child.id == NO_ID) {
          throw IllegalArgumentException("Please set child view id")
        }
        val scaffold = bodyList.find { it.body.id == child.id } ?: object : ScaffoldBody<View>(child) {
          override val onBind: (View) -> Unit = {}
        }
        "id: ${scaffold.body.id}".debugLog()
        val fixedAdapter = ScaffoldBodyAdapter(recyclerView, scaffold)
        scaffold.adapter = fixedAdapter
        concatAdapter.addAdapter(fixedAdapter)
      } else if (i != childCount - 1) {
        throw IllegalArgumentException("RecyclerView must be the last child")
      }
    }

    concatAdapter.addAdapter(originAdapter!!)
    recyclerView.adapter = concatAdapter
    "rv: ${recyclerView.adapter}".debugLog()
    removeViews(0, childCount - 1)
    isReInflated = true
  }

  fun refresh() {
    if (isInflated.not() || isReInflated.not()) return
    getChildAt(0).cast<RecyclerView> {
      "adapter: ${adapter}".debugLog()
      adapter?.notifyDataSetChanged()
    }
  }

  private val bodyList = mutableListOf<ScaffoldBody<View>>()

  fun <V : View> addScaffoldBody(listener: ScaffoldBody<V>) {
    bodyList.add(listener as ScaffoldBody<View>)
  }

  fun <V : View> removeScaffoldBody(listener: ScaffoldBody<V>) {
    bodyList.remove(listener as ScaffoldBody<View>)
  }

  fun clearAllScaffoldBody() {
    bodyList.clear()
  }
}
