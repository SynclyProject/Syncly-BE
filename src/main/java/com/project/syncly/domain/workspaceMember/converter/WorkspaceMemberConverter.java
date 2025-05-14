package com.project.syncly.domain.workspaceMember.converter;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.workspace.dto.WorkspaceResponseDto;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.enums.WorkspaceType;
import com.project.syncly.domain.workspaceMember.entity.WorkspaceMember;
import com.project.syncly.domain.workspaceMember.entity.enums.Role;

public class WorkspaceMemberConverter {
    //워크스페이스 생성시
    public static WorkspaceMember toWorkspaceManager(Member member, Workspace workspace, String memberName) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .member(member)
                .role(Role.MANAGER)
                .name(memberName)
                .build();
    }

    //워크스페이스 초대시
    public static WorkspaceMember toWorkspaceCrew(Member member, Workspace workspace, String memberName) {
        return WorkspaceMember.builder()
                .workspace(workspace)
                .member(member)
                .role(Role.CREW)
                .name(memberName)
                .build();
    }

}
