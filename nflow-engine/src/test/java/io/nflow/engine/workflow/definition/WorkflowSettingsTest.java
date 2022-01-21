package io.nflow.engine.workflow.definition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.joda.time.DateTimeUtils.setCurrentMillisFixed;
import static org.joda.time.DateTimeUtils.setCurrentMillisSystem;

import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.event.Level;

import io.nflow.engine.exception.StateProcessExceptionHandling;

public class WorkflowSettingsTest {
  DateTime now = new DateTime(2014, 10, 22, 20, 44, 0);

  @BeforeEach
  public void setup() {
    setCurrentMillisFixed(now.getMillis());
  }

  @AfterEach
  public void teardown() {
    setCurrentMillisSystem();
  }

  @Test
  public void verifyConstantDefaultValues() {
    WorkflowSettings s = new WorkflowSettings.Builder().build();
    assertThat(s.immediateTransitionDelay, is(0));
    assertThat(s.shortTransitionDelay, is(30000));
    long delta = s.getShortTransitionActivation().getMillis() - currentTimeMillis() - 30000;
    assertThat(delta, greaterThanOrEqualTo(-1000L));
    assertThat(delta, lessThanOrEqualTo(0L));
    assertThat(s.historyDeletableAfter, is(Period.days(45)));
    assertThat(s.defaultPriority, is((short) 0));
  }

  @Test
  public void errorTransitionDelayIsBetweenMinAndMaxDelay() {
    int maxDelay = 1_000_000;
    int minDelay = 1000;
    WorkflowSettings s = new WorkflowSettings.Builder().setMinErrorTransitionDelay(minDelay).setMaxErrorTransitionDelay(maxDelay)
        .build();
    long prevDelay = 0;
    for (int retryCount = 0; retryCount < 100; retryCount++) {
      long delay = s.getErrorTransitionActivation(retryCount).getMillis() - now.getMillis();
      assertThat(delay, greaterThanOrEqualTo((long) minDelay));
      assertThat(delay, lessThanOrEqualTo((long) maxDelay));
      assertThat(delay, greaterThanOrEqualTo(prevDelay));
      prevDelay = delay;
    }
  }

  @Test
  public void getMaxSubsequentStateExecutionsReturns100ByDefault() {
    WorkflowSettings s = new WorkflowSettings.Builder().build();
    assertThat(s.getMaxSubsequentStateExecutions(TestState.BEGIN), is(equalTo(100)));
  }

  @Test
  public void getMaxSubsequentStateExecutionsReturnsValueDefinedForTheState() {
    int executionsDefault = 200;
    int executionsForBegin = 300;
    WorkflowSettings s = new WorkflowSettings.Builder().setMaxSubsequentStateExecutions(executionsDefault)
        .setMaxSubsequentStateExecutions(TestState.BEGIN, executionsForBegin).build();
    assertThat(s.getMaxSubsequentStateExecutions(TestState.BEGIN), is(equalTo(executionsForBegin)));
  }

  @Test
  public void getMaxSubsequentStateExecutionsReturnsGivenDefaultValueWhenNotDefinedForState() {
    int executionsDefault = 200;
    WorkflowSettings s = new WorkflowSettings.Builder().setMaxSubsequentStateExecutions(executionsDefault).build();
    assertThat(s.getMaxSubsequentStateExecutions(TestState.BEGIN), is(equalTo(executionsDefault)));
  }

  @Test
  public void deleteHistoryConditionIsApplied() {
    WorkflowSettings s = new WorkflowSettings.Builder().setDeleteHistoryCondition(() -> true).build();

    assertThat(s.deleteHistoryCondition.getAsBoolean(), is(true));
  }

  @Test
  public void oncePerDaySupplierWorks() {
    BooleanSupplier supplier = WorkflowSettings.Builder.oncePerDay();
    assertThat(supplier.getAsBoolean(), is(false));
    assertThat(supplier.getAsBoolean(), is(false));
    setCurrentMillisFixed(now.plusDays(1).withHourOfDay(5).getMillis());
    assertThat(supplier.getAsBoolean(), is(true));
    assertThat(supplier.getAsBoolean(), is(false));
    assertThat(supplier.getAsBoolean(), is(false));
    setCurrentMillisFixed(now.plusDays(2).withHourOfDay(5).getMillis());
    assertThat(supplier.getAsBoolean(), is(true));
    assertThat(supplier.getAsBoolean(), is(false));
    assertThat(supplier.getAsBoolean(), is(false));
  }

  @Test
  public void defaultExceptionAnalyzer() {
    WorkflowSettings s = new WorkflowSettings.Builder().build();

    StateProcessExceptionHandling exceptionHandling = s.analyzeExeption(TestState.BEGIN, new Throwable());
    assertThat(exceptionHandling.isRetryable, is(true));
    assertThat(exceptionHandling.logLevel, is(Level.ERROR));
    assertThat(exceptionHandling.logStackTrace, is(true));

    exceptionHandling = s.analyzeExeption(TestState.BEGIN, new NonRetryableException());
    assertThat(exceptionHandling.isRetryable, is(false));
    assertThat(exceptionHandling.logLevel, is(Level.ERROR));
    assertThat(exceptionHandling.logStackTrace, is(true));
  }

  @Test
  public void customExceptionAnalyzer() {
    WorkflowSettings s = new WorkflowSettings.Builder()
        .setExceptionAnalyzer((state, thrown) -> new StateProcessExceptionHandling.Builder()
        .setLogLevel(Level.INFO).setRetryable(true).setLogStackTrace(false).build()).build();

    StateProcessExceptionHandling exceptionHandling = s.analyzeExeption(TestState.BEGIN, new NonRetryableException());
    assertThat(exceptionHandling.isRetryable, is(true));
    assertThat(exceptionHandling.logLevel, is(Level.INFO));
    assertThat(exceptionHandling.logStackTrace, is(false));
  }

  @Test
  public void defaultExceptionAnalyzerIsUsedWhenCustomAnalyzerFails() {
    BiFunction<WorkflowState, Throwable, StateProcessExceptionHandling> failingExceptionAnalyzer = (state, thrown) -> {
      throw new IllegalStateException("fail");
    };
    WorkflowSettings s = new WorkflowSettings.Builder().setExceptionAnalyzer(failingExceptionAnalyzer).build();

    StateProcessExceptionHandling exceptionHandling = s.analyzeExeption(TestState.BEGIN, new NonRetryableException());
    assertThat(exceptionHandling.isRetryable, is(false));
    assertThat(exceptionHandling.logLevel, is(Level.ERROR));
    assertThat(exceptionHandling.logStackTrace, is(true));
  }

  @NonRetryable
  static class NonRetryableException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
}
