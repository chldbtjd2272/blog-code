package com.blogcode.redisnote.token;

import com.blogcode.redisnote.RedisNoteApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = RedisNoteApplication.class)
class TokenTest {

    @Autowired
    private TokenRepository tokenRepository;

    @Test
    void name() {
        //given
        Token token = new Token("3");

        //when
        tokenRepository.save(token);

        //then
        assertThat(tokenRepository.findById("3").isPresent()).isTrue();
    }
}