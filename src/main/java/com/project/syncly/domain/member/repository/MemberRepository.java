package com.project.syncly.domain.member.repository;

import com.project.syncly.domain.member.cache.MemberProfileCacheDTO;
import com.project.syncly.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {


    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
        SELECT new com.project.syncly.domain.member.cache.MemberProfileCacheDTO(
            m.id, m.name, m.profileImage
        )
        FROM Member m
        WHERE m.id = :memberId
    """)
    MemberProfileCacheDTO getMemberProfile(@Param("memberId") Long memberIds);

    @Query("""
        SELECT new com.project.syncly.domain.member.cache.MemberProfileCacheDTO(
            m.id, m.name, m.profileImage
        )
        FROM Member m
        WHERE m.id IN :memberIds
    """)
    List<MemberProfileCacheDTO> getMemberProfiles(@Param("memberIds") List<Long> memberIds);
}