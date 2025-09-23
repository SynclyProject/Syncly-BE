package com.project.syncly.domain.workspaceMember.entity;

import com.project.syncly.domain.member.entity.Member;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.workspaceMember.entity.enums.Role;
import com.project.syncly.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "workspace_member")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class WorkspaceMember extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.CREW;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(name = "profile_image", length = 500)
    private String profileImage;

    public void setRole(Role role) {
        this.role = role;
    }
    public void updateName(String name) {}
    public void updateProfileImage(String profileImage) {}
}
