package com.github.l3nz.ezdoctor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class SpreadsheetTest {

    // --- expand (pure) ---

    @Test
    public void expand_rendersTableWithTotalInFooter() {
        Spreadsheet.Result result = expand(Arrays.asList(
                "Item | Hrs",
                "AAAA | 12",
                "BB CC DD | 6",
                "Total: | {sum(0)}"), null);
        assertEquals(Arrays.asList(
                "[cols=\"3,>1\",options=\"header,footer\"]",
                "|===",
                "|Item", "|Hrs",
                "|AAAA", "|12",
                "|BB CC DD", "|6",
                "|Total:", "|18",
                "|==="), result.lines);
        assertTrue(result.attributes.isEmpty());
    }

    @Test
    public void expand_savesNamedAttribute() {
        assertEquals("18", expandTotals("{ore-totali=sum(0)}", null).attributes.get("ore-totali"));
    }

    @Test
    public void expand_meSavesUnderTableName() {
        assertEquals(Collections.singletonMap("ore", "18"), expandTotals("{me=sum(0)}", "ore").attributes);
    }

    @Test
    public void expand_attributeNamesAreLowerCase() {
        assertEquals("18", expandTotals("{Tot=sum(0)}", null).attributes.get("tot"));
    }

    @Test
    public void expand_columnOffsets() {
        List<String> lines = expand(Arrays.asList(
                "A | B | C",
                "1 | 10 | 100",
                "2 | 20 | 200",
                "{sum(1)} | {sum(-1)} | {sum(-2)}"), null).lines;
        assertTrue(lines.toString(), lines.containsAll(Arrays.asList("|30", "|3")));
    }

    @Test
    public void expand_keepsTextAroundFormula() {
        assertTrue(expandTotals("Total {sum(0)} hrs", null).lines.contains("|Total 18 hrs"));
    }

    @Test
    public void expand_noFormulasMeansNoFooter() {
        List<String> lines = expand(Arrays.asList("A | B", "x | y"), null).lines;
        assertEquals("[cols=\"3,3\",options=\"header\"]", lines.get(0));
    }

    @Test
    public void expand_emptyCellInRowIsZero() {
        assertTrue(expand(Arrays.asList("A | B", "x |", "y | 4", "T | {sum(0)}"), null).lines.contains("|4"));
    }

    @Test
    public void expand_decimalsKeepSeparatorWithoutRoundingErrors() {
        assertTrue(expand(Arrays.asList("A", "0,1", "0,2", "{sum(0)}"), null).lines.contains("|0,3"));
        assertTrue(expand(Arrays.asList("A", "0.1", "0.2", "{sum(0)}"), null).lines.contains("|0.3"));
    }

    @Test
    public void expand_escapedPipeStaysInCell() {
        assertTrue(expand(Arrays.asList("A | B", "x\\|y | 1"), null).lines.contains("|x\\|y"));
    }

    @Test
    public void expand_leavesOrdinaryAttributeReferencesAlone() {
        assertTrue(expand(Arrays.asList("A | B", "{foo} | 1"), null).lines.contains("|{foo}"));
    }

    @Test
    public void expand_sumsKnownAttributes() {
        Spreadsheet.Result result = Spreadsheet.expand(
                Arrays.asList("A | B", "x | {Ore}", "y | {rate}", "z | 1", "T | {sum(0)}"), null,
                name -> Collections.singletonMap("ore", "18").getOrDefault(name, "rate".equals(name) ? "1,5" : null));
        assertTrue(result.lines.toString(), result.lines.contains("|20,5"));
        assertTrue(result.lines.toString(), result.lines.contains("|{Ore}"));
        assertEquals("[cols=\"3,>1\",options=\"header,footer\"]", result.lines.get(0));
    }

    @Test
    public void expand_undefinedAttributeInSummedColumnIsRejected() {
        assertRejected("attribute not defined yet: nope", "A", "{nope}", "{sum(0)}");
    }

    @Test
    public void expand_undefinedAttributeOutsideSumsIsKept() {
        assertTrue(expand(Arrays.asList("A | B", "{nope} | 1", "T | {sum(0)}"), null).lines.contains("|{nope}"));
    }

    @Test
    public void expand_rejectsMalformedBlocks() {
        assertRejected("header and at least one row", "A | B");
        assertRejected("expected 2 cells", "A | B", "x");
        assertRejected("only allowed in the last row", "A", "{sum(0)}", "1");
        assertRejected("no data rows", "A", "{sum(0)}");
        assertRejected("unknown function", "A", "1", "{avg(0)}");
        assertRejected("invalid argument", "A", "1", "{sum(:)}");
        assertRejected("outside the table", "A", "1", "{sum(1)}");
        assertRejected("not a number", "A", "n/a", "{sum(0)}");
        assertRejected("mixed decimal", "A", "1,5", "1.5", "{sum(0)}");
        assertRejected("saved twice", "A", "1", "{x=sum(0)} {x=sum(0)}");
        assertRejected("needs a valid table name", "A", "1", "{me=sum(0)}");
    }

    // --- Collected (pure) ---

    @Test
    public void collected_rejectsDuplicates() {
        SpreadsheetExtension.Collected collected = new SpreadsheetExtension.Collected();
        collected.add("t1", Collections.singletonMap("a", "1"));
        assertThrows(IllegalArgumentException.class, () -> collected.add("t1", Collections.emptyMap()));
        assertThrows(IllegalArgumentException.class, () -> collected.add("t2", Collections.singletonMap("a", "2")));
    }

    // --- extension (integration, requires AsciidoctorJ) ---

    @Test
    public void extension_rendersTableAndAttributesEverywhere() {
        String html = convert(
                "Before: {ore} and {tot}.\n\n"
                + ".My title\n"
                + "[spreadsheet,name=ore]\n----\n"
                + "Item | Hrs\nItem a | 12\nBB | 6\nTotal: | {me=sum(0)} {tot=sum(0)}\n"
                + "----\n\n"
                + "After: {ore}.\n");
        assertTrue(html, html.contains("Before: 18 and 18."));
        assertTrue(html, html.contains("After: 18."));
        assertTrue(html, html.contains("My title"));
        assertTrue(html, html.contains("Item a"));
        assertTrue(html, html.contains("18 18"));
        assertTrue(html, html.contains("<tfoot>"));
    }

    @Test
    public void extension_handlesManySpreadsheets() {
        String html = convert(
                "[spreadsheet,name=one]\n----\nA | B\nx | 1\nT | {me=sum(0)}\n----\n\n"
                + "[spreadsheet,name=two]\n----\nA | B\nx | 2\nT | {me=sum(0)}\n----\n\n"
                + "Values: {one} {two}.\n");
        assertTrue(html, html.contains("Values: 1 2."));
    }

    @Test
    public void extension_sumsDocumentAndComputedAttributes() {
        String html = convert(
                "= Doc\n:travel: 150\n\n"
                + "Budget {budget}.\n\n"
                + "[spreadsheet,name=ore]\n----\nA | B\nx | 18\nT | {me=sum(0)}\n----\n\n"
                + "[spreadsheet,name=budget]\n----\nA | B\nwork | {ore}\ntravel | {travel}\nT | {me=sum(0)}\n----\n");
        assertTrue(html, html.contains("Budget 168."));
    }

    @Test
    public void extension_laterAttributeInSumBecomesWarning() {
        String html = convert(
                "[spreadsheet]\n----\nA | B\nx | {later}\nT | {sum(0)}\n----\n\n"
                + "[spreadsheet,name=later]\n----\nA | B\nx | 1\nT | {me=sum(0)}\n----\n");
        assertTrue(html, html.contains("Error in spreadsheet: attribute not defined yet: later"));
    }

    @Test
    public void extension_laterAttributeOutsideSumsIsPrinted() {
        String html = convert(
                "[spreadsheet]\n----\nA | B\nSee {later} | 1\n----\n\n"
                + "[spreadsheet,name=later]\n----\nA | B\nx | 7\nT | {me=sum(0)}\n----\n");
        assertTrue(html, html.contains("See 7"));
    }

    @Test
    public void extension_invalidBlockBecomesWarningAndConversionContinues() {
        String html = convert(
                "[spreadsheet]\n----\nA\n1\n{avg(0)}\n----\n\n"
                + "Still here.\n");
        assertTrue(html, html.contains("admonitionblock warning"));
        assertTrue(html, html.contains("Error in spreadsheet: unknown function: avg"));
        assertTrue(html, html.contains("Still here."));
    }

    @Test
    public void extension_duplicateTableNameBecomesWarning() {
        String html = convert(
                "[spreadsheet,name=t]\n----\nA\n1\n----\n\n"
                + "[spreadsheet,name=t]\n----\nA\n2\n----\n");
        assertTrue(html, html.contains("Error in spreadsheet: duplicate table name: t"));
    }

    private static String convert(String source) {
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            SpreadsheetExtension.register(asciidoctor);
            return asciidoctor.convert(source,
                    Options.builder().safe(SafeMode.UNSAFE).backend("html5").standalone(false).build());
        }
    }

    private static Spreadsheet.Result expand(List<String> source, String tableName) {
        return Spreadsheet.expand(source, tableName, name -> null);
    }

    private static Spreadsheet.Result expandTotals(String formula, String tableName) {
        return expand(Arrays.asList("Item | Hrs", "A | 12", "B | 6", "Total: | " + formula), tableName);
    }

    private static void assertRejected(String expectedMessage, String... source) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> expand(Arrays.asList(source), null));
        assertTrue(e.getMessage(), e.getMessage().contains(expectedMessage));
    }
}
