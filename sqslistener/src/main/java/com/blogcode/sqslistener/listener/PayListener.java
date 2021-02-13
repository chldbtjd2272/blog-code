package com.blogcode.sqslistener.listener;

import com.blogcode.sqslistener.message.MessageThreadPoolProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.aws.messaging.listener.SqsMessageDeletionPolicy;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayListener {
    private final MessageThreadPoolProvider messageThreadPoolProvider;

    @SqsListener(value = "${message.listener.kakaoGroup.destination.kakao}", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void listenKakao(@Payload Message message) throws InterruptedException {
        message.execute();
        log.info("kakao 페이 충전 {}, active tread {}, thread pool {}",
                message.toString() ,
                messageThreadPoolProvider.getExecutor().getActiveCount(),
                messageThreadPoolProvider.getExecutor().getMaxPoolSize());
        Thread.sleep(5000L);
    }

    @SqsListener(value = "${message.listener.naverGroup.destination.naver}", deletionPolicy = SqsMessageDeletionPolicy.ALWAYS)
    public void listenNaver(@Payload Message message) {
        message.execute();
        log.info("naver 페이 충전 {}, active tread {}, thread pool {}",
                message.toString() ,
                messageThreadPoolProvider.getExecutor().getActiveCount(),
                messageThreadPoolProvider.getExecutor().getMaxPoolSize());
    }
}
