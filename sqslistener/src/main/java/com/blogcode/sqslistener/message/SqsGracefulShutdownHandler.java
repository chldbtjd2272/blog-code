package com.blogcode.sqslistener.message;

import com.blogcode.sqslistener.message.MessageThreadPoolProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.SmartLifecycle;

import java.util.List;


@Slf4j
@RequiredArgsConstructor
public class SqsGracefulShutdownHandler implements SmartLifecycle {
    private final List<SimpleMessageListenerContainer> messageListenerContainers;
    //커스텀 스레드 풀은 messageListenerContainers의 관련 빈들을 destroy 한 뒤 destroy해야한다.
    private final MessageThreadPoolProvider messageThreadPoolProvider;

    private boolean isRunning;

    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop(Runnable callback) {
        stop();
        callback.run();
    }

    @Override
    public void start() {
        this.isRunning = true;
    }


    /**
    * DisposableBean을 이용해 파괴하는건 의미없다.
     * SimpleMessageListenerContainer를 의존하고 있는 빈은 SqsGracefulShutdownHandler
     *  customThreadPool을 SimpleMessageListenerContainer가 파괴된 이후 파괴되도록하기 위해 순차 호출
    * */
    @Override
    public void stop() {
        log.info("========= 리스너 파괴 시작");
        this.messageListenerContainers.forEach(simpleMessageListenerContainer -> simpleMessageListenerContainer.stop());
        this.messageListenerContainers.forEach(simpleMessageListenerContainer -> simpleMessageListenerContainer.destroy());
        this.messageThreadPoolProvider.destroy();
        log.info("========= 리스너 파괴 종료");
        this.isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return this.isRunning;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }
}
