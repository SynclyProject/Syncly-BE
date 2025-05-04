package com.project.syncly.domain.member.entity;

import com.project.syncly.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "members", indexes = {
        @Index(name = "idx_member_is_deleted", columnList = "isDeleted")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Member extends BaseTimeDeletedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String profileImage;

    @Column(length = 100)
    private String socialLoginUuid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialLoginProvider socialLoginProvider;


}
