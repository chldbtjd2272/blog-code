package com.blogcode.producerconsumer;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Optional;
import java.util.concurrent.Semaphore;

public class ThreadPoolJobExecutor {
    private final ThreadPoolTaskExecutor executor;
    private final Semaphore semaphore;
    private boolean enableUse = true;

    public ThreadPoolJobExecutor(String processorName, int corePoolSize, int maxPoolSize, int queueCapacity) {
        this.executor = new ThreadPoolTaskExecutor();
        this.semaphore = new Semaphore(maxPoolSize);

        this.executor.setThreadNamePrefix(processorName + "-");
        this.executor.setCorePoolSize(corePoolSize);
        this.executor.setMaxPoolSize(maxPoolSize);
        this.executor.setQueueCapacity(queueCapacity);
        this.executor.setWaitForTasksToCompleteOnShutdown(true);
        this.executor.initialize();
    }

    public void executeJob(ThrowingSupplier<Runnable> receiveJob) throws InterruptedException {
        validateThreadPool();

        Optional<Runnable> maybeJob = getJob(receiveJob);
        if (maybeJob.isPresent()) {
            executeJob(maybeJob.get());
        }
    }

    private void executeJob(Runnable job) throws InterruptedException {
        semaphore.acquire();
        executor.execute(() -> {
            job.run();
            semaphore.release();
        });
    }

    private Optional<Runnable> getJob(ThrowingSupplier<Runnable> receiveJob) throws InterruptedException {
        semaphore.acquire();
        try {
            return Optional.ofNullable(receiveJob.get());
        } catch (RuntimeException e) {
            return Optional.empty();
        } finally {
            semaphore.release();
        }
    }

    private void validateThreadPool() {
        if (!enableUse) {
            throw new IllegalStateException("Already destroy threadPool");
        }
    }

    public void destroy() {
        this.semaphore.acquireUninterruptibly(this.executor.getMaxPoolSize());
        this.executor.destroy();
        this.enableUse = false;
    }

    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws InterruptedException;
    }

}
