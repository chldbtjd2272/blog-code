package com.blogcode.sqslistener.message;

import lombok.Getter;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Getter
public class MessageThreadPoolProvider implements DisposableBean {
    private final ThreadPoolTaskExecutor executor;

    public MessageThreadPoolProvider() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(22);
        executor.setMaxPoolSize(22);
        executor.setThreadNamePrefix("sqs-listener-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.setAwaitTerminationSeconds(30000);
        executor.initialize();
    }

    @Override
    public void destroy(){
        executor.destroy();
    }
}
