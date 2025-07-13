package com.project.syncly.domain.livekit.service;

public interface LiveKitTokenService {
    String issueToken(Long memberId, Long workspaceId);
}
