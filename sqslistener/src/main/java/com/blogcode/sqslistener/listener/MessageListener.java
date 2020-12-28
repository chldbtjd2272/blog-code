package com.blogcode.sqslistener.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.Acknowledgment;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageListener {


    @SqsListener(value = "cys-test", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void listen(Message message) throws InterruptedException {
        log.info(message.toString());
        Thread.sleep(20000L);
        log.info("메시지 처리 완료");
    }
}
