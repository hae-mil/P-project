package AI_Secretary.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AiServerConfig {

    // application.yml에서 가져올 값, 없으면 기본값으로 FastAPI 서버 로컬 주소 사용
    @Value("${ai.server.base-url:http://127.0.0.1:8000}")
    private String aiBaseUrl;

    @Bean
    public WebClient aiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(aiBaseUrl) // 반드시 http:// 로 시작하는 풀 URL이어야 함
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}