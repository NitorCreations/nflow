package nflow.kotlin.engine

import io.nflow.engine.workflow.definition.StateExecution

inline fun <reified TYPE : Any> StateExecution.getVar(name: String): TYPE =
        getVariable(name, TYPE::class.java)

inline fun <reified TYPE : Any> StateExecution.getVar(name: String, defaultValue: TYPE): TYPE =
        getVariable(name, TYPE::class.java, defaultValue)