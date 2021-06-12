package com.blogcode.jpabatch.persitable;

import com.blogcode.jpabatch.JpabatchApplication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = JpabatchApplication.class)
class MemberTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private TeamRepository teamRepository;

    @AfterEach
    void tearDown() {
        memberRepository.deleteAll();
        teamRepository.deleteAll();
    }

    @Test
    void name() {
        //given
        List<Team> teams = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String teamName = String.format("team-%d", i);
            Team team = new Team(teamName);

            for (long j = 0; j < 3; j++) {
                String memberName = String.format("member-%d-%d", i, j);
                Member member = new Member(memberName, team);
                team.addMember(member);
            }
            teams.add(team);
        }

        //when
        teamRepository.saveAll(teams);

        //then
        assertThat(memberRepository.findAll().size()).isEqualTo(9);
        assertThat(teamRepository.findAll().size()).isEqualTo(3);
    }
}