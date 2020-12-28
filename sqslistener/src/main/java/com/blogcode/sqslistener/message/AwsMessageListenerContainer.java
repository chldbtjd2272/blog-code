package com.blogcode.sqslistener.message;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
public class AwsMessageListenerContainer extends SimpleMessageListenerContainer {

    //커스텀 스레드풀만 사용하므로 무조건 destroy하도록 호출
    @Override
    protected void doDestroy() {
        log.info("Worker ThreadPool destroy start");
        ((ThreadPoolTaskExecutor) getTaskExecutor()).destroy();
        log.info("Worker ThreadPool destroy end");
    }

    /**
     * SmartLifeCycle
     * 가장 나중에 시작
     * 가장 먼저 종료
     */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
