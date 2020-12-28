package com.blogcode.sqslistener.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.blogcode.sqslistener.message.MessageThreadPoolProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.config.SimpleMessageListenerContainerFactory;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.blogcode.sqslistener.config.SqsObjectMapperProvider.messageConverter;


@Slf4j
@RequiredArgsConstructor
@Configuration
public class SqsListenerConfig {

    private final AmazonSQSAsync amazonSQSAsync;

    @Bean
    public SimpleMessageListenerContainerFactory simpleMessageListenerContainerFactory() {
        SimpleMessageListenerContainerFactory factory = new SimpleMessageListenerContainerFactory();
        factory.setAmazonSqs(amazonSQSAsync);
        factory.setMaxNumberOfMessages(5);
        factory.setTaskExecutor(messageThreadPoolProvider().getExecutor());
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
    public SqsGracefulShutdownHandler sqsGracefulShutdownHandler(Map<String, SimpleMessageListenerContainer> messageListenerContainers,
                                                                 MessageThreadPoolProvider messageThreadPoolProvider) {
        return new SqsGracefulShutdownHandler(messageListenerContainers, messageThreadPoolProvider);
    }
}
