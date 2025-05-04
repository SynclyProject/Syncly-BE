package com.project.syncly.domain.workspaceMember.entity;

import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspaceMember.entity.enums.Role;
import com.project.syncly.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "workspace_member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class WorkspaceMember extends BaseTimeDeletedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //추후 멤버 완성시 연결해주기
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CREW;

    @Column(length = 100, nullable = false)
    private String name;

    @Lob
    @Column(name = "profile_image")
    private String profileImage;
}
