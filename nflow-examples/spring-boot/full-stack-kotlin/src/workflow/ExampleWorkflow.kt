package nflow.kotlin.workflow

import io.nflow.engine.workflow.definition.NextAction
import io.nflow.engine.workflow.definition.NextAction.moveToStateAfter
import io.nflow.engine.workflow.definition.StateExecution
import io.nflow.engine.workflow.definition.WorkflowState
import io.nflow.engine.workflow.definition.WorkflowStateType
import io.nflow.engine.workflow.definition.WorkflowStateType.manual
import io.nflow.engine.workflow.definition.WorkflowStateType.start
import nflow.kotlin.engine.KtWorkflowDefinition
import nflow.kotlin.engine.getVar
import nflow.kotlin.workflow.State.Companion.error
import nflow.kotlin.workflow.State.Companion.repeat
import org.joda.time.DateTime.now
import org.springframework.stereotype.Component

class State(private val name: String, private val type: WorkflowStateType, private val description: String) : WorkflowState {
    companion object {
        val repeat = State("repeat", start, "Repeating state")
        val error = State("error", manual, "Error state")
    }

    override fun getDescription() = description
    override fun getType() = type
    override fun name() = name
}

@Component
class ExampleWorkflow : KtWorkflowDefinition<State>(TYPE, repeat, error) {

    init {
        permit(repeat, repeat)
    }

    fun repeat(execution: StateExecution): NextAction {
        val counter: Int = execution.getVar(VAR_COUNTER)
        println("Counter: $counter")
        execution.setVariable(VAR_COUNTER, counter + 1)
        return moveToStateAfter(repeat, now().plusSeconds(10), "Next iteration")
    }

    companion object {
        const val TYPE = "repeatingWorkflow"
        const val VAR_COUNTER = "VAR_COUNTER"
    }
}
