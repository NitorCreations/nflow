package com.nitorcreations.nflow.engine.workflow.definition;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WorkflowSettingsTest {
  DateTime now = new DateTime(2014, 10, 22, 20, 44, 0);

  @Before
  public void setup() {
    DateTimeUtils.setCurrentMillisFixed(now.getMillis());
  }

  @After
  public void teardown() {
    DateTimeUtils.setCurrentMillisSystem();
  }

  @Test
  public void verifyConstantDefaultValues() {
    WorkflowSettings s = new WorkflowSettings.Builder().build();
    assertThat(s.immediateTransitionDelay, is(0));
    assertThat(s.shortTransitionDelay, is(30000));
    long delta = s.getShortTransitionActivation().getMillis() - currentTimeMillis() - 30000;
    assertThat(delta, greaterThanOrEqualTo(-1000L));
    assertThat(delta, lessThanOrEqualTo(0L));
  }

  @Test
  public void errorTransitionDelayIsBetweenMinAndMaxDelay() {
    int maxDelay = 1_000_000;
    int minDelay = 1000;
    WorkflowSettings s = new WorkflowSettings.Builder().setMinErrorTransitionDelay(minDelay).setMaxErrorTransitionDelay(maxDelay).build();
    long prevDelay = 0;
    for(int retryCount = 0 ; retryCount < 100 ; retryCount++) {
      long delay  = s.getErrorTransitionActivation(retryCount).getMillis() - now.getMillis();
      assertThat(delay, greaterThanOrEqualTo((long)minDelay));
      assertThat(delay, lessThanOrEqualTo((long)maxDelay));
      assertThat(delay, greaterThanOrEqualTo(prevDelay));
      prevDelay = delay;
    }
  }
}
