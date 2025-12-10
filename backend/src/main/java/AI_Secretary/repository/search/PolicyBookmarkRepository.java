package AI_Secretary.repository.search;

import AI_Secretary.domain.subMenus.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyBookmarkRepository extends JpaRepository<Bookmark,Long> {
    List<Bookmark> findByUser_Id(Long userId);

    List<Bookmark> findByPolicy_Id(Long policyId);

    boolean existsByUserIdAndPolicyId(Long userId, Long policyId);

    void deleteByUser_IdAndPolicy_Id(Long userId, Long policyId);
}
