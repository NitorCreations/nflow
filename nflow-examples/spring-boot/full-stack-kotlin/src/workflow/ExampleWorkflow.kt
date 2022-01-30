package nflow.kotlin.workflow

import io.nflow.engine.workflow.curated.State
import io.nflow.engine.workflow.definition.NextAction
import io.nflow.engine.workflow.definition.NextAction.moveToStateAfter
import io.nflow.engine.workflow.definition.StateExecution
import io.nflow.engine.workflow.definition.WorkflowDefinition
import io.nflow.engine.workflow.definition.WorkflowState
import io.nflow.engine.workflow.definition.WorkflowStateType
import io.nflow.engine.workflow.definition.WorkflowStateType.manual
import io.nflow.engine.workflow.definition.WorkflowStateType.start
import nflow.kotlin.engine.getVar
import org.joda.time.DateTime.now
import org.springframework.stereotype.Component

@Component
class ExampleWorkflow : WorkflowDefinition(TYPE, REPEAT, ERROR) {
    init {
        permit(REPEAT, REPEAT)
    }

    fun repeat(execution: StateExecution): NextAction {
        val counter: Int = execution.getVar(VAR_COUNTER)
        println("Counter: $counter")
        execution.setVariable(VAR_COUNTER, counter + 1)
        return moveToStateAfter(REPEAT, now().plusSeconds(10), "Next iteration")
    }

    companion object {
        const val TYPE = "repeatingWorkflow"
        const val VAR_COUNTER = "VAR_COUNTER"
        val REPEAT = State("repeat", start, "Repeating state")
        val ERROR = State("error", manual, "Error state")
    }
}
