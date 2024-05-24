package com.miyako.core.ksp.mvi

import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.jvm.isAccessible

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

  private val bindMap: MutableMap<Any, List<Pair<String, IDelegateDispatcher<*, *>>>> = mutableMapOf()

  private fun generatedDispatcher(
    obj: Any,
    defReturnArr: Array<out DefReturn<*, *>>
  ): List<Pair<String, IDelegateDispatcher<*, *>>> {
    val clazz = obj::class
    val buildObject = Class.forName("${clazz.qualifiedName!!}_Build")
    val buildList = buildObject.declaredFields.map {
      it.isAccessible = true
      it.get(null) as? String ?: ""
    }
    println("buildList: $buildList")
    val result = mutableListOf<Pair<String, IDelegateDispatcher<*, *>>>()
    buildList.forEach { className ->
      if (className.isNotEmpty()) {
        val delegateClass = Class.forName(className)
        delegateClass.kotlin.constructors.firstOrNull {
          val constructor = it.parameters.firstOrNull()
          constructor != null && clazz == constructor.type.classifier
        }?.let {
          val delegate = it.call(obj) as IDelegateDispatcher<*, *>
          defReturnArr.find {
            it.p == delegate.p && it.r == delegate.r
          }?.let {
            val field = delegateClass.kotlin.members.filterIsInstance<KMutableProperty<*>>().find { it.name == "defReturn" }
            if (field != null) {
              field.isAccessible = true
              println("set: ${it.tag}->$delegate")
              field.setter.call(delegate, it.getDefault)
            }
          }
          delegate
        }?.let {
          result.add(className to it)
        }
      }
    }
    return result
  }

  fun bind(obj: Any, vararg defReturn: DefReturn<*, *>) {
    if (bindMap.contains(obj).not()) {
      bindMap[obj] = generatedDispatcher(obj, defReturn)
    }
  }

  fun unbind(obj: Any) {
    if (bindMap.contains(obj)) {
      bindMap.remove(obj)
    }
  }

  inline fun <T, reified R> dispatch(obj: Any, arg: T): R {
    return dispatch(obj, arg, R::class)
  }

  fun <T, R> dispatch(obj: Any, arg: T, returnType: KClass<*>): R {
    val className = arg!!::class
    return bindMap[obj]?.find {
      println("dispatch: $className, $returnType")
      println("find: ${it.second.p}, ${it.second.r}")
      it.second.p.isInstance(arg) && it.second.r == returnType
    }?.let {
      (it.second as? IDelegateDispatcher<T, R>)?.dispatch(arg)
    } ?: throw IllegalArgumentException("Not Fond $className")
  }
}
