package com.blogcode.sqslistener.listener;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Payload;

@RequiredArgsConstructor
public class MessageListener {


    @SqsListener(value = "${aws.sqs.queue.point-command}")
    public void listen(@Payload Message message) {

    }

}
