package com.miyako.core

import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.text.SpannableString
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.miyako.core.databinding.ActivityMainBinding
import com.miyako.core.recycler.SpaceItemDecoration

class MainActivity : AppCompatActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }

    val s = null
    val title = s.init {
      "init scope"
    }
    binding.tvTitle.text = title
    val target = "展示一段这几个字颜色不同的文字 weilanxiao"
    binding.tvSpannableString.text = SpannableString(target)
      .highlight("展示", Color.BLUE)
      .underline("不同")
      .strikethrough("一段")
      .background("颜色", Color.RED).strikethrough("文字").bold("几个")
      .italic("wei")
      .boldItalic("lan")
      .background("xiao", Color.BLUE)
      .clickable("xiao", Color.GREEN) {

      }
    val dateList = mutableListOf<String>()
    for (i in 0 until 20) {
      dateList.add("str: $i")
    }

    binding.rvList.run {
      layoutManager = LinearLayoutManager(context)
      adapter = StringAdapter(dateList)
      // ItemDecoration 可叠加
      // addItemDecoration(SpaceItemDecoration(Rect(16.dp, 2.dp, 16.dp, 2.dp)))
      addItemDecoration(SpaceItemDecoration(Rect(0, 4.dp, 0.dp, 4.dp), 10.dp))
      addItemDecoration(SpaceItemDecoration(horizontal = 10.dp))
    }
    binding.rvListHorizontal.run {
      layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
      adapter = StringAdapter(dateList)
      addItemDecoration(SpaceItemDecoration(horizontal = 20.dp, vertical = 10.dp))
    }
  }
}
