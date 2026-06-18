package com.example.ticketing.queue.controller;

import com.example.ticketing.queue.service.QueueService;
import com.example.ticketing.queue.service.QueueService.QueueStatusResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shows/{showId}/queue")
@RequiredArgsConstructor
@Tag(name = "Queue", description = "공연별 대기열 진입과 상태 확인 API")
@SecurityRequirement(name = "bearerAuth")
public class QueueController {
    private final QueueService queueService;

    // 공연별 대기열에 사용자 진입
    @Operation(summary = "대기열 참가", description = "현재 사용자를 공연별 대기열에 등록합니다. 이미 대기 중이면 순번이 뒤로 갱신될 수 있습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대기열 등록 성공",
                    content = @Content(examples = @ExampleObject(value = "\"JOINED\""))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @PostMapping("/join")
    public ResponseEntity<?> join(
            @Parameter(description = "공연 ID", example = "1") @PathVariable Long showId,
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(queueService.joinQueue(showId, userId));
    }

    // 예매 진입 시 대기열 화면이 필요한지 먼저 판단
    @Operation(summary = "예매 입장 시도", description = "예매 화면 진입 가능 여부를 확인합니다. 바로 입장 가능하면 Queue Token을 발급합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "입장 상태 조회 성공",
                    content = @Content(schema = @Schema(implementation = QueueStatusResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": "ACTIVE",
                                      "position": null,
                                      "canEnter": true,
                                      "queueToken": "550e8400-e29b-41d4-a716-446655440000"
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @PostMapping("/enter")
    public ResponseEntity<?> enter(
            @Parameter(description = "공연 ID", example = "1") @PathVariable Long showId,
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(queueService.enterQueue(showId, userId));
    }

    // 현재 사용자의 공연별 대기 순번 조회
    @Operation(summary = "대기열 상태 조회", description = "현재 사용자의 대기 순번 또는 입장 가능 상태를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대기열 상태 조회 성공",
                    content = @Content(schema = @Schema(implementation = QueueStatusResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "status": "WAITING",
                                      "position": 37,
                                      "canEnter": false,
                                      "queueToken": null
                                    }
                                    """))),
            @ApiResponse(responseCode = "404", description = "대기열 정보 없음", content = @Content)
    })
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(
            @Parameter(description = "공연 ID", example = "1") @PathVariable Long showId,
            @AuthenticationPrincipal String userId
    ) {
        return ResponseEntity.ok(queueService.getStatus(showId, userId));
    }

    // 공연별 대기열에서 사용자 이탈
    @Operation(summary = "대기열 이탈", description = "현재 사용자를 공연별 대기열과 활성 입장 목록에서 제거하고 Queue Token을 정리합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대기열 이탈 성공"),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @DeleteMapping("/leave")
    public ResponseEntity<?> leave(
            @Parameter(description = "공연 ID", example = "1") @PathVariable Long showId,
            @AuthenticationPrincipal String userId
    ) {
        queueService.leaveQueue(showId, userId);
        return ResponseEntity.ok().build();
    }
}
