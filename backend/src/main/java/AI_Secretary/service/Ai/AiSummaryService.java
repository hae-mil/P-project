package AI_Secretary.service.Ai;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AiSummaryService {

    private final WebClient aiWebClient;

    /**
     * FastAPI /summary 엔드포인트 호출
     * 요청: { "question": "..." }
     * 응답: "요약문 문자열"
     */
    public String summarizeText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Map<String, Object> requestBody = Map.of(
                "question", text
        );

        return aiWebClient.post()
                .uri("/summary")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)          // { "question": "..." } 전송
                .retrieve()
                .bodyToMono(String.class)        // 응답은 그냥 문자열
                .timeout(Duration.ofSeconds(30)) // 타임아웃 (원하면 조정 가능)
                .onErrorResume(ex -> {
                    // 실패 시 null 리턴 → FE에서 "-" 처리 등
                    return Mono.empty();
                })
                .block();
    }
}