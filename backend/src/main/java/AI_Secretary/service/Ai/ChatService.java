package AI_Secretary.service.Ai;

import AI_Secretary.DTO.AiDTO.ChatRequest;
import AI_Secretary.DTO.AiDTO.ChatResponse;
import AI_Secretary.DTO.AiDTO.WelfareAnswer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final AiGuideService aiGuideService;
    // private final PolicySearchService policySearchService; // 추후 정책 검색 연계 시 주입

    public ChatResponse chat(ChatRequest request/*, Long userId*/) {

        // TODO: userId 기반으로 유저 프로필 요약을 붙여서 question에 포함시키는 로직 추가 가능
        String question = request.question();

        WelfareAnswer welfareAnswer = aiGuideService.askWelfareQuestion(question);

        return new ChatResponse(
                welfareAnswer.answer(),
                welfareAnswer.keyPoints(),
                welfareAnswer.nextActions()
        );
    }
}