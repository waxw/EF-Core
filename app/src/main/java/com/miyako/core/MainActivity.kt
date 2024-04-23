package com.miyako.core

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.miyako.core.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

  private val sp by unsafeLazy { getSharedPreferences("language", MODE_PRIVATE) }

  override fun attachBaseContext(newBase: Context?) {
    val language = newBase?.getSharedPreferences("language", MODE_PRIVATE)?.getString("set_language", "")
    "language: $language".debugLog()
    val base = newBase?.changeLanguage(language) ?: newBase
    super.attachBaseContext(base)
  }

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

    binding.btnChangeChinese.setOnClickListener {
      setLanguage("zh,CN")
    }

    binding.btnChangeDefault.setOnClickListener {
      setLanguage("en,US")
    }
  }

  private fun setLanguage(code: String) {
    if (this.changeLanguage(code) != null) {
      "success: $code".debugLog()
    }
    sp.edit().putString("set_language", code).commit()
    recreate()
  }
}
