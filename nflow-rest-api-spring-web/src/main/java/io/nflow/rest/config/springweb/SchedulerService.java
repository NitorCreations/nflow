package io.nflow.rest.config.springweb;

import static org.slf4j.LoggerFactory.getLogger;

import io.nflow.engine.internal.executor.WorkflowInstanceExecutor;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

/**
 * Service to hold a Webflux Scheduler in order to make blocking calls.
 */
@Service
public class SchedulerService {

  private static final Logger logger = getLogger(SchedulerService.class);
  private Scheduler scheduler;
  private WorkflowInstanceExecutor workflowInstanceExecutor;

  @Inject
  public SchedulerService(WorkflowInstanceExecutor workflowInstanceExecutor, Environment env) {
    int dbPoolSize = env.getProperty("nflow.db.max_pool_size", Integer.class);
    int dispatcherCount = workflowInstanceExecutor.getThreadCount();
    int threadPoolSize = Math.max(dbPoolSize - dispatcherCount, 2);
    logger.info("Initializing REST API thread pool size to {}", threadPoolSize);
    this.scheduler = Schedulers.fromExecutor(Executors.newFixedThreadPool(threadPoolSize));
  }

  public <T> Mono<T> wrapBlocking(Callable<T> callable) {
    return Mono.fromCallable(callable).subscribeOn(this.scheduler);
  }
}
