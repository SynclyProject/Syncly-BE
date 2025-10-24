package com.project.syncly.domain.note.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

/**
 * 실시간 협업 노트의 커서 위치 정보
 *
 * position은 전체 텍스트에서 문자의 절대 위치 (0-based index)
 * 예: "Hello\nWorld"에서 'W' 앞 → position=6
 */
@Getter
@Builder
public class CursorPosition {

    /**
     * 커서 위치 (0-based index)
     * 전체 텍스트에서 문자의 절대 위치
     */
    private final int position;

    /**
     * 선택 영역 길이 (0이면 단순 커서)
     * 예: 5글자 드래그 → range=5
     */
    private final int range;

    /**
     * WorkspaceMember ID
     */
    private final Long workspaceMemberId;

    /**
     * 워크스페이스 내 사용자 이름 (WorkspaceMember.name)
     */
    private final String userName;

    /**
     * 워크스페이스 내 프로필 이미지 (WorkspaceMember.profileImage)
     */
    private final String profileImage;

    /**
     * 사용자별 고유 색상 (hex 코드)
     * 예: "#FF6B6B"
     */
    private final String color;

    @JsonCreator
    public CursorPosition(
            @JsonProperty("position") int position,
            @JsonProperty("range") int range,
            @JsonProperty("workspaceMemberId") Long workspaceMemberId,
            @JsonProperty("userName") String userName,
            @JsonProperty("profileImage") String profileImage,
            @JsonProperty("color") String color
    ) {
        this.position = position;
        this.range = range;
        this.workspaceMemberId = workspaceMemberId;
        this.userName = userName;
        this.profileImage = profileImage;
        this.color = color;
    }
}
