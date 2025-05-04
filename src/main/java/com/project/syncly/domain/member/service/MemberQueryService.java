package com.project.syncly.domain.member.service;

import com.project.syncly.domain.member.entity.Member;
import org.springframework.transaction.annotation.Transactional;

public interface MemberQueryService {
    public boolean isEmailExist(String email);
    public Member getMemberByEmail(String email);
}
