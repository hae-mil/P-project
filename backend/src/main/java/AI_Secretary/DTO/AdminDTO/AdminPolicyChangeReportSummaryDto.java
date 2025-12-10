package AI_Secretary.DTO.AdminDTO;

public record AdminPolicyChangeReportSummaryDto(
        Long id,
        Long policyId,
        String policyName,
        String title,
        String summary,
        String createdAt  // 문자열로 내려주면 FE에서 포맷 편함
) {
}
