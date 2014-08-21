package com.nitorcreations.nflow.engine.workflow.definition;

import static org.joda.time.DateTimeUtils.currentTimeMillis;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;


public class WorkflowSettingsTest {
  @Test
  public void verifyConsantDefaultValues() {
    WorkflowSettings s = new WorkflowSettings(null);
    assertThat(s.getImmediateTransitionDelay(), is(0));
    assertThat(s.getShortTransitionDelay(), is(30000));
    long delta = s.getShortTransitionActivation().getMillis() - currentTimeMillis() - 30000;
    assertThat(delta, greaterThanOrEqualTo(-1000L));
    assertThat(delta, lessThanOrEqualTo(0L));
  }
}
