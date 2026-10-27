package com.group2.oes;

import com.group2.oes.util.CsvLine;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvLineTest {

    @Test
    void táchTrườngThườngVàTrườngRỗng() {
        assertEquals(List.of("B24", "An", "", "L01"), CsvLine.split("B24, An ,,L01"));
    }

    @Test
    void trườngTrongNgoặcKépGiữĐượcDấuPhẩyVàDấuNháy() {
        assertEquals(List.of("B24", "Trần, Thị \"Bé\" Bình", "x"), CsvLine.split("B24,\"Trần, Thị \"\"Bé\"\" Bình\",x"));
    }
}
