package com.blogcode.producerconsumer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class Job<R> {
    private final CountDownLatch countDownLatch;
    private final Supplier<R> job;
    private JobStatus status;
    private R result;

    public Job(Supplier<R> job) {
        this.countDownLatch = new CountDownLatch(1);
        this.job = job;
        this.status = JobStatus.WAIT;
    }

    public Runnable started() {
        status = JobStatus.RUNNING;
        return () -> {
            result = job.get();
            status = JobStatus.END;
            countDownLatch.countDown();
        };
    }

    public Job<R> waitJobEnding(int maxWaitTime) {
        try {
            boolean jobEnded = countDownLatch.await(maxWaitTime, TimeUnit.SECONDS);
            if (!jobEnded) {
                this.status = JobStatus.FAIL;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        validateJobEnded();
        return this;
    }


    private void validateJobEnded() {
        if (status != JobStatus.END) {
            throw new RuntimeException("작업 지연 (결과를 알 수 없음)");
        }
    }

    public boolean isFail() {
        return status == JobStatus.FAIL;
    }

    private enum JobStatus {WAIT, RUNNING, END, FAIL}

}