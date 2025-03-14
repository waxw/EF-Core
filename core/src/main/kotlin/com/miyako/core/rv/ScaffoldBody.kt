package com.miyako.core.rv

import android.view.View
import android.view.ViewGroup

abstract class ScaffoldBody<V: View>(
  val body: View,
) {
  lateinit var adapter: ScaffoldBodyAdapter
  open val changeRecycled: () -> Boolean = { false }
  open val onEnable: () -> Boolean = { true }
  open val onCreate: (ViewGroup) -> View = { body }
  abstract val onBind: (V) -> Unit
}
