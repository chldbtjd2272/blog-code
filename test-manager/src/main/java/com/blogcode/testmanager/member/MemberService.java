package com.blogcode.testmanager.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;


    public void save(String name) {
        Member member = new Member(name);
        memberRepository.save(member);
    }

}
