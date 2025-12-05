package com.fuchs.np;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/np")
public class NpController {

    @Autowired
    private NpProxyService npProxyService;

    @PostMapping(value = "/execute", produces = "application/json; charset=utf-8")
    public ResponseEntity<String> executeMethod(@RequestBody ApiRequestDto requestDto) {
        // Ми приймаємо запит, в якому є ModelName і MethodName, але немає ключа
        String result = npProxyService.sendRequest(requestDto);

        // Повертаємо 1С точну відповідь від сервера
        return ResponseEntity.ok(result);
    }
}
