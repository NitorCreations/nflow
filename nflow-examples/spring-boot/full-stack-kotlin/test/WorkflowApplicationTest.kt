package nflow.kotlin

import io.nflow.engine.service.WorkflowInstanceService
import io.nflow.engine.workflow.instance.WorkflowInstance
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory
import jakarta.inject.Inject
import nflow.kotlin.workflow.ExampleWorkflow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class WorkflowApplicationTest {

    @Inject
    lateinit var workflowInstanceService: WorkflowInstanceService

    @Inject
    lateinit var workflowInstanceFactory: WorkflowInstanceFactory

    @Test
    fun workflowExecutesSuccessfully() {
        val id = workflowInstanceService.insertWorkflowInstance(
            workflowInstanceFactory.newWorkflowInstanceBuilder()
                .setType(ExampleWorkflow.TYPE)
                .setExternalId("integration-test-${System.currentTimeMillis()}")
                .putStateVariable(ExampleWorkflow.VAR_COUNTER, 0)
                .build()
        )

        val deadline = System.currentTimeMillis() + 10_000
        var instance: WorkflowInstance? = null
        while (System.currentTimeMillis() < deadline) {
            instance = workflowInstanceService.getWorkflowInstance(id, emptySet(), null)
            if (instance.status == WorkflowInstance.WorkflowInstanceStatus.inProgress) break
            Thread.sleep(200)
        }

        assertNotNull(instance)
        assertEquals(WorkflowInstance.WorkflowInstanceStatus.inProgress, instance!!.status)
    }
}
