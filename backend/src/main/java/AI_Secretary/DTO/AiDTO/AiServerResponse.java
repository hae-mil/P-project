package AI_Secretary.DTO.AiDTO;

import java.util.List;

public record AiServerResponse(
        List<String> keywords,
        Guide guide
) {
    public record Guide(
            String who,
            String when,
            String where,
            String what,
            String how,
            String why
    ) {}
}