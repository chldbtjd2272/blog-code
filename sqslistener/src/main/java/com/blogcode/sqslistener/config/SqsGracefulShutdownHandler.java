package com.blogcode.sqslistener.config;

import com.blogcode.sqslistener.message.MessageThreadPoolProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.SmartLifecycle;

import java.util.Map;


@Slf4j
@RequiredArgsConstructor
public class SqsGracefulShutdownHandler implements SmartLifecycle, BeanFactoryAware {
    private final Map<String, SimpleMessageListenerContainer> messageListenerContainers;
    //커스텀 스레드 풀은 messageListenerContainers의 관련 빈들을 destroy 한 뒤 destroy해야한다.
    private final MessageThreadPoolProvider messageThreadPoolProvider;
    private DefaultSingletonBeanRegistry beanFactory;

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


    @Override
    public void stop() {
        log.info("========= 리스너 파괴 시작");
        this.messageListenerContainers.keySet().forEach(beanName -> beanFactory.destroySingleton(beanName));
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

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = (DefaultSingletonBeanRegistry) beanFactory;
    }

}
