package com.miyako.core

import android.content.Context
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.SpannableString
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.miyako.core.bubble.BubblePosition
import com.miyako.core.bubble.dismissAllBubbles
import com.miyako.core.bubble.showBubble
import com.miyako.core.databinding.ActivityMainBinding
import com.miyako.core.dp as coreDp
import com.miyako.core.rv.ScaffoldBody
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
    setContentView(binding.root)
    ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
      val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
      v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
      insets
    }

    val s = null
    val title =
      s.orInit {
        "init scope"
      }
    binding.tvTitle.text = title
    val target = "展示一段这几个字颜色不同的文字 weilanxiao"
    binding.tvSpannableString.text =
      SpannableString(target)
        .highlight("展示", AndroidColor.BLUE)
        .underline("不同")
        .strikethrough("一段")
        .background("颜色", AndroidColor.RED).strikethrough("文字").bold("几个")
        .italic("wei")
        .boldItalic("lan")
        .background("xiao", AndroidColor.BLUE)
        .clickable("xiao", AndroidColor.GREEN) {
        }

    // binding.btnChangeChinese.setOnClickListener {
    //   val s = measureExecuteNano("ns") {
    //     setLanguage("zh,CN")
    //     // return@setOnClickListener
    //     return@measureExecuteNano
    //   }
    // }

    binding.btnChangeDefault.setOnClickListener {
      lifecycleScope.launchDefault {
        cnt++
        val s =
          measureSuspendMillis("click") {
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

    val dataList = mutableListOf<String>()

    repeat(50) {
      dataList.add("item: $it")
    }

    binding.rvList.adapter = RvAdapter(dataList)
    binding.rvList.layoutManager = LinearLayoutManager(this)

    var gone = false

    val buttonScaffold =
      object : ScaffoldBody<Button>(binding.btnChangeChinese) {
        override val onEnable = {
          gone.not().apply {
            "gone: $gone".debugLog()
          }
        }
        override val onBind = { button: Button ->
          button.setOnClickListener {
            binding.tvTitle.text = "Scaffold Body"
          }
        }
      }

    val goneScaffold =
      object : ScaffoldBody<Button>(binding.btnChangeDefault) {
        override val onBind = { button: Button ->
          button.setOnClickListener {
            gone = true
            buttonScaffold.adapter.notifyDataSetChanged()
            // binding.scaffold.refresh()
          }
        }
      }

    binding.scaffold.addScaffoldBody(buttonScaffold)
    binding.scaffold.addScaffoldBody(goneScaffold)

    binding.scaffold.reInflate()

    // 应用内气泡（View）演示：三个位置（TOP/CENTER/BOTTOM），连点可体验堆叠效果
    binding.btnBubble.setOnClickListener {
      showBubble {
        position = BubblePosition.TOP
        // SystemUI 交由外部处理：调用方获取状态栏高度并传入 margin 避让
        margin = statusBarHeight()
        content = { bubbleContent(it) }
        onClick = { "bubble clicked".debugLog() }
        onDismiss = { "bubble dismissed".debugLog() }
      }
    }

    binding.btnBubbleCenter.setOnClickListener {
      showBubble {
        position = BubblePosition.CENTER
        durationMs = 4000
        content = { bubbleContent(it) }
      }
    }

    binding.btnBubbleCustom.setOnClickListener {
      showBubble {
        position = BubblePosition.BOTTOM
        durationMs = 5000
        content = { bubbleContent(it) }
      }
    }
  }

  /** 状态栏高度：气泡不做 SystemUI 避让，由调用方获取高度后传入 margin */
  private fun statusBarHeight(): Int {
    val id = resources.getIdentifier("status_bar_height", "dimen", "android")
    return if (id != 0) resources.getDimensionPixelSize(id) else 0
  }

  /** 气泡自定义内容：容器无默认外观，样式（背景/圆角/边距）由调用方自绘 */
  private fun bubbleContent(parent: ViewGroup): View {
    return LinearLayout(parent.context).apply {
      orientation = LinearLayout.VERTICAL
      setPadding(20.coreDp, 16.coreDp, 20.coreDp, 16.coreDp)
      background = GradientDrawable().apply {
        cornerRadius = 12.coreDp.toFloat()
        setColor(0xE62B2B2B.toInt())
      }
      addView(
        TextView(context).apply {
          text = "应用内气泡"
          setTextColor(AndroidColor.WHITE)
          textSize = 15f
          setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
      )
      addView(
        TextView(context).apply {
          text = "自定义内容，支持任意 View。点击或左右滑动关闭。"
          setTextColor(0xB3FFFFFF.toInt())
          textSize = 12f
          setPadding(0, 4.coreDp, 0, 0)
        }
      )
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
