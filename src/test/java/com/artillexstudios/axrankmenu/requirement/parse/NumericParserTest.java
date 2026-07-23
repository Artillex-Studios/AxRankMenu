package com.artillexstudios.axrankmenu.requirement.parse;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumericParserTest {

    @Test
    void stripsFormattingAndParsesGrouping() {
        assertEquals(new BigDecimal("12500"), NumericParser.parse("&#00FF00<green>12,500</green>", true).orElseThrow());
        assertEquals(new BigDecimal("12500.50"), NumericParser.parse("12.500,50", true).orElseThrow());
    }

    @Test
    void parsesAbbreviatedValues() {
        assertEquals(new BigDecimal("1500.0"), NumericParser.parse("1.5K", true).orElseThrow());
        assertEquals(new BigDecimal("3250000000.00"), NumericParser.parse("3.25B", true).orElseThrow());
    }

    @Test
    void parseFailureIsExplicit() {
        assertTrue(NumericParser.parse("not-a-number", true).isEmpty());
        assertTrue(NumericParser.parse("%unresolved_placeholder%", true).isEmpty());
    }
}
