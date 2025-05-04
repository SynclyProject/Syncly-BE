package com.project.syncly.domain.workspaceMember.repository;

import com.project.syncly.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceMemberRepository extends JpaRepository<Workspace, Long> {

}
