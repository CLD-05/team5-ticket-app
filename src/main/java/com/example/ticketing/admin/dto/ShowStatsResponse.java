package com.example.ticketing.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowStatsResponse {
    private Long showId;
    private String title;
    private String venue;
    private long totalRevenue;
    private List<GradeStats> stats;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeStats {
        private String gradeName;
        private int price;
        private int total;
        private int available;
        private int hold;
        private int sold;
        private double soldRate;
    }
}
