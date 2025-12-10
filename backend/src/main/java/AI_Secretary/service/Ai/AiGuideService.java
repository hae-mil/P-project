package AI_Secretary.service.Ai;

import AI_Secretary.DTO.AiDTO.*;
import AI_Secretary.domain.policyData.DocumentAiResult;
import AI_Secretary.domain.policyData.PolicyData;
import AI_Secretary.repository.search.DocumentAiResultRepository;
import AI_Secretary.repository.search.PolicyDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiGuideService {
    private final PolicyDataRepository policyDataRepository;
    private final DocumentAiResultRepository documentAiResultRepository;
    private final WebClient aiWebClient;   // AiServerConfig 에서 만든 Bean

    public AiGuideResponse getAiGuideForPolicy(Long policyId) {

        // 1) 정책 기본 정보
        PolicyData policy = policyDataRepository.findById(policyId)
                .orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다. id=" + policyId));

        // 2) 기존에 크롤링/요약해둔 AI 결과(있으면 더 추가)
        DocumentAiResult aiResult = documentAiResultRepository
                .findTopByDocument_Policy_IdOrderByCreatedAtDesc(policyId)
                .orElse(null);

        // 3) FastAPI에 보낼 question 텍스트 구성
        StringBuilder sb = new StringBuilder();
        sb.append("다음은 복지 정책 설명입니다.\n\n");
        sb.append("[정책명] ").append(policy.getName()).append("\n\n");
        if (policy.getSummary() != null) {
            sb.append("[요약 설명] ").append(policy.getSummary()).append("\n\n");
        }
        if (policy.getLifeCycle() != null) {
            sb.append("[대상 계층] ").append(policy.getLifeCycle()).append("\n\n");
        }
        if (aiResult != null && aiResult.getEasyExplanationText() != null) {
            sb.append("[쉬운 설명] ").append(aiResult.getEasyExplanationText()).append("\n\n");
        }

        String question = sb.toString();

        // 4) FastAPI /aiHelper 호출
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("question", question);

        AiServerResponse serverRes = aiWebClient.post()
                .uri("/aiHelper")   // FastAPI 쪽 라우트
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(AiServerResponse.class)
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(ex -> {
                    // 실패 시 null 리턴 → FE 에서 "-”로 표시하게 놔두기
                    return Mono.empty();
                })
                .block();

        if (serverRes == null || serverRes.guide() == null) {
            // 최소한 빈 값이라도 채워서 리턴
            return new AiGuideResponse(null, null, null, null, null, null, null);
        }

        var g = serverRes.guide();

        return new AiGuideResponse(
                g.who(),
                g.when(),
                g.where(),
                g.what(),
                g.how(),
                g.why(),
                serverRes.keywords()
        );
    }

    //정책 상세보기 AI 요약기능
    public WelfareAnswer askWelfareQuestion(String question) {
        AiHelperRequest request = new AiHelperRequest(question);

        AiHelperResponse response = aiWebClient.post()
                .uri("/aiHelper")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(AiHelperResponse.class)
                .block(); // 필요하면 timeout, 예외 처리 래핑 가능

        if (response == null) {
            // 실패 시에 대한 기본 처리 (예: fallback 메시지)
            return new WelfareAnswer(
                    "AI 서버와 통신에 실패했습니다. 잠시 후 다시 시도해주세요.",
                    null,
                    null
            );
        }

        return WelfareAnswer.from(response);
    }
}
