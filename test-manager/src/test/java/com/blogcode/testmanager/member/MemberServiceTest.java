package com.blogcode.testmanager.member;

import com.blogcode.testmanager.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@IntegrationTest
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository repository;

    @AfterEach
    void tearDown() {
        repository.deleteAllInBatch();
    }

    @Test
    @DisplayName("가입")
    void name() {
        //given
        //when
        memberService.save("최유성");
        //then
        Member member=repository.findByName("최유성");
        assertThat(member.getName()).isEqualTo("최유성");
    }
}