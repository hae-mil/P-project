package AI_Secretary.DTO.AiDTO;

import java.util.List;

public record WelfareAnswer(
        String answer,
        List<String> keyPoints,
        List<String> nextActions
) {
    public static WelfareAnswer from(AiHelperResponse response) {
        return new WelfareAnswer(
                response.answer(),
                response.keyPoints(),
                response.nextActions()
        );
    }
}