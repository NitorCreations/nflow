package com.nitorcreations.nflow.tests.demo;

import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.moveToState;
import static com.nitorcreations.nflow.engine.workflow.definition.NextAction.stopInState;

import java.util.Locale;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nitorcreations.nflow.engine.workflow.definition.NextAction;
import com.nitorcreations.nflow.engine.workflow.definition.StateExecution;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowDefinition;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowSettings;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowState;
import com.nitorcreations.nflow.engine.workflow.definition.WorkflowStateType;

public class WordGeneratorWorkflow extends WorkflowDefinition<WordGeneratorWorkflow.State> {

  private static final Logger logger = LoggerFactory.getLogger(WordGeneratorWorkflow.class);

  public static enum State implements WorkflowState {
    start(WorkflowStateType.start), a(0.08167), b(0.01492), c(0.02782), d(0.04253), e(0.12702), f(0.02228), g(0.02015), h(0.06094), //
    i(0.0666), j(0.00153), k(0.00772), l(0.04025), m(0.02406), n(0.06749), o(0.07507), p(0.01929), q(0.00095), r(0.05987), //
    s(0.06327), t(0.09056), u(0.02758), v(0.00978), w(0.02360), x(0.00150), y(0.01974), z(0.00074), end(0.13012), //
    error(WorkflowStateType.end);

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
    public String getDescription() {
      return name();
    }
  }

  protected WordGeneratorWorkflow(String flowName, WorkflowSettings settings) {
    super(flowName, State.start, State.error, settings);
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
    this("wordGenerator", new WorkflowSettings.Builder().build());
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

  public NextAction start(StateExecution execution) {
    State newState = randState();
    return moveToState(newState, "Go to state " + newState);
  }

  public void error(StateExecution execution) {
    logger.error("Finished with error");
  }

  public NextAction end(StateExecution execution) {
    logger.info("Finished word: {}", execution.getVariable("word", "").toUpperCase(Locale.GERMAN));
    return stopInState(State.end, "Finished in end state");
  }

  protected NextAction update(StateExecution execution, String state) {
    State newState = randState();
    logger.info("{}->{}", state, newState.name());
    String word = execution.getVariable("word", "");
    execution.setVariable("word", word + state);
    return moveToState(newState, "Go to state " + newState);
  }

  public NextAction a(StateExecution execution) {
    return update(execution, "a");
  }

  public NextAction b(StateExecution execution) {
    return update(execution, "b");
  }

  public NextAction c(StateExecution execution) {
    return update(execution, "c");
  }

  public NextAction d(StateExecution execution) {
    return update(execution, "d");
  }

  public NextAction e(StateExecution execution) {
    return update(execution, "e");
  }

  public NextAction f(StateExecution execution) {
    return update(execution, "f");
  }

  public NextAction g(StateExecution execution) {
    return update(execution, "g");
  }

  public NextAction h(StateExecution execution) {
    return update(execution, "h");
  }

  public NextAction i(StateExecution execution) {
    return update(execution, "i");
  }

  public NextAction j(StateExecution execution) {
    return update(execution, "j");
  }

  public NextAction k(StateExecution execution) {
    return update(execution, "k");
  }

  public NextAction l(StateExecution execution) {
    return update(execution, "l");
  }

  public NextAction m(StateExecution execution) {
    return update(execution, "m");
  }

  public NextAction n(StateExecution execution) {
    return update(execution, "n");
  }

  public NextAction o(StateExecution execution) {
    return update(execution, "o");
  }

  public NextAction p(StateExecution execution) {
    return update(execution, "p");
  }

  public NextAction q(StateExecution execution) {
    return update(execution, "q");
  }

  public NextAction r(StateExecution execution) {
    return update(execution, "r");
  }

  public NextAction s(StateExecution execution) {
    return update(execution, "s");
  }

  public NextAction t(StateExecution execution) {
    return update(execution, "t");
  }

  public NextAction u(StateExecution execution) {
    return update(execution, "u");
  }

  public NextAction v(StateExecution execution) {
    return update(execution, "v");
  }

  public NextAction w(StateExecution execution) {
    return update(execution, "w");
  }

  public NextAction x(StateExecution execution) {
    return update(execution, "x");
  }

  public NextAction y(StateExecution execution) {
    return update(execution, "y");
  }

  public NextAction z(StateExecution execution) {
    return update(execution, "z");
  }

}
