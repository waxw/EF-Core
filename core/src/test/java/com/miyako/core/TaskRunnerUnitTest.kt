package com.miyako.core

import com.miyako.core.task.AttemptsExhaustedException
import com.miyako.core.task.ExecutionPhase
import com.miyako.core.task.ExecutionResult
import com.miyako.core.task.ObserverSource
import com.miyako.core.task.TaskRunner
import com.miyako.core.task.TaskTimeoutException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.cancellation.CancellationException

class TaskRunnerUnitTest {

  @Test
  fun retry_execute_returns_first_success() = runTest {
    var attemptCount = 0

    val data = TaskRunner.retry(maxAttempts = 3) {
      attemptCount++
      "done"
    }.execute()

    assertEquals("done", data)
    assertEquals(1, attemptCount)
  }

  @Test
  fun retry_retries_unmatched_failure_and_runs_beforeRetry_in_order() = runTest {
    val failure = IllegalStateException("retry")
    val events = mutableListOf<String>()

    val result = TaskRunner.retry(maxAttempts = 2, intervalMs = 100) {
      events += "supplier"
      if (events.count { it == "supplier" } == 1) throw failure
      "done"
    }.onAttemptFailure {
      events += "attemptFailure"
    }.beforeRetry { context ->
      events += "beforeRetry"
      assertEquals(2, context.nextAttempt)
      assertSame(failure, context.previousFailure)
      assertEquals(ExecutionPhase.BEFORE_RETRY, context.metrics.phase)
      assertNull(context.metrics.attemptStartTime)
    }.onAttemptSuccess {
      events += "attemptSuccess"
    }.executeResult()

    assertTrue(result is ExecutionResult.Success)
    assertEquals(
      listOf("supplier", "attemptFailure", "beforeRetry", "supplier", "attemptSuccess"),
      events,
    )
  }

  @Test
  fun abortOn_stops_matching_failure_and_execute_rethrows_it() = runTest {
    val failure = IllegalArgumentException("abort")
    var attempts = 0

    val thrown = runCatching {
      TaskRunner.retry(maxAttempts = 3) {
        attempts++
        throw failure
      }.abortOn<IllegalArgumentException>().execute()
    }.exceptionOrNull()

    assertSame(failure, thrown)
    assertEquals(1, attempts)
  }

  @Test
  fun abortOn_predicate_failure_preserves_attempt_failure_as_suppressed() = runTest {
    val attemptFailure = IllegalArgumentException("attempt")
    val ruleFailure = IllegalStateException("rule")

    val result = TaskRunner.retry(maxAttempts = 3) {
      throw attemptFailure
    }.abortOn<IllegalArgumentException> {
      throw ruleFailure
    }.executeResult()

    assertTrue(result is ExecutionResult.Failure)
    result as ExecutionResult.Failure
    assertSame(ruleFailure, result.throwable)
    assertSame(attemptFailure, result.throwable.suppressed.single())
  }

  @Test
  fun unmatched_failure_exhausts_with_final_failure() = runTest {
    val failures = listOf(IllegalStateException("first"), IllegalStateException("last"))
    var attempt = 0

    val result = TaskRunner.retry(maxAttempts = 2) {
      throw failures[attempt++]
    }.executeResult()

    assertTrue(result is ExecutionResult.Exhausted)
    result as ExecutionResult.Exhausted
    assertSame(failures.last(), result.lastThrowable)
    assertEquals(2, result.metrics.executionCount)
  }

  @Test
  fun execute_maps_exhausted_to_exception_with_cause() = runTest {
    val failure = IllegalStateException("last")

    val thrown = runCatching {
      TaskRunner.retry(maxAttempts = 1) {
        throw failure
      }.execute()
    }.exceptionOrNull()

    assertTrue(thrown is AttemptsExhaustedException)
    assertSame(failure, thrown?.cause)
  }

  @Test
  fun poll_uses_ordered_or_short_circuit_stop_conditions() = runTest {
    val conditions = mutableListOf<String>()

    val result = TaskRunner.poll(maxAttempts = 3) {
      42
    }.stopWhen<Int> {
      conditions += "first"
      false
    }.stopWhen<Int> {
      conditions += "second"
      assertEquals(ExecutionPhase.STOP_CONDITION, it.metrics.phase)
      true
    }.stopWhen<Int> {
      conditions += "third"
      true
    }.executeResult()

    assertTrue(result is ExecutionResult.Success)
    assertEquals(listOf("first", "second"), conditions)
  }

  @Test
  fun poll_without_stopWhen_fails_before_supplier_starts() = runTest {
    var supplierCalled = false
    val runner = TaskRunner.poll(maxAttempts = 2) {
      supplierCalled = true
      1
    }

    val error = runCatching { runner.executeResult() }.exceptionOrNull()

    assertTrue(error is IllegalStateException)
    assertFalse(supplierCalled)
  }

  @Test
  fun retry_rejects_stopWhen_immediately() {
    val error = runCatching {
      TaskRunner.retry { 1 }.stopWhen<Int> { true }
    }.exceptionOrNull()

    assertTrue(error is IllegalStateException)
  }

  @Test
  fun stopWhen_failure_terminates_without_abortOn_or_retry() = runTest {
    val conditionFailure = IllegalStateException("condition")
    var attempts = 0
    var abortCalled = false

    val result = TaskRunner.poll(maxAttempts = 3) {
      attempts++
      1
    }.stopWhen<Int> {
      throw conditionFailure
    }.abortOn<IllegalStateException> {
      abortCalled = true
      true
    }.executeResult()

    assertTrue(result is ExecutionResult.Failure)
    result as ExecutionResult.Failure
    assertSame(conditionFailure, result.throwable)
    assertEquals(1, attempts)
    assertFalse(abortCalled)
  }

  @Test
  fun poll_exhaustion_after_successful_incomplete_attempt_has_no_failure() = runTest {
    val result = TaskRunner.poll(maxAttempts = 2) {
      "pending"
    }.stopWhen<String> {
      false
    }.executeResult()

    assertTrue(result is ExecutionResult.Exhausted)
    result as ExecutionResult.Exhausted
    assertNull(result.lastThrowable)
  }

  @Test
  fun beforeRetry_failure_terminates_without_abortOn_or_next_attempt() = runTest {
    val preparationFailure = IllegalStateException("prepare")
    var attempts = 0
    var abortCalled = false

    val result = TaskRunner.retry(maxAttempts = 3) {
      attempts++
      throw IllegalArgumentException("attempt")
    }.beforeRetry {
      throw preparationFailure
    }.abortOn<IllegalStateException> {
      abortCalled = true
      true
    }.executeResult()

    assertTrue(result is ExecutionResult.Failure)
    result as ExecutionResult.Failure
    assertSame(preparationFailure, result.throwable)
    assertEquals(1, attempts)
    assertFalse(abortCalled)
  }

  @Test
  fun beforeRetry_owned_timeout_is_failure() = runTest {
    val result = TaskRunner.retry(maxAttempts = 2) {
      throw IllegalStateException("retry")
    }.beforeRetry {
      withTimeout(1_000) {
        delay(2_000)
      }
    }.executeResult()

    assertTrue(result is ExecutionResult.Failure)
    result as ExecutionResult.Failure
    assertTrue(result.throwable is kotlinx.coroutines.TimeoutCancellationException)
    assertEquals(ExecutionPhase.BEFORE_RETRY, result.metrics.phase)
  }

  @Test
  fun stopWhen_owned_timeout_is_failure() = runTest {
    val result = TaskRunner.poll(maxAttempts = 2) {
      1
    }.stopWhen<Int> {
      withTimeout(1_000) {
        delay(2_000)
      }
      true
    }.executeResult()

    assertTrue(result is ExecutionResult.Failure)
    result as ExecutionResult.Failure
    assertTrue(result.throwable is kotlinx.coroutines.TimeoutCancellationException)
    assertEquals(ExecutionPhase.STOP_CONDITION, result.metrics.phase)
  }

  @Test
  fun supplier_owned_timeout_is_an_attempt_failure() = runTest {
    var attemptFailures = 0
    var timeoutCalled = false

    val result = TaskRunner.retry(maxAttempts = 2, timeoutMs = 5_000) {
      withTimeout(1_000) {
        delay(2_000)
      }
    }.onAttemptFailure {
      attemptFailures++
    }.onTimeout {
      timeoutCalled = true
    }.executeResult()

    assertTrue(result is ExecutionResult.Exhausted)
    assertEquals(2, attemptFailures)
    assertFalse(timeoutCalled)
  }

  @Test
  fun total_timeout_maps_to_result_and_exception() = runTest {
    val result = TaskRunner.retry(timeoutMs = 1_000) {
      delay(2_000)
    }.executeResult()

    assertTrue(result is ExecutionResult.Timeout)
    result as ExecutionResult.Timeout
    assertEquals(ExecutionPhase.ATTEMPT, result.metrics.phase)

    val thrown = runCatching {
      TaskRunner.retry(timeoutMs = 1_000) {
        delay(2_000)
      }.execute()
    }.exceptionOrNull()
    assertTrue(thrown is TaskTimeoutException)
  }

  @Test
  fun timeout_metrics_identify_initial_delay() = runTest {
    val result = TaskRunner.retry(delayMs = 2_000, timeoutMs = 1_000) { 1 }.executeResult()

    assertTrue(result is ExecutionResult.Timeout)
    result as ExecutionResult.Timeout
    assertEquals(ExecutionPhase.INITIAL_DELAY, result.metrics.phase)
    assertEquals(0, result.metrics.executionCount)
    assertNull(result.metrics.attemptStartTime)
  }

  @Test
  fun timeout_metrics_identify_retry_delay() = runTest {
    val result = TaskRunner.retry(maxAttempts = 2, intervalMs = 2_000, timeoutMs = 1_000) {
      throw IllegalStateException("retry")
    }.executeResult()

    assertTrue(result is ExecutionResult.Timeout)
    result as ExecutionResult.Timeout
    assertEquals(ExecutionPhase.RETRY_DELAY, result.metrics.phase)
    assertEquals(1, result.metrics.executionCount)
    assertNull(result.metrics.attemptStartTime)
  }

  @Test
  fun timeout_metrics_identify_beforeRetry() = runTest {
    val result = TaskRunner.retry(maxAttempts = 2, timeoutMs = 1_000) {
      throw IllegalStateException("retry")
    }.beforeRetry {
      delay(2_000)
    }.executeResult()

    assertTrue(result is ExecutionResult.Timeout)
    result as ExecutionResult.Timeout
    assertEquals(ExecutionPhase.BEFORE_RETRY, result.metrics.phase)
    assertNull(result.metrics.attemptStartTime)
  }

  @Test
  fun timeout_metrics_identify_stop_condition() = runTest {
    val result = TaskRunner.poll(maxAttempts = 2, timeoutMs = 1_000) {
      1
    }.stopWhen<Int> {
      delay(2_000)
      true
    }.executeResult()

    assertTrue(result is ExecutionResult.Timeout)
    result as ExecutionResult.Timeout
    assertEquals(ExecutionPhase.STOP_CONDITION, result.metrics.phase)
    assertEquals(1, result.metrics.executionCount)
  }

  @Test
  fun external_cancellation_notifies_in_order_and_rethrows_same_instance() = runTest {
    val cancellation = CancellationException("external")
    val events = mutableListOf<String>()

    val thrown = runCatching {
      TaskRunner.retry {
        throw cancellation
      }.onCancel {
        events += "cancel"
      }.onFinished {
        events += "finished"
      }.executeResult()
    }.exceptionOrNull()

    assertSame(cancellation, thrown)
    assertEquals(listOf("cancel", "finished"), events)
  }

  @Test
  fun success_notifications_follow_contract_order() = runTest {
    val events = mutableListOf<String>()

    val result = TaskRunner.poll(maxAttempts = 2) {
      events += "supplier"
      1
    }.onAttemptSuccess {
      events += "attemptSuccess"
    }.stopWhen<Int> {
      events += "stopWhen"
      true
    }.onSuccess {
      events += "success"
    }.onFinished {
      events += "finished"
    }.executeResult()

    assertTrue(result is ExecutionResult.Success)
    assertEquals(listOf("supplier", "attemptSuccess", "stopWhen", "success", "finished"), events)
  }

  @Test
  fun failure_notifications_follow_contract_order() = runTest {
    val events = mutableListOf<String>()

    val result = TaskRunner.retry(maxAttempts = 2) {
      events += "supplier"
      throw IllegalArgumentException("abort")
    }.onAttemptFailure {
      events += "attemptFailure"
    }.abortOn<IllegalArgumentException>().onFailure {
      events += "failure"
    }.onFinished {
      events += "finished"
    }.executeResult()

    assertTrue(result is ExecutionResult.Failure)
    assertEquals(listOf("supplier", "attemptFailure", "failure", "finished"), events)
  }

  @Test
  fun exhausted_and_timeout_notifications_run_before_finished() = runTest {
    val exhaustedEvents = mutableListOf<String>()
    val exhaustedResult = TaskRunner.retry(maxAttempts = 1) {
      throw IllegalStateException("exhaust")
    }.onExhausted {
      exhaustedEvents += "exhausted"
    }.onFinished {
      exhaustedEvents += "finished"
    }.executeResult()

    val timeoutEvents = mutableListOf<String>()
    val timeoutResult = TaskRunner.retry(timeoutMs = 1_000) {
      delay(2_000)
    }.onTimeout {
      timeoutEvents += "timeout"
    }.onFinished {
      timeoutEvents += "finished"
    }.executeResult()

    assertTrue(exhaustedResult is ExecutionResult.Exhausted)
    assertEquals(listOf("exhausted", "finished"), exhaustedEvents)
    assertTrue(timeoutResult is ExecutionResult.Timeout)
    assertEquals(listOf("timeout", "finished"), timeoutEvents)
  }

  @Test
  fun observer_failures_are_reported_without_changing_success() = runTest {
    val sources = mutableListOf<ObserverSource>()

    val result = TaskRunner.retry {
      "done"
    }.onAttemptSuccess {
      throw IllegalStateException("attempt observer")
    }.onSuccess {
      throw CancellationException("success observer")
    }.onFinished {
      throw IllegalStateException("finished observer")
    }.onObserverError {
      sources += it.source
      if (it.source == ObserverSource.ON_SUCCESS) throw IllegalStateException("reporter")
    }.executeResult()

    assertTrue(result is ExecutionResult.Success)
    assertEquals(
      listOf(
        ObserverSource.ON_ATTEMPT_SUCCESS,
        ObserverSource.ON_SUCCESS,
        ObserverSource.ON_FINISHED,
      ),
      sources,
    )
  }

  @Test
  fun runner_is_one_shot_and_rejects_changes_after_execution() = runTest {
    val runner = TaskRunner.retry { 1 }
    assertEquals(1, runner.execute())

    val executionError = runCatching { runner.executeResult() }.exceptionOrNull()
    val configurationError = runCatching { runner.onSuccess {} }.exceptionOrNull()

    assertTrue(executionError is IllegalStateException)
    assertTrue(configurationError is IllegalStateException)
  }

  @Test
  fun configuration_values_are_validated_at_construction() {
    assertTrue(
      runCatching { TaskRunner.retry(delayMs = -1) { 1 } }.exceptionOrNull() is IllegalArgumentException,
    )
    assertTrue(
      runCatching { TaskRunner.retry(intervalMs = -1) { 1 } }.exceptionOrNull() is IllegalArgumentException,
    )
    assertTrue(
      runCatching { TaskRunner.retry(maxAttempts = 0) { 1 } }.exceptionOrNull() is IllegalArgumentException,
    )
    assertTrue(
      runCatching { TaskRunner.retry(timeoutMs = -1) { 1 } }.exceptionOrNull() is IllegalArgumentException,
    )
  }
}
