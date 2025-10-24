package com.project.syncly.domain.note.util;

/**
 * 사용자별 고유 색상을 생성하는 유틸리티 클래스
 *
 * <p>실시간 협업 노트에서 각 사용자의 커서 및 선택 영역을 구분하기 위한 색상을 생성합니다.
 */
public class UserColorGenerator {

    /**
     * 미리 정의된 색상 팔레트 (16진수 색상 코드)
     *
     * <p>가독성이 좋고 서로 구분하기 쉬운 색상들로 구성
     */
    private static final String[] COLOR_PALETTE = {
            "#FF6B6B", // Red
            "#4ECDC4", // Teal
            "#45B7D1", // Sky Blue
            "#FFA07A", // Light Salmon
            "#98D8C8", // Mint
            "#F7DC6F", // Yellow
            "#BB8FCE", // Purple
            "#85C1E2", // Light Blue
            "#F8B739", // Orange
            "#52B788", // Green
            "#EF476F", // Pink
            "#06FFA5", // Cyan
            "#FFD97D", // Peach
            "#AAB7B8", // Gray
            "#FF9FF3", // Magenta
            "#54A0FF", // Blue
            "#48DBFB", // Aqua
            "#FF9F43", // Tangerine
            "#00D2D3", // Turquoise
            "#B53471"  // Rose
    };

    /**
     * WorkspaceMember ID를 기반으로 고유한 색상을 생성합니다.
     *
     * <p>동일한 ID는 항상 동일한 색상을 반환하며,
     * 색상 팔레트 내에서 순환합니다.
     *
     * @param workspaceMemberId WorkspaceMember ID
     * @return 16진수 색상 코드 (예: "#FF6B6B")
     */
    public static String generateColor(Long workspaceMemberId) {
        if (workspaceMemberId == null) {
            return COLOR_PALETTE[0]; // 기본 색상
        }

        int index = (int) (workspaceMemberId % COLOR_PALETTE.length);
        return COLOR_PALETTE[index];
    }

    /**
     * 색상 팔레트의 크기를 반환합니다.
     *
     * @return 사용 가능한 색상 개수
     */
    public static int getColorCount() {
        return COLOR_PALETTE.length;
    }
}
