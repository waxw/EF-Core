package com.miyako.mvi

import com.miyako.core.debugLog
import com.miyako.core.ksp.mvi.Action
import com.miyako.core.ksp.mvi.DispatchAction
import com.miyako.core.ksp.mvi.DelegateDispatch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

@DelegateDispatch
class DemoViewModel: MviViewModel<DemoViewModel.UiState, DemoViewModel.UiEffect, DemoViewModel.UiAction>() {

  data class UiState(val state: Int): BaseUiState

  sealed class UiEffect: BaseUiEffect {
    data class Toast(val msg: String): UiEffect()
  }

  sealed class UiAction: BaseUiAction {
    data class Click(val click: String): UiAction()
    data object Back: UiAction()
  }

  override val _uiState: MutableStateFlow<UiState>
    get() = MutableStateFlow(UiState(-1))
  override val _uiEffect: MutableSharedFlow<UiEffect>
    get() = MutableSharedFlow()

  @DispatchAction(UiAction::class)
  fun dispatchDemo(action: UiAction) {

  }

  @Action
  fun click(click: UiAction.Click) {
    "click: $click".debugLog()
  }

  @Action
  fun back(back: UiAction.Back) {
    "back: $back".debugLog()
  }
}
