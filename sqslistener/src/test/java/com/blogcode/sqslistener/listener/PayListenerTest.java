package com.blogcode.sqslistener.listener;

import com.blogcode.sqslistener.config.CustomSqsListenerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = CustomSqsListenerConfig.class)
class PayListenerTest {

//    @Autowired
//    private QueueMessagingTemplate queueMessagingTemplate;
//
//    @Autowired
//    private List<SimpleMessageListenerContainer> containerList;
//
//    @Test
//    void name() throws InterruptedException {
//        //given
//        Message.latch = new CountDownLatch(4);
//        queueMessagingTemplate.convertAndSend("kakao-pay-queue", new Message("kakao"));
//        queueMessagingTemplate.convertAndSend("kakao-pay-queue", new Message("kakao2"));
//        queueMessagingTemplate.convertAndSend("naver-pay-queue", new Message("naver"));
//        queueMessagingTemplate.convertAndSend("naver-pay-queue", new Message("naver2"));
//
//        //when
//        containerList.forEach(container -> container.start());
//        Message.latch.await(3, TimeUnit.SECONDS);
//
//        //then
//        assertThat(Message.getMessageCapture("kakao-thread-").size()).isEqualTo(2);
//        assertThat(Message.getMessageCapture("naver-thread-").size()).isEqualTo(2);
//    }


}