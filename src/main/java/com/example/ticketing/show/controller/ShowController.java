package com.example.ticketing.show.controller;

import com.example.ticketing.show.dto.SeatMapResponseDto;
import com.example.ticketing.show.dto.ShowDetailResponseDto;
import com.example.ticketing.show.dto.ShowListResponseDto;
import com.example.ticketing.show.service.ShowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/shows")
@RequiredArgsConstructor
@Tag(name = "Shows", description = "공연 목록, 상세, 좌석 현황 조회 API")
public class ShowController {

    private final ShowService showService;

    @Operation(summary = "공연 목록 조회", description = "등록된 공연 목록을 조회합니다. keyword를 전달하면 제목 기준으로 검색합니다.")
    @ApiResponse(responseCode = "200", description = "공연 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = ShowListResponseDto.class),
                    examples = @ExampleObject(value = """
                            [
                              {
                                "showId": 1,
                                "title": "S-Tier Concert",
                                "venue": "KSPO DOME"
                              }
                            ]
                            """)))
    @GetMapping
    public List<ShowListResponseDto> getShows(
            @Parameter(description = "공연 제목 검색어", example = "concert")
            @RequestParam(required = false) String keyword
    ) {
        return showService.getShows(keyword);
    }

    @Operation(summary = "공연 상세 조회", description = "공연 ID로 공연의 기본 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공연 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ShowDetailResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "showId": 1,
                                      "title": "S-Tier Concert",
                                      "venue": "KSPO DOME"
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "공연을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{showId}")
    public ShowDetailResponseDto getShowDetail(
            @Parameter(description = "공연 ID", example = "1")
            @PathVariable Long showId
    ) {
        return showService.getShowDetail(showId);
    }

    @Operation(summary = "좌석 등급 현황 조회", description = "공연별 좌석 등급, 가격, 전체 좌석 수와 잔여 좌석 수를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좌석 등급 현황 조회 성공",
                    content = @Content(schema = @Schema(implementation = SeatMapResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "showId": 1,
                                      "venueName": "KSPO DOME",
                                      "seatGrades": [
                                        {
                                          "gradeName": "VIP",
                                          "price": 150000,
                                          "totalSeats": 100,
                                          "remainingSeats": 42
                                        }
                                      ]
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "공연을 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{showId}/seats")
    public SeatMapResponseDto getSeatMap(
            @Parameter(description = "공연 ID", example = "1")
            @PathVariable Long showId
    ) {
        return showService.getSeatMap(showId);
    }
}
