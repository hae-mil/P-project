package AI_Secretary.service.Menu;

import AI_Secretary.DTO.AlarmDto.NotificationDetailResponse;
import AI_Secretary.DTO.AlarmDto.NotificationSummaryDto;
import AI_Secretary.DTO.AlarmDto.PolicyChangeReportForUserDto;
import AI_Secretary.domain.policyData.PolicyData;
import AI_Secretary.domain.subMenus.Notification;
import AI_Secretary.domain.subMenus.PolicyChangeReport;
import AI_Secretary.repository.Alarm.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * ✅ 내 알림 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NotificationSummaryDto> getMyNotifications(Long userId) {
        List<Notification> list = notificationRepository
                .findByUser_IdOrderByCreatedAtDesc(userId);

        return list.stream()
                .map(this::toSummaryDto)
                .toList();
    }

    private NotificationSummaryDto toSummaryDto(Notification n) {
        String msg = n.getMessage();
        String preview = null;
        if (msg != null) {
            preview = msg.length() > 40 ? msg.substring(0, 40) + "..." : msg;
        }

        return new NotificationSummaryDto(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                preview,
                n.isRead(),
                n.getCreatedAt() != null ? n.getCreatedAt().format(DATE_TIME_FMT) : null,
                n.getPolicyChangeReport() != null,
                n.getPolicyChangeReport() != null ? n.getPolicyChangeReport().getId() : null
        );
    }

    /**
     * ✅ 알림 상세 조회 (+변경 레포트 포함)
     *    - 여기에서 읽음 처리까지 같이 해버리는 버전
     */
    @Transactional
    public NotificationDetailResponse getNotificationDetail(Long userId, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다. id=" + notificationId));

        // 조회 시 읽음 처리 (원하면 별도 API로 분리 가능)
        n.markAsRead();

        PolicyChangeReportForUserDto reportDto = null;
        PolicyChangeReport r = n.getPolicyChangeReport();
        if (r != null) {
            reportDto = toReportDto(r);
        }

        return new NotificationDetailResponse(
                n.getId(),
                n.getType().name(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getCreatedAt() != null ? n.getCreatedAt().format(DATE_TIME_FMT) : null,
                n.getReadAt() != null ? n.getReadAt().format(DATE_TIME_FMT) : null,
                reportDto
        );
    }

    /**
     * ✅ 읽음 처리만 따로 하는 API용
     */
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다. id=" + notificationId));
        n.markAsRead();
    }

    private PolicyChangeReportForUserDto toReportDto(PolicyChangeReport r) {
        PolicyData p = r.getPolicy();
        return new PolicyChangeReportForUserDto(
                r.getId(),
                p != null ? p.getId() : null,
                p != null ? p.getName() : null,
                r.getTitle(),
                r.getSummary(),
                r.getWhatChanged(),
                r.getWhoAffected(),
                r.getFromWhen(),
                r.getActionGuide()
        );
    }
}
