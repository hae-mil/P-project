package AI_Secretary.DTO.AdminDTO;

import java.util.List;

public record DashboardSummaryResponse(
        long totalPolicies,                 // 총 등록 사업 수
        long todayReports,                  // 오늘 수집된 리포트 수
        String serverStatus,                // "정상" / "주의" / "장애" 등
        long todayAiApiCalls,               // 오늘 AI API 요청 수
        List<DashboardChangeReportDto> recentChangeReports,
        List<DashboardLogLineDto> recentLogs
) {
}
