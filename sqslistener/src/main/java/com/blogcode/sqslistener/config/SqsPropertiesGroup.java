package com.blogcode.sqslistener.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "message.listener")
public class SqsPropertiesGroup {
    private SqsProperties naverGroup;
    private SqsProperties kakaoGroup;

    @Getter
    @AllArgsConstructor
    @ConstructorBinding
    public static class SqsProperties {
        private final Map<String, String> destination; //queue 정보
        private final Integer maxNumberOfMessages; // 한번 poll 할 때 가져올 메시지 양
        private final Integer maxPoolSize; // 스레드 풀
        private final Integer corePoolSize; // 스레드 풀
        private final String threadPrefixName; // 스레드 풀


        public Set<String> getDestinationSet() {
            return new HashSet<>(destination.values());
        }
    }
}
