package AI_Secretary.DTO.AiDTO;

import java.util.List;

public record AiGuideResponse(
        String who,
        String when,
        String where,
        String what,
        String how,
        String why,
        List<String> keywords
) {}