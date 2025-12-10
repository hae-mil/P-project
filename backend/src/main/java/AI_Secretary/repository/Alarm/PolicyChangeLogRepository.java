package AI_Secretary.repository.Alarm;

import AI_Secretary.domain.policyData.PolicyChangeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PolicyChangeLogRepository extends JpaRepository<PolicyChangeLog, Long> {


    long countByChangedAtBetween(LocalDateTime from, LocalDateTime to);

    List<PolicyChangeLog> findTop10ByOrderByChangedAtDesc();
}