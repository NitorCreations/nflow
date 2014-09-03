package com.nitorcreations.nflow.tests.demo;

import static com.nitorcreations.nflow.engine.workflow.definition.NextState.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.NextState.stopInState;

import java.util.Locale;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nitorcreations.nflow.engine.workflow.definition.NextState;
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

  public NextState error(StateExecution execution) {
    logger.error("Finished with error");
    return stopInState(State.error, "Finished in error state");
  }

  public NextState end(StateExecution execution) {
    logger.info("Finished word: {}", execution.getVariable("word", "").toUpperCase(Locale.GERMAN));
    return stopInState(State.end, "Finished in end state");
  }

  protected NextState update(StateExecution execution, String state) {
    State newState = randState();
    logger.info("{}->{}", state, newState.name());
    String word = execution.getVariable("word", "");
    execution.setVariable("word", word + state);
    return moveToState(newState, "Go to state " + newState);
  }

  public NextState a(StateExecution execution) {
    return update(execution, "a");
  }

  public NextState b(StateExecution execution) {
    return update(execution, "b");
  }

  public NextState c(StateExecution execution) {
    return update(execution, "c");
  }

  public NextState d(StateExecution execution) {
    return update(execution, "d");
  }

  public NextState e(StateExecution execution) {
    return update(execution, "e");
  }

  public NextState f(StateExecution execution) {
    return update(execution, "f");
  }

  public NextState g(StateExecution execution) {
    return update(execution, "g");
  }

  public NextState h(StateExecution execution) {
    return update(execution, "h");
  }

  public NextState i(StateExecution execution) {
    return update(execution, "i");
  }

  public NextState j(StateExecution execution) {
    return update(execution, "j");
  }

  public NextState k(StateExecution execution) {
    return update(execution, "k");
  }

  public NextState l(StateExecution execution) {
    return update(execution, "l");
  }

  public NextState m(StateExecution execution) {
    return update(execution, "m");
  }

  public NextState n(StateExecution execution) {
    return update(execution, "n");
  }

  public NextState o(StateExecution execution) {
    return update(execution, "o");
  }

  public NextState p(StateExecution execution) {
    return update(execution, "p");
  }

  public NextState q(StateExecution execution) {
    return update(execution, "q");
  }

  public NextState r(StateExecution execution) {
    return update(execution, "r");
  }

  public NextState s(StateExecution execution) {
    return update(execution, "s");
  }

  public NextState t(StateExecution execution) {
    return update(execution, "t");
  }

  public NextState u(StateExecution execution) {
    return update(execution, "u");
  }

  public NextState v(StateExecution execution) {
    return update(execution, "v");
  }

  public NextState w(StateExecution execution) {
    return update(execution, "w");
  }

  public NextState x(StateExecution execution) {
    return update(execution, "x");
  }

  public NextState y(StateExecution execution) {
    return update(execution, "y");
  }

  public NextState z(StateExecution execution) {
    return update(execution, "z");
  }

}
