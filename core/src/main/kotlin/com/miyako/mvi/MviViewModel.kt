package com.miyako.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

abstract class MviViewModel<S: BaseUiState, E: BaseUiEffect, A: BaseUiAction>: ViewModel() {

  protected abstract val _uiState: MutableStateFlow<S>
  val uiState: StateFlow<S> = _uiState

  protected abstract val _uiEffect: MutableSharedFlow<E>
  val uiEffect: SharedFlow<E> = _uiEffect

  fun dispatch(action: A) {

  }
}