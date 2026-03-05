package com.noh.stpclient.config;

import io.micrometer.context.ContextSnapshot;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the @Async executor with a TaskDecorator that propagates
 * Micrometer trace context (traceId / spanId) and MDC to async threads.
 *
 * Without this, @Async threads start with an empty MDC, so traceId and
 * spanId are missing from log lines produced inside async methods.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(runnable -> {
            // Capture full context (trace + MDC) on the calling thread
            ContextSnapshot snapshot = ContextSnapshot.captureAll();
            return () -> {
                // Restore context on the async thread for the duration of the task
                try (ContextSnapshot.Scope ignored = snapshot.setThreadLocals()) {
                    runnable.run();
                }
            };
        });
        executor.setThreadNamePrefix("audit-async-");
        executor.initialize();
        return executor;
    }
}
