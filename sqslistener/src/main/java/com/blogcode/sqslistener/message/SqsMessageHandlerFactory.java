package com.blogcode.sqslistener.message;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import org.springframework.cloud.aws.core.env.ResourceIdResolver;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.QueueMessageHandler;
import org.springframework.cloud.aws.messaging.listener.SendToHandlerMethodReturnValueHandler;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import java.util.Collections;
import java.util.Set;


public class SqsMessageHandlerFactory {

    public static QueueMessageHandler createQueueMessageHandler(AmazonSQSAsync amazonSqs,
                                                         Set<String> destinationSet) {
        QueueMessageHandler queueMessageHandler = new SqsMessageHandler(Collections.singletonList(getMessageConverter()), destinationSet);
        QueueMessagingTemplate queueMessagingTemplate = new QueueMessagingTemplate(amazonSqs,
                (ResourceIdResolver) null,
                getMessageConverter());
        SendToHandlerMethodReturnValueHandler sendToHandlerMethodReturnValueHandler =
                new SendToHandlerMethodReturnValueHandler(queueMessagingTemplate);
        queueMessageHandler.getCustomReturnValueHandlers()
                .add(sendToHandlerMethodReturnValueHandler);
        return queueMessageHandler;
    }

    public static  MappingJackson2MessageConverter getMessageConverter() {
        MappingJackson2MessageConverter jacksonMessageConverter = new MappingJackson2MessageConverter();
        jacksonMessageConverter.setSerializedPayloadClass(String.class);
        return jacksonMessageConverter;
    }

}
