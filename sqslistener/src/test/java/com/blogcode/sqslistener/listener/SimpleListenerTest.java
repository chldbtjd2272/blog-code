package com.blogcode.sqslistener.listener;

import com.blogcode.sqslistener.config.SqsListenerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.cloud.aws.messaging.listener.SimpleMessageListenerContainer;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = SqsListenerConfig.class)
public class SimpleListenerTest {

    @Autowired
    private QueueMessagingTemplate queueMessagingTemplate;

    @Autowired
    private List<SimpleMessageListenerContainer> containerList;

    @Test
    @DisplayName("message poll 사이즈가 1이면 스레드를 하나만 사용한다.")
    void name() throws InterruptedException {
        //given
        Message.latch = new CountDownLatch(20);
        for (int i = 0; i < 20; i++) {
            queueMessagingTemplate.convertAndSend("kakao-pay-queue", new Message("kakao" + i));
        }

        //when
        containerList.forEach(container -> container.start());
        Message.latch.await();

        //then
        assertThat(Message.messageCapture.size()).isEqualTo(20);
    }

    @Test
    @DisplayName("처리되는 queue 작업은 서로 영향을 주지 않는다.")
    void name2() throws InterruptedException {
        //given
        Message.latch = new CountDownLatch(20);
        for (int i = 0; i < 10; i++) {
            queueMessagingTemplate.convertAndSend("kakao-pay-queue", new Message("kakao" + i));
            queueMessagingTemplate.convertAndSend("naver-pay-queue", new Message("naver" + i));
        }

        //when
        containerList.forEach(container -> container.start());
        Message.latch.await();

        //then
        assertThat(Message.messageCapture.size()).isEqualTo(20);
    }
}
