package com.example.agenticorderservice.shared.api;

import java.util.concurrent.*;
import org.springframework.core.task.AsyncTaskExecutor;

/** Decorates the selected executor without owning its pool or scheduling policy. */
public final class CorrelatedTaskExecutor implements AsyncTaskExecutor {
    private final AsyncTaskExecutor delegate;

    public CorrelatedTaskExecutor(AsyncTaskExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public void execute(Runnable task) {
        delegate.execute(RequestCorrelationContext.capture().wrap(task));
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(RequestCorrelationContext.capture().wrap(task));
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(RequestCorrelationContext.capture().wrap(task));
    }

    @Override
    public CompletableFuture<Void> submitCompletable(Runnable task) {
        return delegate.submitCompletable(RequestCorrelationContext.capture().wrap(task));
    }

    @Override
    public <T> CompletableFuture<T> submitCompletable(Callable<T> task) {
        return delegate.submitCompletable(RequestCorrelationContext.capture().wrap(task));
    }
}
