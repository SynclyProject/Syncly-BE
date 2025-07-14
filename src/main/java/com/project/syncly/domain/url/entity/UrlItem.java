package com.project.syncly.domain.url.entity;
import com.project.syncly.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "url_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UrlItem extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "url_tab_id", nullable = false)
    private UrlTab urlTab;

    @Column(name = "url", columnDefinition = "TEXT", nullable = false)
    private String url;
}
