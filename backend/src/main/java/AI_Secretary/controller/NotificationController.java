package AI_Secretary.controller;

import AI_Secretary.DTO.AlarmDto.NotificationDetailResponse;
import AI_Secretary.DTO.AlarmDto.NotificationSummaryDto;
import AI_Secretary.Security.CustomUserDetails;
import AI_Secretary.service.Menu.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    /**
     * ✅ 내 알림 목록 조회
     *    GET /api/v1/notifications
     */
    @GetMapping
    public ResponseEntity<List<NotificationSummaryDto>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        Long userId = user.getUserId();
        var list = notificationService.getMyNotifications(userId);
        return ResponseEntity.ok(list);
    }

    /**
     * ✅ 알림 상세 조회 (+읽음 처리)
     *    GET /api/v1/notifications/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationDetailResponse> getDetail(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id
    ) {
        Long userId = user.getUserId();
        var dto = notificationService.getNotificationDetail(userId, id);
        return ResponseEntity.ok(dto);
    }

    /**
     * ✅ 읽음 처리만 따로
     *    POST /api/v1/notifications/{id}/read
     */
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id
    ) {
        Long userId = user.getUserId();
        notificationService.markAsRead(userId, id);
        return ResponseEntity.ok().build();
    }
}
