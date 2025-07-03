package com.project.syncly.global.jwt;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class PrincipalDetailsService {

    private final MemberQueryService memberQueryService;

    public UserDetails loadUserById(Long id) {
        Member member = memberQueryService.getMemberByIdWithRedis(id);
        return new PrincipalDetails(member);
    }
} 