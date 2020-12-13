package com.blogcode.testmanager.member;

import com.blogcode.testmanager.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
class MemberChangeServiceTest {

    @Autowired
    private MemberChangeService memberChangeService;
    @Autowired
    private MemberRepository memberRepository;


    @AfterEach
    void tearDown() {
        memberRepository.deleteAllInBatch();
    }

    @Test
    void name() {
        //given
        Member member = memberRepository.save(new Member("최유성"));
        //when
        memberChangeService.change(member.getId(),"최유성2");
        //then
        assertThat(memberRepository.findByName("최유성2")).isNotNull();

    }
}