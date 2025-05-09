package com.project.syncly.domain.member.repository;

import com.project.syncly.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
    //구현할 메서드 인터페이스 정의
}
