package com.project.syncly.domain.folder.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "folder_closure")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@IdClass(FolderClosureId.class)
public class FolderClosure {
    @Id
    @Column(name = "ancestor_id", nullable = false)
    private Long ancestorId;

    @Id
    @Column(name = "descendant_id", nullable = false)
    private Long descendantId;

    @Column(name = "depth", nullable = false)
    private int depth;
}