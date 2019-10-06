package com.blogcode.producerconsumer;

import org.springframework.util.ObjectUtils;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;


public class JobTaskBroker {

    private final BlockingQueue<Job> jobQueue;
    private final int maxWaitTime;

    public JobTaskBroker(int maxQueueSize, int maxWaitTime) {
        this.jobQueue = new LinkedBlockingQueue<>(maxQueueSize);
        this.maxWaitTime = maxWaitTime;
    }

    public Runnable getJob(int waitTimeSeconds) throws InterruptedException {
        Job job = jobQueue.poll(waitTimeSeconds, TimeUnit.SECONDS);
        if (ObjectUtils.isEmpty(job)) {
            return null;
        }

        return job.started();
    }

    public <R> R executeJob(Supplier<R> jobSupplier) {
        Job<R> job = new Job<>(jobSupplier);
        try {
            jobQueue.add(job);
        } catch (IllegalStateException e) {
            throw new RuntimeException("대기 중인 작업이 많음");
        }
        return job.waitJobEnding(maxWaitTime);
    }

    private enum JobStatus {WAIT, RUNNING, END}

    private class Job<R> {
        private final CountDownLatch countDownLatch;
        private final Supplier<R> job;
        private JobStatus status;
        private R result;

        private Job(Supplier<R> job) {
            this.countDownLatch = new CountDownLatch(1);
            this.job = job;
            this.status = JobStatus.WAIT;
        }

        private Runnable started() {
            status = JobStatus.RUNNING;
            return () -> {
                result = job.get();
                status = JobStatus.END;
                countDownLatch.countDown();
            };
        }

        private R waitJobEnding(int maxWaitTime) {
            try {
                boolean jobEnded = countDownLatch.await(maxWaitTime, TimeUnit.SECONDS);
                if (!jobEnded) {
                    cancelRequestTask();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            validateJobEnded();
            return result;
        }

        private void validateJobEnded() {
            if (status != JobStatus.END) {
                throw new RuntimeException("작업 지연 (결과를 알 수 없음)");
            }
        }

        private void cancelRequestTask() {
            boolean canceled = jobQueue.remove(this);
            if (!canceled) {
                validateJobEnded();
            }
        }

    }
}
