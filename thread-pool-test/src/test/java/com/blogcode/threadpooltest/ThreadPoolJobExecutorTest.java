package com.blogcode.threadpooltest;

import org.junit.After;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

public class ThreadPoolJobExecutorTest {

    private static CountDownLatch countDownLatch;
    private static Map<String, Boolean> threadHitMap;
    private ThreadPoolJobExecutor threadPoolJobExecutor;

    @After
    public void tearDown() {
        threadPoolJobExecutor.destroy();
    }

    @Test
    public void 스레드_10개로_10개의_Job을_동시에_수행할_수_있다() throws InterruptedException {
        //given
        final int theadCount = 10;
        threadPoolJobExecutor = jobThreadPool(theadCount);
        threadHitMap = threadHitMap(theadCount);
        countDownLatch = new CountDownLatch(theadCount);
        List<Runnable> jobs = jobs(theadCount);

        //when
        threadPoolJobExecutor.executeJob((count) -> jobs);

        countDownLatch.await();

        //then
        assertThread(theadCount);
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

    private List<Runnable> jobs(int count) {
        List<Runnable> jobs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            jobs.add(started(new TestJob()));
        }
        return jobs;
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

        void hit() throws InterruptedException {
            countDownLatch.countDown();
            threadHitMap.put(Thread.currentThread().getName(), true);
            countDownLatch.await();
        }
    }

}