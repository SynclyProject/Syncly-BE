package com.project.syncly.domain.workspace.dto;

import com.project.syncly.domain.workspaceMember.entity.enums.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;


@Schema(description = "워크스페이스 소속 멤버 조회 응답 DTO")
public record WorkspaceMemberInfoResponseDto(
        Long workspaceMemberId,
        String memberName,
        String memberEmail,
        String memberObjectKey,
        Role role,
        LocalDateTime joinedAt
) {}