package com.blogcode.sqslistener.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Slf4j
@ControllerAdvice
public class ListenerAdvice {

    @MessageExceptionHandler(Exception.class)
    void handleException(Exception e) {
        log.error("[시스템 오류] {}", e.toString(), e);
    }
}
