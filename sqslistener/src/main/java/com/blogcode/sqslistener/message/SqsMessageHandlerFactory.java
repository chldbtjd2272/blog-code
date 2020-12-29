package com.blogcode.sqslistener.message;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.config.QueueMessageHandlerFactory;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class SqsMessageHandlerFactory  {

    private AmazonSQSAsync amazonSqs;

    private List<MessageConverter> messageConverters;


    public void setAmazonSqs(AmazonSQSAsync amazonSqs) {
        this.amazonSqs = amazonSqs;
    }

    public void setMessageConverters(List<MessageConverter> messageConverters) {
        this.messageConverters = messageConverters;
    }

    public QueueMessageHandler createQueueMessageHandler() {
        QueueMessageHandler queueMessageHandler = new SqsMessageHandler(
                CollectionUtils.isEmpty(this.messageConverters)
                        ? Collections.singletonList(getDefaultMappingJackson2MessageConverter())
                        : this.messageConverters);

        SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler = new SendToHandlerMethodReturnValueHandler(
                new QueueMessagingTemplate(amazonSqs, (ResourceIdResolver) null, getDefaultMappingJackson2MessageConverter()));

        queueMessageHandler.getCustomReturnValueHandlers()
                .add(sendToHandlerMethodReturnValueHandler);
        return queueMessageHandler;
    }


    private MappingJackson2MessageConverter getDefaultMappingJackson2MessageConverter() {
        MappingJackson2MessageConverter jacksonMessageConverter = new MappingJackson2MessageConverter();
        jacksonMessageConverter.setSerializedPayloadClass(String.class);
        jacksonMessageConverter.setStrictContentTypeMatch(true);
        return jacksonMessageConverter;
    }

}
