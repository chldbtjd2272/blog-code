package com.blogcode.sqslistener.config;

import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.localstack.LocalStackContainer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AwsMessageClientConfig {

    @Bean(initMethod = "start", destroyMethod = "close")
    public LocalStackContainer localStackContainer() {
        return new LocalStackContainer().withServices(
                LocalStackContainer.Service.SNS,
                LocalStackContainer.Service.SQS
        );
    }

    @Bean(destroyMethod = "shutdown")
    public AmazonSQSBufferedAsyncClient amazonSQS(LocalStackContainer localStackContainer) {
        QueueBufferConfig queueBufferConfig = new QueueBufferConfig();
        queueBufferConfig.setFlushOnShutdown(true);

        return new AmazonSQSBufferedAsyncClient(
                AmazonSQSAsyncClientBuilder.standard()
                        .withEndpointConfiguration(localStackContainer.getEndpointConfiguration(LocalStackContainer.Service.SQS))
                        .withCredentials(localStackContainer.getDefaultCredentialsProvider())
                        .build(), queueBufferConfig);
    }

}
