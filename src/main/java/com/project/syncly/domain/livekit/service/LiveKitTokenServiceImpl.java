package com.project.syncly.domain.livekit.service;

import com.project.syncly.domain.livekit.converter.LiveKitConverter;
import com.project.syncly.domain.livekit.exception.LiveKitErrorCode;
import com.project.syncly.domain.livekit.exception.LiveKitException;
import com.project.syncly.domain.livekit.util.LiveKitTokenUtil;
import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.workspaceMember.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LiveKitTokenServiceImpl implements LiveKitTokenService {
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final LiveKitTokenUtil tokenUtil;



    @Override
    public boolean isMemberIncludeWorkspace(Long memberId, Long workspaceId) {
        return workspaceMemberRepository.existsByWorkspaceIdAndMemberId(workspaceId, memberId);
    }
    @Override
    public String issueToken(Member member, Long workspaceId) {
        if (!isMemberIncludeWorkspace(member.getId(), workspaceId)) {
            throw new LiveKitException(LiveKitErrorCode.MEMBER_NOT_INCLUDE_WORKSPACE);
        }

        String roomName = LiveKitConverter.getRoomId(workspaceId);
        return tokenUtil.createToken(member.getId().toString(), member.getName(), roomName);
    }

}
