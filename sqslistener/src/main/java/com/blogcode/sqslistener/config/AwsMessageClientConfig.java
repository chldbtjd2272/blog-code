package com.blogcode.sqslistener.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import com.amazonaws.services.sqs.buffered.AmazonSQSBufferedAsyncClient;
import com.amazonaws.services.sqs.buffered.QueueBufferConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AwsMessageClientConfig {

    @Primary
    @Bean(destroyMethod = "shutdown")
    public AmazonSQSBufferedAsyncClient amazonSQSAws() {
        //queue설정으로 sqs 요청 다 처리한 이후 shutdown
        QueueBufferConfig queueBufferConfig = new QueueBufferConfig();
        queueBufferConfig.setFlushOnShutdown(true);

        return new AmazonSQSBufferedAsyncClient(AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(Regions.AP_NORTHEAST_2)
                .build(), queueBufferConfig);
    }

    @Primary
    @Bean
    public AmazonSNS amazonSNS() {
        return AmazonSNSClientBuilder.standard()
                .withCredentials(new EnvironmentVariableCredentialsProvider())
                .withRegion(Regions.AP_NORTHEAST_2)
                .build();
    }
}
