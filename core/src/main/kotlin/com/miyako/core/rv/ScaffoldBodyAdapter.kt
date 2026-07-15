package com.miyako.core.rv

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miyako.core.debugLog

class ScaffoldBodyAdapter(
  private val recyclerView: RecyclerView,
  private val scaffoldBody: ScaffoldBody<View>,
) : RecyclerView.Adapter<ScaffoldBodyAdapter.ViewHolder>() {

  private var isRecycled: Boolean = false

  fun setRecycled(recycled: Boolean, max: Int? = null) {
    isRecycled = recycled
    if (recycled) {
      recyclerView.recycledViewPool.setMaxRecycledViews(scaffoldBody.body.id, max ?: 5)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    "fixed onCreate".debugLog()
    val isRecycled = isRecycled
    val viewHolder = if (isRecycled) null else (recyclerView.recycledViewPool.getRecycledView(viewType) as? ViewHolder)
    return viewHolder ?: ViewHolder(scaffoldBody.onCreate.invoke(parent)).also {
      if (isRecycled.not()) {
        it.setIsRecyclable(false)
        recyclerView.recycledViewPool.putRecycledView(it)
      }
    }
  }

  override fun getItemViewType(position: Int) = scaffoldBody.body.id

  override fun getItemCount() = if (scaffoldBody.onEnable()) 1 else 0

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    "fixed onBind".debugLog()
    scaffoldBody.onBind(holder.itemView)
  }

  class ViewHolder(root: View) : RecyclerView.ViewHolder(root)
}
