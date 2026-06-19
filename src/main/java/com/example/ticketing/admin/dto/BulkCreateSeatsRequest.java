package com.example.ticketing.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "복수 등급 좌석 일괄 생성 요청")
public class BulkCreateSeatsRequest {
    
    @Schema(description = "생성할 좌석 등급 목록")
    private List<GradeItem> grades;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "단일 등급 좌석 생성 항목")
    public static class GradeItem {
        @Schema(description = "좌석 등급명", example = "VIP")
        private String gradeName;

        @Schema(description = "좌석 가격", example = "165000")
        private int price;

        @Schema(description = "총 생성할 좌석 수", example = "120")
        private int totalSeats;

        @Schema(description = "좌석 번호 접두사 (생략 시 등급명 사용)", example = "V-A")
        private String prefix;
    }
}
