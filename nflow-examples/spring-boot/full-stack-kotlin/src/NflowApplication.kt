package nflow.kotlin

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import io.nflow.engine.config.EngineConfiguration
import io.nflow.engine.config.NFlow
import io.nflow.engine.service.WorkflowInstanceService
import io.nflow.engine.workflow.instance.WorkflowInstanceFactory
import io.nflow.rest.config.NflowRestApiPropertiesConfiguration
import io.nflow.rest.config.RestConfiguration
import io.nflow.rest.config.RestConfiguration.REST_OBJECT_MAPPER
import jakarta.inject.Inject
import jakarta.inject.Named
import nflow.kotlin.workflow.ExampleWorkflow
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.runApplication
import org.springframework.context.annotation.*
import org.springframework.context.event.EventListener;

@Import(value = [EngineConfiguration::class, NflowRestApiPropertiesConfiguration::class])
@ComponentScan(value = ["io.nflow.rest"], excludeFilters = [ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [RestConfiguration::class])])
@Configuration
class WorkflowAppConfig

@SpringBootApplication
class WorkflowApplication {

    @Inject
    private lateinit var workflowInstanceFactory: WorkflowInstanceFactory

    @Inject
    private lateinit var workflowInstances: WorkflowInstanceService

    @EventListener(ApplicationReadyEvent::class)
    fun createExampleWorkflowInstance() {
        workflowInstances.insertWorkflowInstance(workflowInstanceFactory.newWorkflowInstanceBuilder()
                .setType(ExampleWorkflow.TYPE)
                .setExternalId("example")
                .putStateVariable(ExampleWorkflow.VAR_COUNTER, 0)
                .build())
    }

    @Primary
    @Bean
    @Named(REST_OBJECT_MAPPER)
    fun nflowRestObjectMapper(@NFlow nflowObjectMapper: ObjectMapper): ObjectMapper = nflowObjectMapper.copy().apply {
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
    }
}

fun main(args: Array<String>) {
    runApplication<WorkflowApplication>(*args)
}
