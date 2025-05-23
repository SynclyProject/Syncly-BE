package com.project.syncly.domain.member.entity;

import com.project.syncly.global.entity.BaseTimeDeletedEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Where(clause = "is_deleted = false")//조회 시 쿼리에 "is_deleted = false" 조건 추가
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
    @Column(nullable = true)
    private SocialLoginProvider socialLoginProvider;

    @Column
    private LeaveReasonType leaveReasonType;

    @Column(length = 200)
    private String leaveReason;

    public void updateName(String newName) {
        this.name = newName;
    }


    public void markAsDeleted(LeaveReasonType reasonType, String leaveReason) {
        super.markAsDeleted();
        this.leaveReasonType = reasonType;
        this.leaveReason = leaveReason;
    }

}
