package com.miyako.core.viewbinding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB : ViewBinding> : Fragment(), IBindingFragment<VB>, IFragment {
  private var _binding: VB? = null

  /**
   * You can't call [binding] after [onDestroyView]
   */
  override val binding: VB get() = _binding ?: throw IllegalArgumentException("Binding has been destroyed")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activity?.onBackPressedDispatcher?.addCallback(this) {
      isEnabled = onBackPressed()
      // 不拦截时先把监听关闭再开启，防止递归
      if (!isEnabled) activity?.onBackPressed()
      isEnabled = true
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = inflateBinding(layoutInflater)
    initOnce()
    return binding.root
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  override fun finish() {
    activity?.finish()
  }
}

sealed interface IFragment {
  /**
   * 返回键事件的监听
   * @return 是否需要拦截返回事件，默认不拦截
   */
  fun onBackPressed(): Boolean = false

  /**
   * 结束 activity
   */
  fun finish()
}
