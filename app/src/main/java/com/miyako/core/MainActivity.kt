package com.miyako.core

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.util.Log
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.miyako.core.databinding.ActivityMainBinding
import com.miyako.core.viewbinding.inflate
import kotlinx.coroutines.delay

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
    inflate<ActivityMainBinding>(window.decorView as ViewGroup)
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
      val s = measureExecuteNano("ns") {
        setLanguage("zh,CN")
        // return@setOnClickListener
        return@measureExecuteNano
      }
    }

    binding.btnChangeDefault.setOnClickListener {
      lifecycleScope.launchDefault {
        cnt++
        val s = measureSuspendMillis("click") {
          // setLanguage("en,US")
          // return@setOnClickListener 1
          Log.d("miyako", "measureExecuteMillis")
          if (cnt % 2 == 0) {
            return@measureSuspendMillis run {
              delay(3000)
              -1
            }
          }
          delay(1000)
          1
        }
        Log.d("miyako", "res: $s")
      }
    }
  }

  private var cnt = 0

  private fun setLanguage(code: String) {
    if (this.changeLanguage(code) != null) {
      "success: $code".debugLog()
    }
    sp.edit().putString("set_language", code).commit()
    recreate()
  }
}
