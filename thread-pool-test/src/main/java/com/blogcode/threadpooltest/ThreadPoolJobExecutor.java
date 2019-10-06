package com.blogcode.threadpooltest;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Function;

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

    public void executeJob(Function<Integer, List<Runnable>> receiveJob) throws InterruptedException {
        validateThreadPool();

        for (Runnable runnable : getJob(receiveJob)) {
            executeJob(runnable);
        }
    }

    private void executeJob(Runnable job) throws InterruptedException {
        semaphore.acquire();
        executor.execute(() -> {
            job.run();
            semaphore.release();
        });
    }

    private List<Runnable> getJob(Function<Integer, List<Runnable>> receiveJob) throws InterruptedException {
        semaphore.acquire();
        try {
            final int availableCount = executor.getMaxPoolSize() - executor.getActiveCount();
            return receiveJob.apply(availableCount);
        } catch (RuntimeException e) {
            return new ArrayList<>();
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
}
