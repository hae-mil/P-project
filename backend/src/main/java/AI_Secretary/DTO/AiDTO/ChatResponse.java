package AI_Secretary.DTO.AiDTO;

import java.util.List;

public record ChatResponse(
        String answer,
        List<String> keyPoints,
        List<String> nextActions
) {
}
