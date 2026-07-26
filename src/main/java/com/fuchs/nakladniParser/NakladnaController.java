package com.fuchs.nakladniParser;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/nakladna")
public class NakladnaController {

    private final NakladnaProcessingService nakladnaService;

    public NakladnaController(NakladnaProcessingService nakladnaService) {
        this.nakladnaService = nakladnaService;
    }

    @PostMapping
    public ResponseEntity<String> createNakladna(@RequestBody NakladnaDto dto) {
        try {
            boolean updated = nakladnaService.processAndSaveNakladna(dto);
            if (updated) {
                return ResponseEntity.status(HttpStatus.OK).body("Накладна вже існувала — дані оновлено");
            }
            return ResponseEntity.status(HttpStatus.CREATED).body("Накладну успішно збережено в БД");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Помилка обробки даних: " + e.getMessage());
        }
    }
}
