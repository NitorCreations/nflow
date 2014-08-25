package com.nitorcreations.nflow.tests.demo;

import java.util.Locale;
import java.util.Random;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

public class WordGeneratorWorkflow extends
    WorkflowDefinition<WordGeneratorWorkflow.State> {
  private static final Logger logger = LoggerFactory
      .getLogger(WordGeneratorWorkflow.class);

  public static enum State implements WorkflowState {
    a(0.08167), b(0.01492), c(0.02782), d(0.04253), e(0.12702), f(0.02228), g(
        0.02015), h(0.06094), i(0.0666), j(0.00153), k(0.00772), l(0.04025), m(
        0.02406), n(0.06749), o(0.07507), p(0.01929), q(0.00095), r(0.05987), s(
        0.06327), t(0.09056), u(0.02758), v(0.00978), w(0.02360), x(0.00150), y(
        0.01974), z(0.00074), end(0.13012), error(WorkflowStateType.end);

    final double fraction;
    private final WorkflowStateType type;

    private State(WorkflowStateType type) {
      this(0, type);
    }

    private State(double fraction) {
      this(fraction, WorkflowStateType.normal);
    }

    private State(double fraction, WorkflowStateType type) {
      this.fraction = fraction;
      this.type = type;
    }

    @Override
    public WorkflowStateType getType() {
      return type;
    }

    @Override
    public String getName() {
      return name();
    }

    @Override
    public String getDescription() {
      return name();
    }
  }

  protected WordGeneratorWorkflow(String flowName, WorkflowSettings settings) {
    super(flowName, randState(), State.error, settings);
    for (State originState : State.values()) {
      for (State targetState : State.values()) {
        if (originState == State.end || originState == State.error || targetState == State.error) {
          continue;
        }
        permit(originState, targetState);
      }
    }
  }

  public WordGeneratorWorkflow() {
    this("wordGenerator", new WorkflowSettings(null));
  }
  protected static State randState() {
    Random random = new Random();
    double sum = 0;
    for (State v : State.values()) {
      sum += v.fraction;
    }
    double rand = random.nextDouble();
    double threshold = 0;
    for (State v : State.values()) {
      threshold += v.fraction / sum;
      if (threshold > rand) {
        return v;
      }
    }
    return State.values()[State.values().length - 1];
  }

  public void error(StateExecution execution) {
    execution.setNextState(State.error);
    logger.error("Finished with error");
  }

  public void end(StateExecution execution) {
    execution.setNextState(State.end);
    logger.info("Finished word: {}", execution.getVariable("word", "").toUpperCase(Locale.GERMAN));
  }

  protected void update(StateExecution execution, String state) {
    State newState = randState();
    logger.info("{}->{}", state, newState.name());
    String word = execution.getVariable("word", "");
    execution.setVariable("word", word + state);
    execution.setNextState(newState);
    execution.setNextActivation(DateTime.now());
  }

  public void a(StateExecution execution) {
    update(execution, "a");
  }

  public void b(StateExecution execution) {
    update(execution, "b");
  }

  public void c(StateExecution execution) {
    update(execution, "c");
  }

  public void d(StateExecution execution) {
    update(execution, "d");
  }

  public void e(StateExecution execution) {
    update(execution, "e");
  }

  public void f(StateExecution execution) {
    update(execution, "f");
  }

  public void g(StateExecution execution) {
    update(execution, "g");
  }

  public void h(StateExecution execution) {
    update(execution, "h");
  }

  public void i(StateExecution execution) {
    update(execution, "i");
  }

  public void j(StateExecution execution) {
    update(execution, "j");
  }

  public void k(StateExecution execution) {
    update(execution, "k");
  }

  public void l(StateExecution execution) {
    update(execution, "l");
  }

  public void m(StateExecution execution) {
    update(execution, "m");
  }

  public void n(StateExecution execution) {
    update(execution, "n");
  }

  public void o(StateExecution execution) {
    update(execution, "o");
  }

  public void p(StateExecution execution) {
    update(execution, "p");
  }

  public void q(StateExecution execution) {
    update(execution, "q");
  }

  public void r(StateExecution execution) {
    update(execution, "r");
  }

  public void s(StateExecution execution) {
    update(execution, "s");
  }

  public void t(StateExecution execution) {
    update(execution, "t");
  }

  public void u(StateExecution execution) {
    update(execution, "u");
  }

  public void v(StateExecution execution) {
    update(execution, "v");
  }

  public void w(StateExecution execution) {
    update(execution, "w");
  }

  public void x(StateExecution execution) {
    update(execution, "x");
  }

  public void y(StateExecution execution) {
    update(execution, "y");
  }

  public void z(StateExecution execution) {
    update(execution, "z");
  }

}
