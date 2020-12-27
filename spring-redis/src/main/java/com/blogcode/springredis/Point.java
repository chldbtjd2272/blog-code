package com.blogcode.springredis;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@NoArgsConstructor
@RedisHash(value = "point", timeToLive = 3000L)
public class Point {

    @Id
    private String id; // userId
    private Long point;

    public Point(String id, Long point) {
        this.id = id;
        this.point = point;
    }
}
