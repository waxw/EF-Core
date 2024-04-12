package com.miyako.core.viewbinding

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.viewbinding.ViewBinding

abstract class BaseActivity<VB : ViewBinding> : ComponentActivity(), IBinding<VB> {

  override lateinit var binding: VB

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // ViewBinding
    binding = inflateBinding(layoutInflater).also {
      setContentView(it.root)
    }
    initData()
    initView()
  }

  abstract fun initData()

  abstract fun initView()
}
