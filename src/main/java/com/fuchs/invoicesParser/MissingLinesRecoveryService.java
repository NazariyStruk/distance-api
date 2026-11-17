package com.fuchs.invoicesParser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Дозбереження позицій, які AI Builder прочитав в OCR, але не повернув у items[]
 * (через це основний For each у флоу їх не бачив, напр. позиція 460 у 0103298349).
 *
 * Не змінює стару логіку:
 *  - викликається окремим кроком флоу ПІСЛЯ For each;
 *  - зберігає тільки ті номери рядків, яких ще немає в БД для цього документа;
 *  - кожен пропущений рядок проходить через той самий InvoiceParserService.parseAndSave,
 *    з тими самими полями, що шле крок ParseAndSaveData (descr / amount / textPrice).
 */
@Service
public class MissingLinesRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(MissingLinesRecoveryService.class);

    private final InvoiceItemRepository repository;
    private final InvoiceParserService parserService;

    public MissingLinesRecoveryService(InvoiceItemRepository repository, InvoiceParserService parserService) {
        this.repository = repository;
        this.parserService = parserService;
    }

    public FillMissingResponseDto fillMissing(FillMissingRequestDto request) {
        if (request.getNumber() == null || request.getVendorTaxId() == null || request.getDate() == null) {
            throw new IllegalArgumentException("number, vendorTaxId and date are required");
        }

        // Розбір OCR перевірено тільки на форматі FUCHS Germany (VAT DE...).
        // Для інших постачальників нічого не робимо — працює лише стара логіка.
        if (!request.getVendorTaxId().startsWith("DE")) {
            return FillMissingResponseDto.skippedResponse();
        }

        List<OcrRowAssembler.AssembledRow> pricedRows = OcrRowAssembler.assemble(request.getReadResults()).stream()
                .filter(OcrRowAssembler.AssembledRow::isPriced)
                .toList();

        Set<Integer> savedLineNumbers = new HashSet<>(repository.findLineNumbers(
                request.getNumber(), request.getVendorTaxId(), request.getDate()));

        List<Integer> added = new ArrayList<>();
        List<Integer> failed = new ArrayList<>();
        int alreadySaved = 0;

        for (OcrRowAssembler.AssembledRow row : pricedRows) {
            if (savedLineNumbers.contains(row.lineNumber())) {
                alreadySaved++;
                continue;
            }

            InvoiceRequestDto dto = new InvoiceRequestDto();
            dto.setDescr(row.rowText());
            dto.setNumber(request.getNumber());
            dto.setVendorTaxId(request.getVendorTaxId());
            dto.setDate(request.getDate());
            dto.setAmount(row.amount());
            dto.setTextPrice(row.textPrice());
            dto.setWeightPL(null); // для німецьких інвойсів вага береться з самого рядка

            try {
                parserService.parseAndSave(dto);
                savedLineNumbers.add(row.lineNumber());
                added.add(row.lineNumber());
                log.warn("Invoice {} ({}): line {} was missing in AI Builder items, recovered from OCR: '{}'",
                        request.getNumber(), request.getVendorTaxId(), row.lineNumber(), row.rowText());
            } catch (Exception e) {
                failed.add(row.lineNumber());
                log.error("Invoice {} ({}): failed to save recovered line {} '{}'",
                        request.getNumber(), request.getVendorTaxId(), row.lineNumber(), row.rowText(), e);
            }
        }

        return new FillMissingResponseDto(false, pricedRows.size(), alreadySaved, added, failed);
    }
}
