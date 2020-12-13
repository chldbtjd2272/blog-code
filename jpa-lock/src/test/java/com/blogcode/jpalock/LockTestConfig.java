package com.blogcode.jpalock;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Import(JpaLockApplication.class)
@Configuration
@RequiredArgsConstructor
public class LockTestConfig {

    @Bean
    public TransactionSupport transactionSupport(){
        return new TransactionSupport();
    }
}
