package com.blogcode.sqslistener.message;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.blogcode.sqslistener.config.SqsPropertiesGroup;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class AwsMessageListenerContainerFactory {

    public static SimpleMessageListenerContainer createListenerContainer(AmazonSQSAsync amazonSQS,
                                                                  SqsPropertiesGroup.SqsProperties sqsProperties,
                                                                  QueueMessageHandler queueMessageHandler) {
        SimpleMessageListenerContainer container = new AwsMessageListenerContainer();
        container.setAmazonSqs(amazonSQS);
        container.setMaxNumberOfMessages(sqsProperties.getMaxNumberOfMessages());
        container.setMessageHandler(queueMessageHandler);
        container.setTaskExecutor(createThreadPool(sqsProperties));
        container.setAutoStartup(false);
        return container;
    }

    private static ThreadPoolTaskExecutor createThreadPool(SqsPropertiesGroup.SqsProperties sqsProperties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(sqsProperties.getCorePoolSize());
        executor.setMaxPoolSize(sqsProperties.getMaxPoolSize());
        executor.setThreadNamePrefix(sqsProperties.getThreadPrefixName());
        executor.initialize();
        return executor;
    }
}
