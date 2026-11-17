package com.fuchs.invoicesParser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OcrRowAssemblerTest {

    static List<OcrLineDto> loadFixture() throws Exception {
        try (InputStream in = OcrRowAssemblerTest.class.getResourceAsStream("/readResults_0103298349_p9_p13.json")) {
            return new ObjectMapper().readValue(in, new TypeReference<>() {});
        }
    }

    @Test
    void rebuildsRowsInTheSameFormatAsAiBuilderRowText() throws Exception {
        List<OcrRowAssembler.AssembledRow> rows = OcrRowAssembler.assemble(loadFixture());

        assertThat(rows).extracting(OcrRowAssembler.AssembledRow::lineNumber)
                .containsExactly(320, 330, 430, 440, 450, 460);

        List<OcrRowAssembler.AssembledRow> priced = rows.stream().filter(OcrRowAssembler.AssembledRow::isPriced).toList();
        assertThat(priced).extracting(OcrRowAssembler.AssembledRow::rowText).containsExactly(
                // ці три рядки AI Builder повернув — текст має збігатися символ у символ
                "320 602008114 30 K03 380.7 KG 568.85 / 100 KG 2,165.61",
                "330 602008114 30 K03 380.7 KG 568.85 / 100 KG 2,165.61",
                "450 602070593 1 EA 51.3 KG 477.46 / 100 KG 244.94",
                // а цей AI Builder пропустив
                "460 602183736 36 K12 375.84 KG 434.85 / 100 KG 1,634.34"
        );
    }

    @Test
    void extractsPriceAndAmountLikeTheFlowDoes() throws Exception {
        OcrRowAssembler.AssembledRow row460 = OcrRowAssembler.assemble(loadFixture()).stream()
                .filter(r -> r.lineNumber() == 460).findFirst().orElseThrow();

        assertThat(row460.articul()).isEqualTo("602183736");
        assertThat(row460.textPrice()).isEqualTo("434.85 / 100 KG");
        assertThat(row460.amount()).isEqualTo("1,634.34");
    }

    @Test
    void positionsWithoutPriceAreNotPriced() throws Exception {
        List<OcrRowAssembler.AssembledRow> rows = OcrRowAssembler.assemble(loadFixture());

        assertThat(rows).filteredOn(r -> r.lineNumber() == 430 || r.lineNumber() == 440)
                .allSatisfy(r -> {
                    assertThat(r.isPriced()).isFalse();
                    assertThat(r.rowText()).doesNotContain("/");
                });
    }

    @Test
    void emptyOrNullInputGivesNoRows() {
        assertThat(OcrRowAssembler.assemble(null)).isEmpty();
        assertThat(OcrRowAssembler.assemble(List.of())).isEmpty();
    }
}
