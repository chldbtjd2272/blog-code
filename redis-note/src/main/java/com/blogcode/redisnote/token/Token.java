package com.blogcode.redisnote.token;

import lombok.Getter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@RedisHash(value = "token", timeToLive = 60)
public class Token implements Serializable {

    @Id
    private String id;
    private LocalDateTime generateTime;

    public Token(String id) {
        this.id = id;
        this.generateTime = LocalDateTime.now();
    }
}
