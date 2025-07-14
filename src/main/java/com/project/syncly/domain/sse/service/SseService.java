package com.project.syncly.domain.sse.service;


import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspace.entity.WorkspaceInvitation;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {
    public SseEmitter subscribe(Long memberId);
    public void sendInvitationAlertToUser(Long memberId, WorkspaceInvitation invitation, Workspace workspace);
}