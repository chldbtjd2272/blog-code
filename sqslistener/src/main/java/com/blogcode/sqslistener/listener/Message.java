package com.blogcode.sqslistener.listener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

@Getter
@ToString
@NoArgsConstructor
public class Message {
    public static final Set<String> messageCapture = new HashSet<>();
    public static CountDownLatch latch;
    private String text;

    public Message(String text) {
        this.text = text;
    }

    public void execute() {
        messageCapture.add(text);
        latch.countDown();
    }

//    public static Set<String> getMessageCapture(String prefix){
//        return messageCapture.stream()
//                .filter(threadName->threadName.contains(prefix))
//                .collect(Collectors.toSet());
//    }
}
