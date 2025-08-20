package com.project.syncly.domain.auth.cache;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.member.exception.MemberErrorCode;
import com.project.syncly.domain.member.exception.MemberException;
import com.project.syncly.domain.member.repository.MemberRepository;
import com.project.syncly.global.redis.core.RedisStorage;
import com.project.syncly.global.redis.enums.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

@RequiredArgsConstructor
@Service
public class LoginCacheService {

    private final RedisStorage redisStorage;
    private final MemberRepository memberRepository;

    @Value("${redis.cache.member-ttl-seconds}")
    private long memberTtlSeconds;

    public void cacheMember(Member member) {
        String key = RedisKeyPrefix.MEMBER_CACHE.get(member.getId());
        redisStorage.set(key, member, Duration.ofSeconds(memberTtlSeconds));
    }
    public Member getCachedMember(Long memberId) {
        String key = RedisKeyPrefix.MEMBER_CACHE.get(memberId);
        Member member = redisStorage.getValueAsString(key, Member.class);

        if (member == null) {
            member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
            cacheMember(member);
        }

        return member;
    }
    // 수정 시 : 삭제 후 재 캐싱
    public void removeMemberCache(Long memberId) {
        String key = RedisKeyPrefix.MEMBER_CACHE.get(memberId);
        redisStorage.delete(key);
    }
}
