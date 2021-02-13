package com.blogcode.springredis;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@ToString
@Getter
@NoArgsConstructor
@RedisHash(value = "point", timeToLive = 30L)
public class Point {

    @Id
    private String id; // userId

    @Indexed
    private Long point;

    public Point(String id, Long point) {
        this.id = id;
        this.point = point;
    }
}
