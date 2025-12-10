package AI_Secretary.DTO.AdminDTO;

import AI_Secretary.domain.policyData.PolicyChangeLog;

import java.time.LocalDateTime;

public record DashboardChangeReportDto(Long id,
                                       LocalDateTime changedAt,
                                       String title,          // 리포트 제목 (정책명 + 요약)
                                       PolicyChangeLog.ChangeType status,     // NEW / UPDATE / DELETE (UI에서 뱃지로 표시)
                                       String owner    ) {
}
