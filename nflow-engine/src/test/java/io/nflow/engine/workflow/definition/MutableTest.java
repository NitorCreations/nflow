package io.nflow.engine.workflow.definition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.Test;

public class MutableTest {
  @Test
  public void methodsWork() {
    Mutable<Integer> m = new Mutable<>();
    assertThat(m.getVal(), nullValue());
    assertThat(m.toString(), is("null"));
    m.setVal(1);
    assertThat(m.getVal(), is(1));
    assertThat(m.toString(), is("1"));
  }

  @Test
  public void constructorWorks() {
    Mutable<Integer> m = new Mutable<>(2);
    assertThat(m.getVal(), is(2));
  }

  @Test
  public void fieldAccessWorks() {
    Mutable<Integer> m = new Mutable<>(3);
    assertThat(m.val, is(3));
    m.val = 4;
    assertThat(m.getVal(), is(4));
  }
}
