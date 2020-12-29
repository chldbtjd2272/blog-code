package com.blogcode.sqslistener.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.blogcode.sqslistener.message.MessageThreadPoolProvider;
import com.blogcode.sqslistener.message.SqsGracefulShutdownHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Collections;
import java.util.List;

import static com.blogcode.sqslistener.message.SqsObjectMapperProvider.messageConverter;


@Slf4j
@RequiredArgsConstructor
@Configuration
public class SqsListenerConfig {

    private final AmazonSQSAsync amazonSQSAsync;

    @Bean
    public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory(ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
        factory.setAmazonSqs(amazonSQSAsync);
        factory.setMaxNumberOfMessages(5);
        factory.setTaskExecutor(threadPoolTaskExecutor);
        factory.setVisibilityTimeout(60);
        factory.setWaitTimeOut(1);
        return factory;
    }

    @Bean
    public QueueMessageHandlerFactory queueMessageHandlerFactory() {
        QueueMessageHandlerFactory queueMessageHandlerFactory = new QueueMessageHandlerFactory();
        queueMessageHandlerFactory.setAmazonSqs(amazonSQSAsync);
        queueMessageHandlerFactory.setMessageConverters(Collections.singletonList(messageConverter()));
        return queueMessageHandlerFactory;
    }

    @Bean
    public MessageThreadPoolProvider messageThreadPoolProvider() {
        return new MessageThreadPoolProvider();
    }

    @Bean
    public SqsGracefulShutdownHandler sqsGracefulShutdownHandler(List<SimpleMessageListenerContainer> messageListenerContainers,
                                                                 MessageThreadPoolProvider messageThreadPoolProvider) {
        return new SqsGracefulShutdownHandler(messageListenerContainers, messageThreadPoolProvider);
    }
}
