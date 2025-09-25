package com.project.syncly.domain.file.entity;

import com.project.syncly.domain.file.enums.FileType;
import com.project.syncly.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "file")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class File extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "folder_id", nullable = false)
    private Long folderId;

    @Column(name = "workspace_member_id", nullable = false)
    private Long workspaceMemberId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private FileType type;

    @Column(name = "object_key", nullable = false, columnDefinition = "TEXT")
    private String objectKey;

    @Column(name = "size", nullable = false)
    private Long size;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;
}