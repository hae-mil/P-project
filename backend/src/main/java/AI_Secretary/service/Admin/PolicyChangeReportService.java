package AI_Secretary.service.Admin;

import AI_Secretary.DTO.AdminDTO.AdminPolicyChangeReportDto;
import AI_Secretary.DTO.AdminDTO.AdminPolicyChangeReportSummaryDto;
import AI_Secretary.domain.policyData.PolicyData;
import AI_Secretary.domain.subMenus.Bookmark;
import AI_Secretary.domain.subMenus.Notification;
import AI_Secretary.domain.subMenus.PolicyChangeReport;
import AI_Secretary.domain.user.UserSettings;
import AI_Secretary.domain.user.users;
import AI_Secretary.repository.Alarm.NotificationRepository;
import AI_Secretary.repository.Alarm.PolicyChangeReportRepository;
import AI_Secretary.repository.User.UserSettingsRepository;
import AI_Secretary.repository.search.PolicyBookmarkRepository;
import AI_Secretary.repository.sideService.BookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyChangeReportService {
    private final PolicyChangeReportRepository reportRepository;
    private final PolicyBookmarkRepository bookmarkRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final NotificationRepository notificationRepository;

    private static final DateTimeFormatter DATE_TIME_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 관리자 목록 화면용: 전체 보고서를 최신순으로 반환
     */
    @Transactional(readOnly = true)
    public List<AdminPolicyChangeReportSummaryDto> getReportList(Long policyIdOrNull) {

        List<PolicyChangeReport> list;

        if (policyIdOrNull != null) {
            list = reportRepository.findByPolicy_IdOrderByCreatedAtDesc(policyIdOrNull);
        } else {
            list = reportRepository.findAllByOrderByCreatedAtDesc();
        }

        return list.stream()
                .map(this::toSummaryDto)
                .toList();
    }

    /**
     * 관리자 상세 화면용: 단일 보고서 조회
     */
    @Transactional(readOnly = true)
    public AdminPolicyChangeReportDto getReportDetail(Long reportId) {
        PolicyChangeReport r = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("보고서를 찾을 수 없습니다. id=" + reportId));

        return toDetailDto(r);
    }

    private AdminPolicyChangeReportSummaryDto toSummaryDto(PolicyChangeReport r) {
        PolicyData p = r.getPolicy();
        return new AdminPolicyChangeReportSummaryDto(
                r.getId(),
                p != null ? p.getId() : null,
                p != null ? p.getName() : null,
                r.getTitle(),
                r.getSummary(),
                r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_TIME_FMT) : null
        );
    }

    private AdminPolicyChangeReportDto toDetailDto(PolicyChangeReport r) {
        PolicyData p = r.getPolicy();
        return new AdminPolicyChangeReportDto(
                r.getId(),
                p != null ? p.getId() : null,
                p != null ? p.getName() : null,
                r.getTitle(),
                r.getSummary(),
                r.getWhatChanged(),
                r.getWhoAffected(),
                r.getFromWhen(),
                r.getActionGuide(),
                r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_TIME_FMT) : null
        );
    }

    /**
     * ✅ 관리자 "승인 및 배포" 눌렀을 때:
     *  - 이 정책을 북마크한 유저들 중
     *  - 정책 변경 알림 허용한 사람들에게
     *  - notification 테이블에 레코드 생성
     */
    @Transactional
    public void approveAndNotify(Long reportId) {
        PolicyChangeReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("보고서를 찾을 수 없습니다. id=" + reportId));

        PolicyData policy = report.getPolicy();

        // 1) 이 정책을 북마크한 사용자들
        List<Bookmark> bookmarks = bookmarkRepository.findByPolicy_Id(policy.getId());

        for (Bookmark bm : bookmarks) {
            users user = bm.getUser();

            // 2) 알림 설정 확인
            UserSettings settings = userSettingsRepository.findByUser_Id(user.getId())
                    .orElse(null);
            if (settings != null && !settings.getNotifyPolicyChanges()) {
                continue;
            }

            // 3) 알림 생성
            Notification notification = Notification.builder()
                    .user(user)
                    .type(Notification.NotificationType.CHANGE_POLICY)
                    .title(report.getTitle())
                    .message(report.getSummary())     // 혹은 report.getWhatChanged()
                    .policyChangeReport(report)
                    .read(false)
                    .build();

            notificationRepository.save(notification);
        }
    }
}
