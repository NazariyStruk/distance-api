package com.fuchs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;

@SpringBootApplication
public class DistanceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DistanceApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // Додаємо підтримку UTF-8 для тексту примусово
        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return restTemplate;
    }
}

