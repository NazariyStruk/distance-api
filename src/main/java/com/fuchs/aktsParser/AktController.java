package com.fuchs.aktsParser;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/acts")
public class AktController {

    private final AktService aktService;

    public AktController(AktService aktService) {
        this.aktService = aktService;
    }

    @PostMapping
    public ResponseEntity<String> createAkt(@RequestBody AktDto dto) {
        try {
            boolean updated = aktService.saveAkt(dto);
            if (updated) {
                return ResponseEntity.status(HttpStatus.OK).body("Акт вже існував — дані оновлено");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body("Акт успішно збережено в БД");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Помилка обробки даних: " + e.getMessage());
        }
    }
}
