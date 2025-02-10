package com.miyako.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

sealed interface DataState<out T> {
  data object Loading : DataState<Nothing>
  data object Initial : DataState<Nothing>
  data class Success<out T>(val data: T) : DataState<T>
  data class Failure(val throwable: Throwable) : DataState<Nothing>
}

class DataFactory<T>(
  private val _source: MutableSharedFlow<DataState<T>>
) {

  val source: SharedFlow<DataState<T>> = _source

  companion object {
    fun <T> create(): DataFactory<T> {
      return DataFactory(MutableStateFlow(DataState.Initial))
    }
  }

  suspend fun load(delay: Long = 0L, block: suspend () -> DataState<T>) {
    delay(delay)
    _source.emit(DataState.Loading)
    _source.emit(block())
  }

  suspend fun collect(collector: FlowCollector<DataState<T>>) {
    _source.collect(collector)
  }
}

abstract class DataRepository {

  suspend fun <T> request(
    block: suspend () -> T
  ): DataState<T> {
    return try {
      DataState.Success(block())
    } catch (e: Throwable) {
      handleException(e)
    }
  }

  open fun handleException(e: Throwable): DataState.Failure {
    return DataState.Failure(e)
  }
}
