package com.miyako.core.ksp.mvi

import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
annotation class Action

@Target(AnnotationTarget.FUNCTION)
annotation class DispatchAction(val param: KClass<*>, val returnType: KClass<*> = Unit::class)

@Target(AnnotationTarget.CLASS)
annotation class DelegateDispatch
