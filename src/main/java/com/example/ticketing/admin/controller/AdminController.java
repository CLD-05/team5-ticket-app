package com.example.ticketing.admin.controller;

import com.example.ticketing.admin.dto.BulkCreateSeatsRequest;
import com.example.ticketing.admin.dto.CreateShowRequest;
import com.example.ticketing.admin.dto.QueueResetRequest;
import com.example.ticketing.admin.service.AdminService;
import com.example.ticketing.global.exception.BusinessException;
import com.example.ticketing.global.exception.ErrorCode;
import com.example.ticketing.show.entity.Show;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "어드민 전용 API")
public class AdminController {

    private final AdminService adminService;

    @Value("${app.admin.token:admin-secret-key}")
    private String adminToken;

    private void validateAdminToken(String token) {
        if (token == null) {
            throw new BusinessException("올바른 어드민 인증 토큰이 필요합니다.", ErrorCode.ACCESS_DENIED);
        }
        boolean isStaticTokenValid = token.equals(adminToken);
        boolean isTotpValid = com.example.ticketing.admin.util.TotpUtil.verifyOtp("TICKETWAVEADMINX", token);
        if (!isStaticTokenValid && !isTotpValid) {
            throw new BusinessException("올바른 어드민 인증 토큰이 필요합니다.", ErrorCode.ACCESS_DENIED);
        }
    }

    @Operation(summary = "신규 공연 등록", description = "신규 공연을 등록합니다. (어드민 토큰 검증 필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공연 등록 성공",
                    content = @Content(schema = @Schema(implementation = Show.class))),
            @ApiResponse(responseCode = "403", description = "어드민 권한 없음", content = @Content)
    })
    @PostMapping("/shows")
    public ResponseEntity<Show> createShow(
            @Parameter(description = "어드민 인증 토큰", example = "admin-secret-key")
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody CreateShowRequest request
    ) {
        validateAdminToken(token);
        Show show = adminService.createShow(request);
        return ResponseEntity.ok(show);
    }

    @Operation(summary = "공연 좌석 일괄 생성", description = "특정 공연에 대한 좌석 및 좌석 등급을 일괄 생성합니다. (어드민 토큰 검증 필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "좌석 일괄 생성 성공"),
            @ApiResponse(responseCode = "403", description = "어드민 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "공연을 찾을 수 없음", content = @Content)
    })
    @PostMapping("/shows/{showId}/seats/bulk")
    public ResponseEntity<String> bulkCreateSeats(
            @Parameter(description = "어드민 인증 토큰", example = "admin-secret-key")
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @Parameter(description = "공연 ID", example = "1")
            @PathVariable Long showId,
            @RequestBody BulkCreateSeatsRequest request
    ) {
        validateAdminToken(token);
        adminService.bulkCreateSeats(showId, request);
        int totalCreated = (request.getGrades() != null) 
                ? request.getGrades().stream().mapToInt(BulkCreateSeatsRequest.GradeItem::getTotalSeats).sum()
                : 0;
        return ResponseEntity.ok("Successfully created " + totalCreated + " seats for show " + showId);
    }

    @Operation(summary = "공연 삭제", description = "공연을 강제 삭제합니다. 관련 예매, 좌석, 대기열 정보도 모두 삭제됩니다. (어드민 토큰 검증 필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "공연 삭제 성공"),
            @ApiResponse(responseCode = "430", description = "어드민 권한 없음", content = @Content),
            @ApiResponse(responseCode = "404", description = "공연을 찾을 수 없음", content = @Content)
    })
    @DeleteMapping("/shows/{showId}")
    public ResponseEntity<String> deleteShow(
            @Parameter(description = "어드민 인증 토큰", example = "admin-secret-key")
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @Parameter(description = "공연 ID", example = "1")
            @PathVariable Long showId
    ) {
        validateAdminToken(token);
        adminService.deleteShow(showId);
        return ResponseEntity.ok("Successfully deleted show " + showId);
    }

    @Operation(summary = "대기열 강제 리셋", description = "특정 공연의 대기열 또는 전체 공연의 대기열을 강제로 초기화(리셋)합니다. (어드민 토큰 검증 필요)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대기열 리셋 성공"),
            @ApiResponse(responseCode = "403", description = "어드민 권한 없음", content = @Content)
    })
    @PostMapping("/queue/reset")
    public ResponseEntity<String> resetQueue(
            @Parameter(description = "어드민 인증 토큰", example = "admin-secret-key")
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @RequestBody(required = false) QueueResetRequest request
    ) {
        validateAdminToken(token);
        Long showId = (request != null) ? request.getShowId() : null;
        adminService.resetQueue(showId);
        if (showId != null) {
            return ResponseEntity.ok("Successfully reset queue for show " + showId);
        } else {
            return ResponseEntity.ok("Successfully reset all queues");
        }
    }

    @Operation(summary = "공연 예매 시간 수정", description = "기존 공연의 예매 시작/마감 및 공연 시간을 수정합니다. (어드민 토큰 검증 필요)")
    @PostMapping("/shows/{showId}/booking-time")
    public ResponseEntity<String> updateBookingTime(
            @Parameter(description = "어드민 인증 토큰", example = "admin-secret-key")
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @Parameter(description = "공연 ID", example = "1")
            @PathVariable Long showId,
            @RequestBody CreateShowRequest request
    ) {
        validateAdminToken(token);
        adminService.updateBookingTime(showId, request);
        return ResponseEntity.ok("Successfully updated booking time for show " + showId);
    }

    @Operation(summary = "공연 포스터 이미지 업로드", description = "공연의 대표 포스터 이미지를 업로드하고 URL을 갱신합니다. (어드민 토큰 검증 필요)")
    @PostMapping(value = "/shows/{showId}/image", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateShowImage(
            @Parameter(description = "어드민 인증 토큰", example = "admin-secret-key")
            @RequestHeader(value = "X-Admin-Token", required = false) String token,
            @Parameter(description = "공연 ID", example = "1")
            @PathVariable Long showId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) {
        validateAdminToken(token);
        String imageUrl = adminService.updateShowImage(showId, file);
        return ResponseEntity.ok(imageUrl);
    }

    @Operation(summary = "어드민 토큰 검증", description = "입력한 어드민 토큰 또는 OTP 코드의 유효성을 검증하고, 성공 시 세션 유지용 어드민 토큰을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "검증 성공", content = @Content(schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "403", description = "검증 실패 (권한 없음)", content = @Content)
    })
    @PostMapping("/validate")
    public ResponseEntity<String> validateTokenOnly(
            @Parameter(description = "어드민 인증 토큰 또는 OTP 번호", example = "admin-secret-key")
            @RequestHeader(value = "X-Admin-Token", required = false) String token
    ) {
        validateAdminToken(token);
        return ResponseEntity.ok(adminToken);
    }
}
