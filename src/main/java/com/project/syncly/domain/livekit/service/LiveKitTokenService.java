package com.project.syncly.domain.livekit.service;

import com.project.syncly.domain.member.entity.Member;

public interface LiveKitTokenService {
    boolean isMemberIncludeWorkspace(Long memberId, Long workspaceId);
    String issueToken(Member member, Long workspaceId);
}
