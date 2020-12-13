package com.blogcode.testmanager.member;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberChangeService {

    private final MemberRepository memberRepository;
    private final CipherClient cipherClient;

    @Transactional
    public void change(Long id, String name) {
        Member member = memberRepository.findById(id)
                .orElseThrow(RuntimeException::new);
        member.changeName(cipherClient.encryt(name));
    }

}
