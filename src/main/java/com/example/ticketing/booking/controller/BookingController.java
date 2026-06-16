package com.example.ticketing.booking.controller;

import com.example.ticketing.booking.service.BookingService;
import com.example.ticketing.booking.service.BookingService.BookingAcceptResponse;
import com.example.ticketing.booking.service.BookingService.BookingStatusResponse;
import com.example.ticketing.booking.service.BookingService.UserBookingResponse;
import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.ErrorCode;
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
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "예매 요청, 처리 상태 조회, 내 예매 관리 API")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {
    private final BookingService bookingService;

    @Operation(summary = "예매 요청", description = "선점한 좌석을 예매 처리 큐에 등록합니다. Queue Token은 X-Queue-Token 헤더 또는 queueToken 쿠키로 전달합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "202", description = "예매 요청 접수",
                    content = @Content(schema = @Schema(implementation = BookingAcceptResponse.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "requestId": "9a1f57bc-ff25-4a8b-a9a3-5c93847f0f52",
                                      "status": "ACCEPTED"
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "seatId 누락 또는 잘못된 요청", content = @Content),
            @ApiResponse(responseCode = "403", description = "Queue Token 누락, 만료 또는 불일치", content = @Content),
            @ApiResponse(responseCode = "409", description = "좌석 선점 만료 또는 예매 불가", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> requestBooking(
            @Parameter(description = "예매할 좌석 ID. request body 대신 query로 전달할 수 있습니다.", example = "101")
            @RequestParam(required = false) Long seatId,
            @RequestBody(required = false) BookingRequest request,
            @AuthenticationPrincipal String userId,
            @Parameter(description = "대기열 입장 후 발급된 Queue Token", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestHeader(value = "X-Queue-Token", required = false) String queueTokenHeader,
            @CookieValue(value = "queueToken", required = false) String queueTokenCookie
    ) {
        Long resolvedSeatId = resolveSeatId(seatId, request);
        String queueToken = resolveQueueToken(queueTokenHeader, queueTokenCookie);

        var response = bookingService.requestBooking(resolvedSeatId, userId, queueToken);
        return ResponseEntity.accepted().body(response);
    }
    
    @Operation(summary = "예매 처리 상태 조회", description = "비동기 예매 요청의 처리 상태를 requestId로 조회합니다.")
    @ApiResponse(responseCode = "200", description = "예매 처리 상태 조회 성공",
            content = @Content(schema = @Schema(implementation = BookingStatusResponse.class),
                    examples = @ExampleObject(value = """
                            {
                              "requestId": "9a1f57bc-ff25-4a8b-a9a3-5c93847f0f52",
                              "status": "PROCESSING"
                            }
                            """)))
    @GetMapping("/status/{requestId}")
    public ResponseEntity<?> getStatus(
            @Parameter(description = "예매 요청 ID", example = "9a1f57bc-ff25-4a8b-a9a3-5c93847f0f52")
            @PathVariable String requestId
    ) {
    	return ResponseEntity.ok(bookingService.getBookingStatus(requestId));
    }

    @Operation(summary = "내 예매 목록 조회", description = "로그인한 사용자의 예매 내역을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내 예매 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = UserBookingResponse.class),
                            examples = @ExampleObject(value = """
                                    [
                                      {
                                        "bookingId": "BK-20240615-0001",
                                        "showTitle": "S-Tier Concert",
                                        "venue": "KSPO DOME",
                                        "seatNumber": "A-1",
                                        "price": 150000,
                                        "bookedAt": "2026-06-15T19:30:00"
                                      }
                                    ]
                                    """))),
            @ApiResponse(responseCode = "401", description = "인증 필요", content = @Content)
    })
    @GetMapping("/me")
    public ResponseEntity<?> getMyBookings(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(bookingService.getUserBookings(userId));
    }
    
    @Operation(summary = "예매 취소", description = "본인의 예매를 취소하고 좌석을 다시 예매 가능 상태로 되돌립니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "예매 취소 성공"),
            @ApiResponse(responseCode = "403", description = "본인 예매가 아님", content = @Content),
            @ApiResponse(responseCode = "404", description = "예매 내역 없음", content = @Content)
    })
    @DeleteMapping("/{bookingId}")
    public ResponseEntity<Void> cancelBooking(
            @Parameter(description = "예매 ID", example = "BK-20240615-0001")
            @PathVariable String bookingId,
            @AuthenticationPrincipal String userId) {
        bookingService.cancelBooking(bookingId, userId);
        return ResponseEntity.noContent().build();  // 204
    }

    private Long resolveSeatId(Long seatId, BookingRequest request) {
        Long resolvedSeatId = seatId != null ? seatId : request != null ? request.seatId() : null;
        if (resolvedSeatId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return resolvedSeatId;
    }

    private String resolveQueueToken(String queueTokenHeader, String queueTokenCookie) {
        return StringUtils.hasText(queueTokenHeader) ? queueTokenHeader : queueTokenCookie;
    }

    @Schema(description = "예매 요청 body")
    public record BookingRequest(
            @Schema(description = "예매할 좌석 ID", example = "101")
            Long seatId
    ) {}
}
