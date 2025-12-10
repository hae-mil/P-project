package AI_Secretary.DTO.AdminDTO;

public record AdminPolicyChangeReportDto(
        Long id,
        Long policyId,
        String policyName,
        String title,
        String summary,
        String whatChanged,
        String whoAffected,
        String fromWhen,
        String actionGuide,
        String createdAt
) {
}
