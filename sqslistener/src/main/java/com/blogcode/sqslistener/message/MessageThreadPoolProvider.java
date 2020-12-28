package com.blogcode.sqslistener.message;

import lombok.Getter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Getter
public class MessageThreadPoolProvider implements DisposableBean {
    private final ThreadPoolTaskExecutor executor;

    public MessageThreadPoolProvider() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setThreadNamePrefix("sqs-listener-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30000);
        executor.initialize();
    }

    @Override
    public void destroy(){
        executor.destroy();
    }
}
