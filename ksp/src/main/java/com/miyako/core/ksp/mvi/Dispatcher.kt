package com.miyako.core.ksp.mvi

import kotlin.reflect.KClass

interface IDelegateDispatcher<T, R> {
  val p: KClass<*>
  val r: KClass<*>
  var defReturn: Return<T, R>
  fun dispatch(arg: T): R
}

abstract class DefReturn<T, R>(
  val tag: String
) {
  abstract val p: KClass<*>
  abstract val r: KClass<*>
  abstract val getDefault: Return<T, R>
}

fun interface Return<in T, R> {
  fun apply(t: T): R
}

inline fun <reified T, reified R> defReturn(tag: String, crossinline block: (T) -> R): DefReturn<T, R> {
  return object : DefReturn<T, R>(tag) {
    override val p = T::class
    override val r = R::class
    override val getDefault = Return<T, R> {
      block.invoke(it)
    }
  }
}

object Dispatcher {

  private val bindMap: MutableMap<Any, IDelegateDispatcher<*, *>> = mutableMapOf()

  private fun generatedDispatcher(obj: Any): IDelegateDispatcher<*, *> {
    val clazz = obj::class
    val className = clazz.qualifiedName!! + "Dispatcher"
    val newClazz = Class.forName(className).kotlin

    return newClazz.constructors.firstOrNull {
      val constructor = it.parameters.firstOrNull()
      constructor != null && clazz == constructor.type.classifier
    }?.let {
      it.call(obj) as IDelegateDispatcher<*, *>
    } ?: throw IllegalArgumentException()
  }

  fun bind(obj: Any) {
    if (bindMap.contains(obj).not()) {
      bindMap[obj] = generatedDispatcher(obj)
    }
  }

  fun unbind(obj: Any) {
    if (bindMap.contains(obj)) {
      bindMap.remove(obj)
    }
  }

  fun <T, R> dispatch(obj: Any, arg: T): R {
    return (bindMap[obj] as? IDelegateDispatcher<T, R>)?.dispatch(arg) ?: throw IllegalArgumentException()
  }
}
