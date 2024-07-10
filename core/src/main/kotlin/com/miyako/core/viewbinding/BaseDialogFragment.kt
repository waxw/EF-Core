package com.miyako.core.viewbinding

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding

abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment(), IBindingFragment<VB>, IFragment {
  private var _binding: VB? = null

  /**
   * You can't call [binding] after [onDestroyView]
   */
  override val binding: VB get() = _binding ?: throw IllegalArgumentException("Binding has been destroyed")

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = inflateBinding(layoutInflater)
    initOnce()
    dialog?.let {
      // requestFeature 必须放在 BarUtils.transparentStatusBar(it) 之前
      it.requestWindowFeature(Window.FEATURE_NO_TITLE)
      it.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
    return binding.root
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  open var onDismissListener: DialogInterface.OnDismissListener? = null
  open var onCancelListener: DialogInterface.OnCancelListener? = null

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    return super.onCreateDialog(savedInstanceState).apply {
      setOnKeyListener { _, keyCode, keyEvent ->
        if (keyCode == KeyEvent.KEYCODE_BACK && keyEvent.action == KeyEvent.ACTION_UP) {
          this@BaseDialogFragment.onBackPressed()
        } else {
          false
        }
      }
    }
  }

  override fun onResume() {
    super.onResume()
    dialog?.run {
      setCanceledOnTouchOutside(isCancelable)
      setCancelable(isCancelable)
    }
  }

  override fun onDismiss(dialog: DialogInterface) {
    onDismissListener?.onDismiss(dialog)
    super.onDismiss(dialog)
  }

  override fun onCancel(dialog: DialogInterface) {
    onCancelListener?.onCancel(dialog)
    super.onCancel(dialog)
  }

  override fun finish() {
    activity?.finish()
  }
}
