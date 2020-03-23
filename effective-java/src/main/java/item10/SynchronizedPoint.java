package item10;

import java.util.concurrent.CountDownLatch;

public class SynchronizedPoint {
    private boolean threadStop = false;

    public void start(CountDownLatch latch) {

        Thread backgroundThread = new Thread(() -> {

            if (threadStop) {

            }
        });

        backgroundThread.start();
    }

    public void stop() {
        threadStop = true;
    }
}
