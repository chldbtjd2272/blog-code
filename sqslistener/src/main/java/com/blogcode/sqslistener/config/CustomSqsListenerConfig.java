package com.blogcode.sqslistener.config;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.blogcode.sqslistener.message.AwsMessageListenerContainerFactory;
import com.blogcode.sqslistener.message.SqsMessageHandlerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.annotation.PostConstruct;

@Import(AwsMessageClientConfig.class)
@Configuration
@RequiredArgsConstructor
@ComponentScan("com.blogcode.sqslistener.listener")
@EnableConfigurationProperties(SqsPropertiesGroup.class)
public class CustomSqsListenerConfig {

    private final AmazonSQSAsync amazonSQS;
    private final SqsPropertiesGroup sqsPropertiesGroup;

    /*
     * naver pay
     * */

    @Bean
    public SimpleMessageListenerContainer naverPayMessageContainer() {
        return AwsMessageListenerContainerFactory.createListenerContainer(amazonSQS,
                sqsPropertiesGroup.getNaverGroup(),
                naverPayMessageHandler());
    }

    @Bean
    public QueueMessageHandler naverPayMessageHandler() {
        return SqsMessageHandlerFactory.createQueueMessageHandler(amazonSQS,
                sqsPropertiesGroup.getNaverGroup().getDestinationSet());
    }

    /*
     * kakao pay
     * */

    @Bean
    public SimpleMessageListenerContainer kakaoPayMessageContainer() {
        return AwsMessageListenerContainerFactory.createListenerContainer(amazonSQS,
                sqsPropertiesGroup.getKakaoGroup(),
                kakaoPayMessageHandler());
    }

    @Bean
    public QueueMessageHandler kakaoPayMessageHandler() {
        return SqsMessageHandlerFactory.createQueueMessageHandler(amazonSQS,
                sqsPropertiesGroup.getKakaoGroup().getDestinationSet());
    }


    @Bean
    public QueueMessagingTemplate queueMessagingTemplate(){
        return new QueueMessagingTemplate(amazonSQS,
                (ResourceIdResolver) null,
                SqsMessageHandlerFactory.getMessageConverter());
    }

    @PostConstruct
    private void init() {
        sqsPropertiesGroup.getKakaoGroup().getDestinationSet().forEach(amazonSQS::createQueue);
        sqsPropertiesGroup.getNaverGroup().getDestinationSet().forEach(amazonSQS::createQueue);
    }
}
