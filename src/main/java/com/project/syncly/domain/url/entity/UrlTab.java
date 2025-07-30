package com.project.syncly.domain.url.entity;
import com.project.syncly.domain.workspace.entity.Workspace;
import com.project.syncly.domain.url.entity.UrlItem;
import com.project.syncly.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Entity
@Table(name = "url_tab")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UrlTab extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "tab_name", length = 100, nullable = false)
    private String tabName;

    @OneToMany(mappedBy = "urlTab", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UrlItem> urlItems;

    public void updateTabName(String newName) {
        this.tabName = newName;
    }
}

