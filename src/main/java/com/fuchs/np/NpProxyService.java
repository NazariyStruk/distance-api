package com.fuchs.np;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NpProxyService {

    private final String apiUrl ="https://api.novaposhta.ua/v2.0/json/";

    private final String apiKey = "1a19db8f32df559f97775c892743aca1"; // Твій ключ з налаштувань

    private final RestTemplate restTemplate = new RestTemplate();

    public String sendRequest(ApiRequestDto requestDto) {
        try {
            // 1. Впроваджуємо наш секретний ключ у запит
            requestDto.setApiKey(this.apiKey);

            // 2. Налаштовуємо заголовки (стандарт для JSON)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 3. Формуємо запит
            HttpEntity<ApiRequestDto> entity = new HttpEntity<>(requestDto, headers);

            // 4. Відправляємо на сервери НП
            // Використовуємо postForObject, щоб отримати відповідь як String (Raw JSON)

            return restTemplate.postForObject(apiUrl, entity, String.class);

        } catch (Exception e) {
            e.printStackTrace();
            return "{\"success\": false, \"errors\": [\"Java Proxy Error: " + e.getMessage() + "\"]}";
        }
    }
}
