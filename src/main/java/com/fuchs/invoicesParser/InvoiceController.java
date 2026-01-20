package com.fuchs.invoicesParser;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceParserService parserService;

    public InvoiceController(InvoiceParserService parserService) {
        this.parserService = parserService;
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

    @PostMapping("/cleanup")
    public ResponseEntity<String> cleanupInvoice(@RequestBody InvoiceRequestDto request) {
        try {
            parserService.deleteAllInvoicesRelatedEntries(request);
            return ResponseEntity.ok("Old records deleted (if any existed)");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting records: " + e.getMessage());
        }
    }
}
