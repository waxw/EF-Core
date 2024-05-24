package com.miyako.core.ksp.mvi

interface IDelegateDispatcher<T, R> {
  fun dispatch(arg: T): R
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
