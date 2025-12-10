package AI_Secretary.DTO.AiDTO;

import java.util.List;

public record AiHelperResponse(
        String answer,
        List<String> keyPoints,
        List<String> nextActions
) {
}
