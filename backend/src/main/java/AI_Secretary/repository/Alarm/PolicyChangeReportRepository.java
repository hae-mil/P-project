package AI_Secretary.repository.Alarm;

import AI_Secretary.domain.subMenus.PolicyChangeReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PolicyChangeReportRepository extends JpaRepository<PolicyChangeReport,Long> {
    // 예) 최근 생성 순으로 전체 조회
    List<PolicyChangeReport> findAllByOrderByCreatedAtDesc();

    // 특정 정책에 대한 레포트만 보고 싶을 때
    List<PolicyChangeReport> findByPolicy_IdOrderByCreatedAtDesc(Long policyId);

}

