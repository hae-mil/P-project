package AI_Secretary.DTO.AlarmDto;

public record PolicyChangeReportForUserDto(
        Long id,
        Long policyId,
        String policyName,
        String title,
        String summary,
        String whatChanged,
        String whoAffected,
        String fromWhen,
        String actionGuide
) {
}
