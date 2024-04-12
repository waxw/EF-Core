package com.miyako.core

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miyako.core.databinding.ItemGridStringLayoutBinding

class GridStringAdapter(private val dataList: List<String>): RecyclerView.Adapter<GridStringAdapter.ViewHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
    return ViewHolder(ItemGridStringLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false))
  }

  override fun getItemCount(): Int {
    return dataList.size
  }


  override fun onBindViewHolder(holder: ViewHolder, position: Int) {
    holder.binding.tvContent.text = dataList[position]
    holder.itemView.runWhenReady {
      val size = Pair(holder.itemView.width, holder.itemView.height)
      "pos: $position, view: $size, ${dataList[position]}".debugLog("space")
    }
  }

  class ViewHolder(val binding: ItemGridStringLayoutBinding): RecyclerView.ViewHolder(binding.root)
}
