package com.blogcode.producerconsumer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

public class ThreadPoolJobExecutorTest {

    private ThreadPoolJobExecutor threadPoolJobExecutor;
    private static Map<String, Boolean> threadHitMap;

    @After
    public void tearDown() {
        threadPoolJobExecutor.destroy();
    }

    @Test
    public void 스레드_두개로_두개의_Job을_동시에_수행할_수_있다() throws InterruptedException {
        //given
        threadPoolJobExecutor = jobThreadPool(10);
        threadHitMap = threadHitMap(10);

        TestJob testJob = new TestJob(10);

        //when
        for (int i = 1; i <= 10; i++) {
            threadPoolJobExecutor.executeJob(() -> started(testJob));
        }

        testJob.waitEndJob();

        //then
        assertThread(10);
    }

    private void assertThread(int count) {
        for (int i = 1; i <= count; i++) {
            assertThat(threadHitMap.get("test-" + i)).isTrue();
        }
    }

    private Map<String, Boolean> threadHitMap(int count) {
        threadHitMap = new HashMap<>();
        for (int i = 1; i <= count; i++) {
            threadHitMap.put("test-" + i, false);
        }
        return threadHitMap;
    }


    private Runnable started(TestJob testJob) {
        return () -> {
            try {
                testJob.hit();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        };
    }

    private ThreadPoolJobExecutor jobThreadPool(int poolSize) {
        return new ThreadPoolJobExecutor("test", poolSize, poolSize, poolSize);
    }

    static private class TestJob {
        private CountDownLatch countDownLatch;

        TestJob(int threadCount) {
            this.countDownLatch = new CountDownLatch(threadCount);
        }

        void hit() throws InterruptedException {
            countDownLatch.countDown();
            threadHitMap.put(Thread.currentThread().getName(), true);
            countDownLatch.await();
        }

        void waitEndJob() throws InterruptedException {
            countDownLatch.await();
        }

    }
}