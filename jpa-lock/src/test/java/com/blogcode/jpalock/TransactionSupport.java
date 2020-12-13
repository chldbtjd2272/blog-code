package com.blogcode.jpalock;

import org.springframework.transaction.annotation.Transactional;

public class TransactionSupport {

    @Transactional
    public void doTransaction(InterruptedRunnable runnable){
        try {
            runnable.run();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    interface InterruptedRunnable {
        void run() throws InterruptedException;
    }
}
