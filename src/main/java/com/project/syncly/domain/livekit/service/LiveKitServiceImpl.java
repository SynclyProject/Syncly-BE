package com.project.syncly.domain.livekit.service;

import com.project.syncly.domain.livekit.exception.LiveKitErrorCode;
import com.project.syncly.domain.livekit.exception.LiveKitException;
import com.project.syncly.domain.livekit.util.LiveKitTokenUtil;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LiveKitServiceImpl implements LiveKitService {
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final LiveKitTokenUtil tokenUtil;

    public String issueToken(Long memberId, Long workspaceId) {
        if (!workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, memberId)) {
            throw new LiveKitException(LiveKitErrorCode.MEMBER_NOT_INCLUDE_WORKSPACE);
        }

        String roomName = "workspace-" + workspaceId;
        return tokenUtil.createToken(memberId.toString(), roomName);
    }
}
