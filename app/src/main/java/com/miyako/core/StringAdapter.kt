package com.miyako.core

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miyako.core.databinding.ItemStringLayoutBinding

class StringAdapter(private val dataList: List<String>): RecyclerView.Adapter<StringAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(ItemStringLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
  }

  override fun getItemCount(): Int {
    return dataList.size
  }

  private var size: Pair<Int, Int> = Pair(0, 0)

  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.binding.tvContent.text = dataList[position]
    holder.itemView.runWhenReady {
      if (holder.itemView.width != size.first || holder.itemView.height != size.second) {
        size = Pair(holder.itemView.width, holder.itemView.height)
        "view: $size".debugLog("space")
      }
    }
  }

  class ViewHolder(val binding: ItemStringLayoutBinding): RecyclerView.ViewHolder(binding.root)
}
