package com.miyako.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class MviViewModel<S : UiState, E : UiEffect, A : UiAction> : ViewModel() {

  protected abstract val _uiState: MutableStateFlow<S>
  val uiState: StateFlow<S> by lazy { _uiState }

  protected open val _uiEffect: MutableSharedFlow<E> = MutableSharedFlow()
  val uiEffect: SharedFlow<E> by lazy { _uiEffect }
}
