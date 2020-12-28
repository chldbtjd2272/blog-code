package com.blogcode.sqslistener.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.blogcode.sqslistener.message.AwsMessageListenerContainer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Collections;

import static com.blogcode.sqslistener.config.SqsObjectMapperProvider.messageConverter;

@Import(AwsMessageClientConfig.class)
@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(value = "message.listener", havingValue = "custom")
public class CustomSqsListenerConfig {

    private final AmazonSQSAsync amazonSQS;

    @Bean
    public SimpleMessageListenerContainer simpleMessageListenerContainer() {
        SimpleMessageListenerContainer container = new AwsMessageListenerContainer();
        container.setMaxNumberOfMessages(5);
        container.setWaitTimeOut(10);
        container.setQueueStopTimeout(30);
        container.setAmazonSqs(amazonSQS);
        container.setMessageHandler(queueMessageHandler());
        container.setTaskExecutor(createThreadPool());
        return container;
    }

    @Bean
    public QueueMessageHandler queueMessageHandler() {
        QueueMessageHandlerFactory queueMessageHandlerFactory = new QueueMessageHandlerFactory();
        queueMessageHandlerFactory.setAmazonSqs(amazonSQS);
        queueMessageHandlerFactory.setMessageConverters(Collections.singletonList(messageConverter()));
        return queueMessageHandlerFactory.createQueueMessageHandler();
    }

    private ThreadPoolTaskExecutor createThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(10);
        executor.setThreadNamePrefix("custom-thread-");
        executor.initialize();
        return executor;
    }
}
