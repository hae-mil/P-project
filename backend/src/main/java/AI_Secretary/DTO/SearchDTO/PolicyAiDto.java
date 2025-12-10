package AI_Secretary.DTO.SearchDTO;

import AI_Secretary.DTO.AiDTO.AiGuideResponse;

import java.time.LocalDateTime;
import java.util.List;

public record PolicyAiDto(
        String easyText,                 // 쉬운 설명
        String summary,                  // AI 요약
        List<String> keyPoints,          // 핵심 포인트
        List<PolicyFaqDto> faq,          // 자주 묻는 질문
        AiGuideResponse guide,           // 🔥 새로 추가된 신청 도우미(5W1H)
        LocalDateTime analyzedAt         // 분석 시각
) {
}
