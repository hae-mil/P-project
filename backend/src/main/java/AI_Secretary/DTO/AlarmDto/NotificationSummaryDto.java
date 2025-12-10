package AI_Secretary.DTO.AlarmDto;

public record NotificationSummaryDto(
        Long id,
        String type,           // "CHANGE_POLICY" 등
        String title,
        String messagePreview, // 앞부분 몇 자만 잘라서
        boolean isRead,
        String createdAt,      // "2025-12-09 10:30"
        boolean hasReport,
        Long reportId
) {
}
