package com.blogcode.jpabatch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
@SpringBootApplication
public class JpabatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(JpabatchApplication.class, args);
    }

}
