package com.miyako.core.viewbinding

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.viewbinding.ViewBinding
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

sealed interface IBinding<VB : ViewBinding> {
  val binding: VB

  fun IBinding<*>.inflateBinding(inflater: LayoutInflater): VB {
    var method: Method?
    var clazz: Class<*> = javaClass
    while (clazz.superclass != null) {
      method = clazz.filterBindingMethod()
      if (method == null) {
        clazz = clazz.superclass
      } else {
        @Suppress("UNCHECKED_CAST")
        return method.invoke(null, inflater) as VB
      }
    }
    error("No Binding type argument found.")
  }

  private fun Class<*>.filterBindingMethod(): Method? {
    return (genericSuperclass as? ParameterizedType)?.actualTypeArguments
      ?.asSequence()
      ?.filterIsInstance<Class<*>>()
      ?.firstOrNull { it.simpleName.endsWith("Binding") }
      ?.getDeclaredMethod("inflate", LayoutInflater::class.java)
      ?.also { it.isAccessible = true }
  }
}

sealed interface IBindingFragment<VB : ViewBinding> : IBinding<VB> {
  @MainThread
  fun initOnce() {
  }
}

inline fun <reified VB : ViewBinding> inflate(
  parent: ViewGroup,
  attachToParent: Boolean = false,
  inflater: LayoutInflater = LayoutInflater.from(parent.context)
): VB {
  return inflate(VB::class.java, parent, attachToParent, inflater) as VB
}

fun inflate(
  javaClass: Class<*>,
  parent: ViewGroup,
  attachToParent: Boolean,
  inflater: LayoutInflater
): ViewBinding {
  return javaClass.takeIf { it.simpleName.endsWith("Binding") }
    ?.getMethod("inflate", LayoutInflater::class.java, ViewGroup::class.java, Boolean::class.java)
    ?.also { it.isAccessible = true }
    ?.invoke(null, inflater, parent, attachToParent) as? ViewBinding
    ?: error("It isn't binding class.")
}
