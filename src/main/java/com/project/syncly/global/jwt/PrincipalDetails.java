package com.project.syncly.global.jwt;

import com.project.syncly.domain.member.entity.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class PrincipalDetails implements UserDetails {

    private final Member member;


    @Override
    // 권한을 가져오는 메소드
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();//권한 인증X
    }

    @Override
    // username을 가져오는 메소드
    public String getUsername() {
        return member.getEmail();
    }

    @Override
    // 비밀번호를 가져오는 메소드
    public String getPassword() {
        return member.getPassword() != null ? member.getPassword() : "";
    }

    @Override
    // 사용 가능한지 여부
    public boolean isEnabled() {
        return true;
    }

    @Override
    // 계정이 만료되지 않았는지 여부
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    // 계정이 잠겼는지에 대한 여부
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    // 비밀번호가 만료되었는지 여부
    public boolean isCredentialsNonExpired() {
        return true;
    }
} 