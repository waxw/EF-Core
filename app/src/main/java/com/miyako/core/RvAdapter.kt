package com.miyako.core

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.miyako.core.databinding.ItemRvBinding

class RvAdapter(
  val dataList: List<String>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {
  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): RecyclerView.ViewHolder {
    val root = ItemRvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ViewHolder(root)
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    "onBind: $position".debugLog()
    (holder as ViewHolder).binding.tvContent.text = dataList.get(position)
  }

  override fun getItemCount() = dataList.size

  class ViewHolder(val binding: ItemRvBinding): RecyclerView.ViewHolder(binding.root)
}
