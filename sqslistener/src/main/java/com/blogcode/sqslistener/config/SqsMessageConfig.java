package com.blogcode.sqslistener.config;

import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

@Slf4j
@Configuration
public class SqsMessageConfig {

    @Primary
    @Bean(destroyMethod = "shutdown")
    public AmazonSQSBufferedAsyncClient amazonSQSAws() {

        AmazonSQSAsync amazonSQSAsync = AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(InstanceProfileCredentialsProvider.getInstance())
                .withRegion(Regions.AP_NORTHEAST_2)
                .build();

        log.info("[SQS] amazonSQSAws !local data={}", amazonSQSAsync);

        return new AmazonSQSBufferedAsyncClient(amazonSQSAsync);
    }

    @SuppressWarnings("Duplicates")
    @Primary
    @Bean
    public QueueMessagingTemplate queueMessagingTemplate(AmazonSQSAsync amazonSqs) {
        return new QueueMessagingTemplate(amazonSqs, (ResourceIdResolver) null, messageConverter());
    }

    @Primary
    @Bean
    public MappingJackson2MessageConverter messageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(SqsObjectMapperProvider.provider());
        converter.setSerializedPayloadClass(String.class);
        return converter;
    }
}
