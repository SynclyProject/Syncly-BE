package com.project.syncly.domain.workspace.repository;

import com.project.syncly.domain.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

}
