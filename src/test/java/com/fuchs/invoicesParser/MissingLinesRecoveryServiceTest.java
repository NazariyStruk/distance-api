package com.fuchs.invoicesParser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class MissingLinesRecoveryServiceTest {

    private static final String NUMBER = "103298349";
    private static final String VAT = "DE813331100";
    private static final LocalDate DATE = LocalDate.of(2026, 11, 16);

    private InvoiceItemRepository repository;
    private InvoiceParserService parserService;
    private MissingLinesRecoveryService service;

    @BeforeEach
    void setUp() {
        repository = mock(InvoiceItemRepository.class);
        parserService = mock(InvoiceParserService.class);
        service = new MissingLinesRecoveryService(repository, parserService);
    }

    private FillMissingRequestDto request(String vat) throws Exception {
        FillMissingRequestDto r = new FillMissingRequestDto();
        r.setNumber(NUMBER);
        r.setVendorTaxId(vat);
        r.setDate(DATE);
        r.setReadResults(OcrRowAssemblerTest.loadFixture());
        return r;
    }

    @Test
    void savesOnlyTheLineThatAiBuilderMissed() throws Exception {
        // основний For each уже зберіг 320, 330, 450 — бракує тільки 460
        when(repository.findLineNumbers(NUMBER, VAT, DATE)).thenReturn(List.of(320, 330, 450));

        FillMissingResponseDto response = service.fillMissing(request(VAT));

        assertThat(response.skipped()).isFalse();
        assertThat(response.foundInOcr()).isEqualTo(4);
        assertThat(response.alreadySaved()).isEqualTo(3);
        assertThat(response.added()).containsExactly(460);
        assertThat(response.failed()).isEmpty();

        ArgumentCaptor<InvoiceRequestDto> captor = ArgumentCaptor.forClass(InvoiceRequestDto.class);
        verify(parserService, times(1)).parseAndSave(captor.capture());
        InvoiceRequestDto dto = captor.getValue();
        // ті самі поля, що шле крок ParseAndSaveData у флоу
        assertThat(dto.getDescr()).isEqualTo("460 602183736 36 K12 375.84 KG 434.85 / 100 KG 1,634.34");
        assertThat(dto.getTextPrice()).isEqualTo("434.85 / 100 KG");
        assertThat(dto.getAmount()).isEqualTo("1,634.34");
        assertThat(dto.getNumber()).isEqualTo(NUMBER);
        assertThat(dto.getVendorTaxId()).isEqualTo(VAT);
        assertThat(dto.getDate()).isEqualTo(DATE);
        assertThat(dto.getWeightPL()).isNull();
    }

    @Test
    void doesNothingWhenEverythingIsAlreadySaved() throws Exception {
        when(repository.findLineNumbers(NUMBER, VAT, DATE)).thenReturn(List.of(320, 330, 450, 460));

        FillMissingResponseDto response = service.fillMissing(request(VAT));

        assertThat(response.added()).isEmpty();
        assertThat(response.alreadySaved()).isEqualTo(4);
        verify(parserService, never()).parseAndSave(any());
    }

    @Test
    void duplicateArticulOnDifferentLinesIsNotTreatedAsDuplicate() throws Exception {
        // 320 і 330 мають однаковий артикул 602008114 — порівнюємо по номеру рядка, не по артикулу
        when(repository.findLineNumbers(NUMBER, VAT, DATE)).thenReturn(List.of(320, 450, 460));

        FillMissingResponseDto response = service.fillMissing(request(VAT));

        assertThat(response.added()).containsExactly(330);
    }

    @Test
    void skipsNonGermanVendorsSoOldLogicStaysTheOnlyOne() throws Exception {
        FillMissingResponseDto response = service.fillMissing(request("PL1234567890"));

        assertThat(response.skipped()).isTrue();
        verifyNoInteractions(repository, parserService);
    }

    @Test
    void oneFailedLineDoesNotStopTheOthers() throws Exception {
        when(repository.findLineNumbers(NUMBER, VAT, DATE)).thenReturn(List.of(320, 330));
        doThrow(new RuntimeException("boom"))
                .when(parserService).parseAndSave(argThat(d -> d.getDescr().startsWith("450 ")));

        FillMissingResponseDto response = service.fillMissing(request(VAT));

        assertThat(response.failed()).containsExactly(450);
        assertThat(response.added()).containsExactly(460);
    }

    @Test
    void requiresDocumentKeys() {
        FillMissingRequestDto r = new FillMissingRequestDto();
        r.setVendorTaxId(VAT);
        assertThatThrownBy(() -> service.fillMissing(r)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Наскрізна перевірка зі справжнім InvoiceParserService:
     * відновлений рядок 460 зберігається з тими самими полями, як і звичайні рядки.
     */
    @Test
    void recoveredLineIsParsedByExistingLogic() throws Exception {
        InvoiceItemRepository repo = mock(InvoiceItemRepository.class);
        when(repo.findLineNumbers(NUMBER, VAT, DATE)).thenReturn(List.of(320, 330, 450));
        InvoiceParserService realParser = new InvoiceParserService(repo);
        MissingLinesRecoveryService realService = new MissingLinesRecoveryService(repo, realParser);

        realService.fillMissing(request(VAT));

        ArgumentCaptor<InvoiceItem> saved = ArgumentCaptor.forClass(InvoiceItem.class);
        verify(repo, times(1)).save(saved.capture());
        InvoiceItem item = saved.getValue();

        assertThat(item.getLineNumber()).isEqualTo(460);
        assertThat(item.getArticul()).isEqualTo("602183736");
        assertThat(item.getPrice()).isEqualByComparingTo(new BigDecimal("434.85"));
        assertThat(item.getAmount()).isEqualByComparingTo(new BigDecimal("1634.34"));
        assertThat(item.getQuantity()).isEqualByComparingTo(new BigDecimal("36"));
        assertThat(item.getUnits()).isEqualTo("K12");
        assertThat(item.getWeight()).isEqualByComparingTo(new BigDecimal("375.84"));
        assertThat(item.getUnitPrice()).isEqualTo("100 KG");
        assertThat(item.getNumber()).isEqualTo(NUMBER);
        assertThat(item.getVendorTaxId()).isEqualTo(VAT);
        assertThat(item.getDate()).isEqualTo(DATE);
        assertThat(item.isUpdatedBy1c()).isTrue();
    }

    /** Контроль: рядок 450 зі старого шляху (rowText від AI Builder) дає ті самі поля, що й з OCR. */
    @Test
    void ocrRowAndAiRowTextGiveIdenticalItem() throws Exception {
        InvoiceItemRepository repo = mock(InvoiceItemRepository.class);
        InvoiceParserService realParser = new InvoiceParserService(repo);

        // як шле старий флоу (поля з Select)
        InvoiceRequestDto oldWay = new InvoiceRequestDto();
        oldWay.setDescr("450 602070593 1 EA 51.3 KG 477.46 / 100 KG 244.94");
        oldWay.setTextPrice("477.46 / 100 KG");
        oldWay.setAmount("244.94");
        oldWay.setNumber(NUMBER);
        oldWay.setVendorTaxId(VAT);
        oldWay.setDate(DATE);
        oldWay.setWeightPL("");
        realParser.parseAndSave(oldWay);

        // як шле новий крок (з OCR)
        when(repo.findLineNumbers(NUMBER, VAT, DATE)).thenReturn(List.of(320, 330, 460));
        new MissingLinesRecoveryService(repo, realParser).fillMissing(request(VAT));

        ArgumentCaptor<InvoiceItem> saved = ArgumentCaptor.forClass(InvoiceItem.class);
        verify(repo, times(2)).save(saved.capture());
        InvoiceItem a = saved.getAllValues().get(0);
        InvoiceItem b = saved.getAllValues().get(1);
        a.setId(null);
        b.setId(null);
        assertThat(b).isEqualTo(a);
    }
}
