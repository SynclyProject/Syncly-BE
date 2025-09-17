package com.project.syncly.domain.folder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class ListingDto {

    @Schema(description = "폴더/파일 목록 조회 요청 DTO")
    public record ItemListRequest(
            @Schema(description = "정렬 방식 (latest, alphabet)", example = "latest")
            String sort,
            
            @Schema(description = "커서 (최신순: 날짜시간, 가나다순: 이름)", example = "2025-09-17T10:00:00")
            String cursor,
            
            @Schema(description = "검색어", example = "회의록")
            String search,
            
            @Schema(description = "한 페이지당 항목 수 (1-100)", example = "20")
            @Min(value = 1, message = "한 페이지당 항목 수는 최소 1개입니다.")
            @Max(value = 100, message = "한 페이지당 항목 수는 최대 100개입니다.")
            Integer limit
    ) {
        public ItemListRequest {
            if (sort == null) sort = "latest";
            if (limit == null) limit = 20;
        }
    }

    @Schema(description = "사용자 정보 DTO")
    public record UserInfo(
            Long id,
            String name,
            String profileUrl
    ){}

    @Schema(description = "폴더/파일 아이템 DTO")
    public record Item(
            Long id,
            String type,
            String name,
            String date,
            UserInfo user
    ){}
}