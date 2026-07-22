package com.miyako.core

import com.miyako.core.task.TaskRunner
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test
import java.lang.Exception
import kotlin.random.Random

class TaskRunnerUnitTest {

  private val testDispatcher = StandardTestDispatcher()
  private val testScope = TestScope(testDispatcher)

  private val random = Random(System.currentTimeMillis())

  private fun mainJob(): Int {
    return random.nextInt(0, 100)
  }

  private fun <T> TaskRunner<T>.logExecution() = apply {
    result {
      "${it.executionMetrics}-${it.data}".debugLog()
    }.beforeRetry {
      "retry: ${it.executionMetrics}".debugLog()
    }
  }

  @Test
  fun test_execution_stopWhen() = runTest {
    val result = mutableListOf<Int>()
    val job = TaskRunner {
      mainJob()
    }.stopWhen<Int> {
      if (it.data < 20) {
        result.add(it.data)
        true
      } else false
    }.stopWhen<Int> {
      if (it.data >= 20) {
        result.add(it.data)
        true
      } else false
    }.logExecution().launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertTrue(result.isNotEmpty())
  }

  @Test
  fun test_execution_retry() = runTest {
    val result = mutableListOf<Int>()
    val job = TaskRunner(maxAttempts = 3) {
      mainJob()
    }.stopWhen<Int> {
      if (it.data > 100) {
        true
      } else {
        result.add(it.data)
        false
      }
    }.logExecution().launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertTrue(result.isNotEmpty())
  }

  @Test
  fun test_execution_exhausted() = runTest {
    var exhaustedCall = false
    val job = TaskRunner(maxAttempts = 3) {
      mainJob()
    }.stopWhen<Int> {
      // 不可能条件
      it.data > 100
    }.exhausted {
      exhaustedCall = true
    }.logExecution().launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertTrue(exhaustedCall)
  }

  @Test
  fun test_execution_supplier_fail() = runTest {
    var failWhenCalled = false
    val job = TaskRunner {
      mainJob()
      throw IllegalStateException("Max")
    }.stopWhen<Int> {
      false
    }.failWhen<Exception> {
      it.data.message == "Max".apply {
        failWhenCalled = true
      }
    }.logExecution().launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertTrue(failWhenCalled)
  }

  @Test
  fun test_execution_failWhen_false_retry_until_exhausted() = runTest {
    var attemptCount = 0
    var exhaustedCalled = false
    val job = TaskRunner(maxAttempts = 3) {
      attemptCount++
      throw IllegalStateException("Retry")
    }.failWhen<IllegalStateException> {
      false
    }.exhausted {
      exhaustedCalled = true
    }.launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertEquals(3, attemptCount)
    Assert.assertTrue(exhaustedCalled)
  }

  @Test
  fun test_execution_failWhen_true_stop_before_exhausted() = runTest {
    var attemptCount = 0
    var exhaustedCalled = false
    val job = TaskRunner(maxAttempts = 3) {
      attemptCount++
      throw IllegalStateException("Stop")
    }.failWhen<IllegalStateException> {
      true
    }.exhausted {
      exhaustedCalled = true
    }.launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertEquals(1, attemptCount)
    Assert.assertFalse(exhaustedCalled)
  }

  @Test
  fun test_execution_failWhenFallback_handles_unmatched_error() = runTest {
    var attemptCount = 0
    var fallbackCalled = false
    var exhaustedCalled = false
    val job = TaskRunner(maxAttempts = 3) {
      attemptCount++
      throw IllegalArgumentException("Fallback")
    }.failWhen<IllegalStateException> {
      true
    }.failWhenFallback {
      fallbackCalled = true
      false
    }.exhausted {
      exhaustedCalled = true
    }.launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertEquals(3, attemptCount)
    Assert.assertTrue(fallbackCalled)
    Assert.assertTrue(exhaustedCalled)
  }

  @Test
  fun test_execution_result_fail() = runTest {
    var failWhenCalled = false
    val job = TaskRunner {
      mainJob()
    }.result {
      throw IllegalStateException("Max")
    }.failWhen<Exception> {
      it.data.message == "Max".apply {
        failWhenCalled = true
      }
    }.launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertTrue(failWhenCalled)
  }

  @Test
  fun test_execution_stopWhen_fail() = runTest {
    var failWhenCalled = false
    val job = TaskRunner {
      mainJob()
    }.stopWhen<Int> {
      throw IllegalStateException("Max")
      false
    }.failWhen<Exception> {
      it.data.message == "Max".apply {
        failWhenCalled = true
      }
    }.logExecution().launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertTrue(failWhenCalled)
  }

  @Test
  fun test_execution_timeout() = runTest {
    var timeoutCalled = false
    val job = TaskRunner(0, 500, maxAttempts = 3, timeoutMs = 1000) {
      mainJob()
    }.timeout {
      timeoutCalled = true
    }.logExecution().launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertTrue(timeoutCalled)
  }

  @Test
  fun test_execution_beforeRetry_fail() = runTest {
    var throwableCalled = false
    val job = TaskRunner(maxAttempts = 2) {
      mainJob()
    }.stopWhen<Int> {
      false
    }.beforeRetry {
      throw IllegalStateException("Before")
    }.throwable {
      throwableCalled = it.data.message == "Before"
    }.launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertTrue(throwableCalled)
  }

  @Test
  fun test_execution_finally_fail() = runTest {
    var throwableCalled = false
    val job = TaskRunner {
      mainJob()
    }.stopWhen<Int> {
      true
    }.finally {
      throw IllegalStateException("Finally")
    }.throwable {
      throwableCalled = it.data.message == "Finally"
    }.launchIn(this, Dispatchers.IO)

    job.join()
    Assert.assertTrue(throwableCalled)
  }

  @Test
  fun test_execution_throwable_fail() = runTest {
    val handlerError = IllegalStateException("Handler")
    val exceptionHandler = CoroutineExceptionHandler { _, _ -> }
    val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler) + exceptionHandler)
    var completionError: Throwable? = null
    val job = TaskRunner {
      throw IllegalStateException("Max")
    }.throwable {
      throw handlerError
    }.launchIn(scope, StandardTestDispatcher(testScheduler))

    job.invokeOnCompletion {
      completionError = it
    }
    job.join()
    Assert.assertSame(handlerError, completionError)
  }

}
