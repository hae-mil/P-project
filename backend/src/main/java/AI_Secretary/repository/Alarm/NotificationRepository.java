package AI_Secretary.repository.Alarm;

import AI_Secretary.domain.subMenus.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUser_IdOrderByCreatedAtDesc(Long userId);
    // 특정 유저의 특정 알림 (소유자 체크용)
    Optional<Notification> findByIdAndUser_Id(Long id, Long userId);

    // 읽지 않은 알림 개수 (뱃지용 필요 시)
    long countByUser_IdAndReadFalse(Long userId);
}