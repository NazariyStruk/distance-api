package com.fuchs.invoicesParser;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceParserService parserService;
    private final MissingLinesRecoveryService recoveryService;

    public InvoiceController(InvoiceParserService parserService, MissingLinesRecoveryService recoveryService) {
        this.parserService = parserService;
        this.recoveryService = recoveryService;
    }

    @PostMapping("/parse")
    public ResponseEntity<String> parseInvoiceItem(@RequestBody InvoiceRequestDto request) {
        try {
            parserService.parseAndSave(request);
            return ResponseEntity.ok("Item parsed and saved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error parsing item: " + e.getMessage());
        }
    }

    @PostMapping("/reset-status")
    public ResponseEntity<String> resetStatus(@RequestBody InvoiceRequestDto request) {
        try {
            parserService.resetSyncStatus(request);
            return ResponseEntity.ok("Status 'updatedBy1C' reset to false(0) for matching records.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error resetting status: " + e.getMessage());
        }
    }

    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupInvoice(@RequestBody InvoiceRequestDto request) {
        try {
            parserService.deleteAllInvoicesRelatedEntries(request);
            return ResponseEntity.ok("Old records deleted (if any existed)");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting records: " + e.getMessage());
        }
    }

    /**
     * Дозбирає позиції, які AI Builder пропустив у items[], з OCR (readResults).
     * Викликається флоу після For each і перед /reset-status.
     */
    @PostMapping("/fill-missing")
    public ResponseEntity<?> fillMissing(@RequestBody FillMissingRequestDto request) {
        try {
            return ResponseEntity.ok(recoveryService.fillMissing(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error filling missing lines: " + e.getMessage());
        }
    }
}
