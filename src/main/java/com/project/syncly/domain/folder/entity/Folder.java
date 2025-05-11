package com.project.syncly.domain.folder.entity;

import com.project.syncly.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "folder")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Folder extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    @Column(name = "parent_id")
    private Long parentId; // 최상위 폴더면 NULL

    @Column(name = "name", nullable = false)
    private String name; // 폴더명
}
