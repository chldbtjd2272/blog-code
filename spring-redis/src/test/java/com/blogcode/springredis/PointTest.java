package com.blogcode.springredis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = RedisConfig.class)
class PointTest {

    @Autowired
    private PointRepository pointRepository;

    @Test
    void name() {
        //given

        //when
        pointRepository.save(new Point("test1", 100L));
        //then
    }

}