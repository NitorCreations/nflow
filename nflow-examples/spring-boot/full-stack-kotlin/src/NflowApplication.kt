package nflow.kotlin

import io.nflow.engine.service.WorkflowInstanceService
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory
import io.nflow.rest.config.RestConfiguration
import nflow.kotlin.workflow.ExampleWorkflow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import javax.annotation.PostConstruct
import javax.inject.Inject


@Import(RestConfiguration::class)
@Configuration
class WorkflowAppConfig

@SpringBootApplication
class WorkflowApplication {

    @Inject
    private lateinit var workflowInstanceFactory: WorkflowInstanceFactory

    @Inject
    private lateinit var workflowInstances: WorkflowInstanceService

    @PostConstruct
    fun createExampleWorkflowInstance() {
        workflowInstances.insertWorkflowInstance(workflowInstanceFactory.newWorkflowInstanceBuilder()
                .setType(ExampleWorkflow.TYPE)
                .setExternalId("example")
                .putStateVariable(ExampleWorkflow.VAR_COUNTER, 0)
                .build())
    }
}

fun main(args: Array<String>) {
    runApplication<WorkflowApplication>(*args)
}
