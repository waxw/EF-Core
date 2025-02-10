package com.miyako.demo

import androidx.lifecycle.viewModelScope
import com.miyako.core.debugLog
import com.miyako.core.ksp.mvi.Action
import com.miyako.core.ksp.mvi.DelegateDispatch
import com.miyako.core.ksp.mvi.DispatchAction
import com.miyako.core.ksp.mvi.Dispatcher
import com.miyako.core.ksp.mvi.defReturn
import com.miyako.data.DataFactory
import com.miyako.mvi.MviViewModel
import com.miyako.mvi.UiAction
import com.miyako.mvi.UiEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@DelegateDispatch
class DemoViewModel : MviViewModel<DemoViewModel.UiState, UiEffect, UiAction>() {
  override val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)

  sealed class UiState : com.miyako.mvi.UiState {
    data object Loading : UiState()
    data object Error : UiState()
    data class Success(
      val groupId: Int,
      val list: List<String>
    ) : UiState()
  }

  sealed class UiAction : com.miyako.mvi.UiAction {
    data object Back : UiAction()
    data class Group(val settings: String) : UiAction()
    data class Item(val settings: Int) : UiAction()
  }

  init {
    val unitReturn = defReturn<UiAction, Unit>(tag = "Unit") {
      "def Unit".debugLog()
    }
    val intReturn = defReturn<UiAction, Int>(tag = "Int") {
      "def Int".debugLog()
      -233
    }
    Dispatcher.bind(this, unitReturn, intReturn)
  }

  @DispatchAction(UiAction::class)
  fun click(action: UiAction) {
    Dispatcher.dispatch<UiAction, Unit>(this, action)
  }

  @DispatchAction(UiAction::class, Int::class)
  fun clickAndReturn(action: UiAction): Int {
    return Dispatcher.dispatch<UiAction, Int>(this, action)
  }

  @Action
  fun clickGroup(action: UiAction.Group) {

  }

  @Action
  fun clickSettings(action: UiAction.Item) {
  }

  val dataFactory = DataFactory.create<NetResult<ArticlePageDto>>()
  private val netRepository = NetRepository()

  fun loadData() {
    viewModelScope.launch(Dispatchers.IO) {
      dataFactory.load(delay = 1000, netRepository::requestArticles)
    }
  }
}
