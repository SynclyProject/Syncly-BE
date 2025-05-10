package com.project.syncly.global.jwt;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.domain.auth.cache.LoginCacheService;
import com.project.syncly.domain.member.service.MemberQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class PrincipalDetailsService {

    private final MemberQueryService memberQueryService;

    public UserDetails loadUserById(Long id) {
        Member member = memberQueryService.getMemberById(id);
        return new PrincipalDetails(member);
    }
} 