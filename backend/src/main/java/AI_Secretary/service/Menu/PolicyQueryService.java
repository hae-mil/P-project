package AI_Secretary.service.Menu;

import AI_Secretary.DTO.AiDTO.AiGuideResponse;
import AI_Secretary.DTO.SearchDTO.*;
import AI_Secretary.Exceptions.PolicyNotFoundException;
import AI_Secretary.domain.policyData.DocumentAiResult;
import AI_Secretary.domain.policyData.PolicyData;
import AI_Secretary.domain.policyData.PolicyRequiredDocument;
import AI_Secretary.domain.user.UserInterests;
import AI_Secretary.domain.user.UserProfile;
import AI_Secretary.repository.User.UserInterestsRepository;
import AI_Secretary.repository.User.UserProfileRepository;
import AI_Secretary.repository.User.UserRepository;
import AI_Secretary.repository.search.*;
import AI_Secretary.repository.sideService.CalendarEventRepository;
import AI_Secretary.service.Ai.AiGuideService;
import AI_Secretary.service.Ai.AiSummaryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PolicyQueryService {

    private final PolicyDataRepository policyDataRepository;
    private final PolicyBookmarkRepository policyBookmarkRepository;
    private final PolicyRequiredDocumentRepository policyRequiredDocumentRepository;
    private final PolicyCheckListRepository policyChecklistRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final DocumentAiResultRepository documentAiResultRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserInterestsRepository userInterestsRepository;
    private final AiGuideService aiGuideService;
    private final AiSummaryService aiSummaryService;
    @Transactional(readOnly = true)
    public List<PolicySummaryDto> searchPolicies(String keyword) {
        return policyDataRepository.searchByKeyword(keyword)
                .stream()
                .map(this::toPolicySummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PolicySummaryDto> getRecommendedPolicies(Long userId, int limit) {

        // 1) 프로필 필수: 없으면 추천을 할 수 없으니 예외 처리 또는 빈 리스트
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("프로필이 존재하지 않습니다. userId=" + userId));

        String regionCtpv = profile.getRegionCtpv(); // 시/도 단위 지역 코드

        // 2) 유저 관심 카테고리 코드 (user_interests 테이블)
        List<UserInterests> interests = userInterestsRepository.findByUser_Id(userId);
        Set<String> recommendedCategoryCodes = interests.stream()
                .map(i -> i.getCategory().getCode()) // ex) "HEALTH", "JOB", "LOCAL" ...
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new)); // 순서 유지 + 중복 제거

        // 3) 복지정보(welfareInfo) 기반으로 카테고리 코드 확장
        enrichCategoryCodesByWelfareInfo(profile, recommendedCategoryCodes);

        // JPA 쿼리 메서드 파라미터용 List
        List<String> categoryCodesParam = recommendedCategoryCodes.isEmpty()
                ? null          // null이면 "카테고리 조건 없이" 레포 쿼리하도록 설계 가능
                : new ArrayList<>(recommendedCategoryCodes);

        // 4) 레포지토리에서 후보 정책 조회 (지역 + 카테고리 기반 필터링)
        List<PolicyData> raw = policyDataRepository.findRecommendedForUser(
                regionCtpv,
                categoryCodesParam
        );

        // 5) 사용자 선호 카테고리 우선 정렬 + 마감일 기준 보조 정렬
        List<PolicyData> sorted = sortByPersonalPriority(raw, recommendedCategoryCodes);

        // 6) limit 만큼만 잘라서 DTO 변환
        return sorted.stream()
                .limit(limit)
                .map(this::toPolicySummaryDto)
                .toList();
    }
    private void enrichCategoryCodesByWelfareInfo(UserProfile profile, Set<String> codes) {
        if (profile == null) return;

        String incomeLevel = profile.getIncomeLevel();
        if ("basic".equalsIgnoreCase(incomeLevel) || "near".equalsIgnoreCase(incomeLevel)) {
            codes.add("FINANCE");
            codes.add("LOCAL");
        }

        if (Boolean.TRUE.equals(profile.getHasDisability())) {
            codes.add("SENIOR");
            codes.add("LOCAL");
        }

        if (Boolean.TRUE.equals(profile.getLivingAlone())) {
            codes.add("LOCAL");
        }

        if (profile.getAge() != null && profile.getAge() >= 65) {
            codes.add("SENIOR");
        }
    }

    private List<PolicyData> sortByPersonalPriority(List<PolicyData> list, Set<String> preferredCodes) {
        if (list == null || list.isEmpty()) return List.of();
        if (preferredCodes == null || preferredCodes.isEmpty()) {
            // 선호 카테고리가 없으면 endDate 기준으로만 정렬
            return list.stream()
                    .sorted(Comparator.comparing(
                            PolicyData::getEndDate,
                            Comparator.nullsLast(Comparator.naturalOrder())
                    ))
                    .toList();
        }

        return list.stream()
                .sorted((p1, p2) -> {
                    String c1 = p1.getMainCategory() != null ? p1.getMainCategory().getCode() : null;
                    String c2 = p2.getMainCategory() != null ? p2.getMainCategory().getCode() : null;

                    boolean pref1 = c1 != null && preferredCodes.contains(c1);
                    boolean pref2 = c2 != null && preferredCodes.contains(c2);

                    // 1순위: 선호 카테고리 여부
                    if (pref1 != pref2) {
                        return pref1 ? -1 : 1; // pref1이 true면 위로
                    }

                    // 2순위: 마감일(endDate) 오름차순 (null은 제일 뒤)
                    LocalDate e1 = p1.getEndDate();
                    LocalDate e2 = p2.getEndDate();
                    if (e1 == null && e2 == null) return 0;
                    if (e1 == null) return 1;
                    if (e2 == null) return -1;
                    return e1.compareTo(e2);
                })
                .toList();
    }

    public PolicyDetailResponse getPolicyDetail(Long policyId, Long userIdOrNull) {
        PolicyData policy = policyDataRepository
                .findByIdWithCategory(policyId)
                .orElseThrow(() -> new PolicyNotFoundException(policyId));

        // 1) 기본 정보 DTO
        PolicyBasicDto basicDto = mapToBasicDto(policy);

        // 2) AI 분석 관련 (문서 기반 결과)
        DocumentAiResult aiResult = documentAiResultRepository
                .findTopByDocument_Policy_IdOrderByCreatedAtDesc(policyId)
                .orElse(null);

        // 2-1) 요약용 텍스트 생성 후 FastAPI /summary 호출
        String summarySourceText = buildSummarySourceText(policy, aiResult);
        String aiSummary = aiSummaryService.summarizeText(summarySourceText);

        // 2-2) 신청 도우미(AiGuideResponse)
        AiGuideResponse guide = aiGuideService.getAiGuideForPolicy(policyId);

        // 2-3) 최종 PolicyAiDto 구성
        PolicyAiDto aiDto = mapToAiDto(policy, aiResult, aiSummary, guide);

        // 3) 필수 서류 목록
        List<PolicyRequiredDocumentDto> docDtos =
                policyRequiredDocumentRepository
                        .findByPolicy_IdOrderBySortOrderAsc(policyId)
                        .stream()
                        .map(this::mapToRequiredDocumentDto)
                        .toList();

        // 4) 유저 컨텍스트
        PolicyUserContextDto userContext = null;
        if (userIdOrNull != null) {
            boolean bookmarked =
                    policyBookmarkRepository.existsByUserIdAndPolicyId(userIdOrNull, policyId);

            boolean hasChecklist =
                    policyChecklistRepository.existsByUserIdAndPolicyId(userIdOrNull, policyId);

            LocalDate nearestEventDate =
                    calendarEventRepository
                            .findNearestDateByUserIdAndPolicyId(userIdOrNull, policyId)
                            .orElse(null);

            userContext = new PolicyUserContextDto(bookmarked, hasChecklist, nearestEventDate);
        }

        // ✅ 이제 guide를 따로 넘기지 않고, aiDto 안에 포함된 상태로 리턴
        return new PolicyDetailResponse(
                basicDto,
                aiDto,
                docDtos,
                userContext
        );
    }

    // --------------------------------------------------------------------
    // DTO 매핑 로직
    // --------------------------------------------------------------------
    /**
     * 상세 기본 정보 DTO
     * 엔티티 구조에 맞게 필드를 매핑
     */
    private PolicyBasicDto mapToBasicDto(PolicyData p) {
        String applicationPeriod = buildApplicationPeriod(p.getStartDate(), p.getEndDate());

        return new PolicyBasicDto(
                p.getId(),
                p.getName(), // title
                p.getMainCategory() != null ? p.getMainCategory().getName() : null, // categoryName
                p.getDeptName(),    // provider (주관부처/기관)
                p.getRegionCtpv(),
                p.getRegionSgg(),
                p.getSupportCycle(),   // supportType: 현재는 supportCycle을 매핑
                applicationPeriod,
                p.getLifeCycle(),      // targetDescription: lifeCycle을 임시로 사용
                p.getSummary(),        // summaryText
                p.getLastCrawledAt() != null ? p.getLastCrawledAt() : p.getLastModifiedAt()
        );
    }

    private String buildApplicationPeriod(LocalDate start, LocalDate end) {
        if (start == null && end == null) {
            return "상시";
        }
        if (start != null && end != null) {
            return start + " ~ " + end;
        }
        if (start != null) {
            return start + " ~";
        }
        return "~ " + end;
    }

    /**
     * AI 분석 결과 DTO
     * DocumentAiResult 엔티티 구조에 맞춰 수정
     */
    private PolicyAiDto mapToAiDto(
            PolicyData policy,
            DocumentAiResult r,
            String aiSummary,
            AiGuideResponse guide
    ) {
        if (policy == null && r == null && guide == null) {
            return null;
        }

        String easyText = (r != null) ? r.getEasyExplanationText() : null;
        List<String> keyPoints = (r != null) ? parseJsonArray(r.getKeywordsJson()) : List.of();
        List<PolicyFaqDto> faqList = (r != null) ? parseFaqJson(r.getQaTemplateJson()) : List.of();
        LocalDateTime analyzedAt = (r != null) ? r.getCreatedAt() : null;

        // AI 요약이 null이면 DocumentAiResult.summaryText를 fallback으로
        String summary = aiSummary != null
                ? aiSummary
                : (r != null ? r.getSummaryText() : null);

        return new PolicyAiDto(
                easyText,
                summary,
                keyPoints,
                faqList,
                guide,
                analyzedAt
        );
    }

    /**
     * 필수 서류 DTO
     * 현재 스키마에 없는 description/required는 임시 값 처리
     */
    private PolicyRequiredDocumentDto mapToRequiredDocumentDto(PolicyRequiredDocument d) {
        return new PolicyRequiredDocumentDto(
                d.getId(),
                d.getDocName(),                           // name
                null,                                     // description: 컬럼 없음 → null
                d.getSourceType() != null ? d.getSourceType().name() : null,
                Boolean.TRUE,                             // required: 정보 없음 → 일단 true
                d.getFileUri()                            // exampleUrl: 파일/양식 URL
        );
    }
    private String buildSummarySourceText(PolicyData policy, DocumentAiResult aiResult) {
        // 1️⃣ 1순위: Document.contentText 기반 요약
        String rawContent = null;
        if (aiResult != null && aiResult.getDocument() != null) {
            rawContent = aiResult.getDocument().getContentText(); // ← content_text
        }

        if (rawContent != null && !rawContent.isBlank()) {
            StringBuilder sb = new StringBuilder();

            // 원문 전체를 먼저 넣고
            sb.append(rawContent).append("\n\n");

            // 그 위에 요약 지시 프롬프트만 살짝 얹어준다
            sb.append(
                    "위 복지 정책 안내문을 바탕으로, [지원대상], [지원내용], [신청방법]을 중심으로 " +
                            "노년층도 이해하기 쉬운 2~3문장 요약을 한국어로 작성해 주세요. " +
                            "전화번호, 법령명, 서식 파일명 등은 요약에 포함하지 않아도 됩니다."
            );

            return sb.toString();
        }

        // 2️⃣ fallback: content_text가 없으면 기존 메타데이터 기반 요약으로 생성
        StringBuilder sb = new StringBuilder();
        sb.append("다음은 복지 정책에 대한 설명입니다.\n\n");
        sb.append("[정책명] ").append(policy.getName()).append("\n\n");

        if (policy.getSummary() != null) {
            sb.append("[정책 요약] ").append(policy.getSummary()).append("\n\n");
        }
        if (policy.getLifeCycle() != null) {
            sb.append("[대상 계층] ").append(policy.getLifeCycle()).append("\n\n");
        }
        if (policy.getSupportCycle() != null) {
            sb.append("[지원 형태 또는 주기] ").append(policy.getSupportCycle()).append("\n\n");
        }
        if (aiResult != null && aiResult.getEasyExplanationText() != null) {
            sb.append("[쉬운 설명] ").append(aiResult.getEasyExplanationText()).append("\n\n");
        }

        sb.append("위 내용을 기반으로, 노년층도 이해하기 쉬운 2~3문장의 간단한 요약을 한국어로 작성해 주세요.");

        return sb.toString();
    }
    // --------------------------------------------------------------------
    // JSON 파싱
    // --------------------------------------------------------------------

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // log.warn("Failed to parse keywords json: {}", json, e);
            return List.of(); // 실패 시 빈 리스트
        }
    }

    private List<PolicyFaqDto> parseFaqJson(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, new TypeReference<List<PolicyFaqDto>>() {});
        } catch (Exception e) {
            // log.warn("Failed to parse FAQ json: {}", json, e);
            return List.of();
        }
    }
    // 🔽 공통 변환 로직 분리
    private PolicySummaryDto toPolicySummaryDto(PolicyData p) {
        return new PolicySummaryDto(
                p.getId(),
                p.getName(),
                p.getSummary(),
                p.getMainCategory() != null ? p.getMainCategory().getCode() : null,
                p.getMainCategory() != null ? p.getMainCategory().getName() : null, // 추가
                p.getRegionCtpv(),
                p.getRegionSgg(),
                p.getDeptName(),    // 기관명
                p.getSupportCycle(),// 지원유형
                p.getOnapPossible()
        );
    }
}
