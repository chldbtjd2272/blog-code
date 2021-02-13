package com.blogcode.sqslistener.main;

import com.blogcode.sqslistener.config.CustomSqsListenerConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@Import(CustomSqsListenerConfig.class)
@SpringBootApplication
public class SqslistenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SqslistenerApplication.class, args);
    }

}
