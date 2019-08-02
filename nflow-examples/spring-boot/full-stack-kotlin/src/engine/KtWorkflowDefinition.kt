package nflow.kotlin.engine

import io.nflow.engine.workflow.definition.AbstractWorkflowDefinition
import io.nflow.engine.workflow.definition.WorkflowState
import kotlin.reflect.KProperty1
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.memberProperties

@Suppress("UNCHECKED_CAST")
open class KtWorkflowDefinition<S : WorkflowState>(_type: String,
                                                   _initialState: S,
                                                   _errorState: S) :
        AbstractWorkflowDefinition<S>(_type, _initialState, _errorState) {

    private val allStates: Set<S>

    init {
        val companion = initialState::class.companionObjectInstance
                ?: throw IllegalArgumentException("No companion object")
        allStates = initialState::class.companionObject?.memberProperties
                ?.map { it as KProperty1<Any, S> }
                ?.map { it.get(companion) }
                ?.toSet()
                ?: emptySet()

    }

    override fun getStates() = allStates
}
