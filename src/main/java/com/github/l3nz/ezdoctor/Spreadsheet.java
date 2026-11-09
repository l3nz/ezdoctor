package com.github.l3nz.ezdoctor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns the text of a spreadsheet block into a plain AsciiDoc table, computing its formulas.
 * The first line is the header, each following line a row, with cells separated by "|".
 * Formulas may only appear in the last row, as {sum(N)} or {attribute=sum(N)}.
 */
final class Spreadsheet {

    /** The generated AsciiDoc lines, and the attributes saved by the formulas. */
    static final class Result {
        final List<String> lines;
        final Map<String, String> attributes;

        Result(List<String> lines, Map<String, String> attributes) {
            this.lines = lines;
            this.attributes = attributes;
        }
    }

    /** Attribute name that stands for the name of the table. */
    private static final String SELF = "me";
    private static final String ATTRIBUTE_NAME = "\\w[\\w-]*";
    private static final Pattern CELL_SEPARATOR = Pattern.compile("(?<!\\\\)\\|");
    private static final Pattern FORMULA =
            Pattern.compile("\\{(?:(" + ATTRIBUTE_NAME + ")=)?(\\w+)\\(([^)}]*)\\)\\}");
    private static final Pattern ATTRIBUTE_REFERENCE = Pattern.compile("\\{(" + ATTRIBUTE_NAME + ")\\}");
    private static final Pattern NUMBER =Pattern.compile("[+-]?\\d+([.,]\\d+)?");
    private static final Pattern COLUMN_OFFSET = Pattern.compile("-?\\d+");

    private Spreadsheet() {
    }

    /**
     * Converts the lines of a spreadsheet block.
     *
     * @param source    the lines inside the block
     * @param tableName the name of the table, or null if it has none
     * @param lookup    gives the value of an attribute known so far, or null if there is none
     * @return the equivalent table and the attributes to set on the document
     * @throws IllegalArgumentException if the block is malformed
     */
    static Result expand(List<String> source, String tableName, Function<String, String> lookup) {
        List<List<String>> rows = parseRows(source);
        int last = rows.size() - 1;
        for (int r = 0; r < last; r++) {
            for (String cell : rows.get(r)) {
                if (FORMULA.matcher(cell).find()) {
                    throw new IllegalArgumentException("formulas are only allowed in the last row: " + cell);
                }
            }
        }

        boolean hasFormulas = false;
        for (String cell : rows.get(last)) {
            hasFormulas |= FORMULA.matcher(cell).find();
        }
        if (hasFormulas && last < 2) {
            throw new IllegalArgumentException("no data rows above the formulas");
        }

        String columns = columnSpec(rows, lookup);
        Map<String, String> attributes = new LinkedHashMap<>();
        if (hasFormulas) {
            rows.set(last, evaluateLastRow(rows, tableName, lookup, attributes));
        }

        List<String> lines = new ArrayList<>();
        lines.add("[cols=\"" + columns + "\",options=\"header" + (hasFormulas ? ",footer" : "") + "\"]");
        lines.add("|===");
        // One cell per line: on a shared line, trailing text such as " a" would be read as a cell format.
        for (List<String> row : rows) {
            for (String cell : row) {
                lines.add("|" + cell);
            }
        }
        lines.add("|===");
        return new Result(lines, attributes);
    }

    /** The AsciiDoc shown in place of a spreadsheet block that could not be processed. */
    static List<String> errorLines(String message) {
        return Collections.singletonList("WARNING: Error in spreadsheet: " + message);
    }

    private static List<List<String>> parseRows(List<String> source) {
        List<List<String>> rows = new ArrayList<>();
        for (String line : source) {
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> cells = new ArrayList<>();
            for (String cell : CELL_SEPARATOR.split(line, -1)) {
                cells.add(cell.trim());
            }
            if (!rows.isEmpty() && cells.size() != rows.get(0).size()) {
                throw new IllegalArgumentException("expected " + rows.get(0).size()
                        + " cells but found " + cells.size() + ": " + line);
            }
            rows.add(cells);
        }
        if (rows.size() < 2) {
            throw new IllegalArgumentException("a header and at least one row are required");
        }
        return rows;
    }

    /** Right-aligns the columns that hold only numbers and formulas. */
    private static String columnSpec(List<List<String>> rows, Function<String, String> lookup) {
        StringBuilder spec = new StringBuilder();
        for (int c = 0; c < rows.get(0).size(); c++) {
            spec.append(c > 0 ? "," : "").append(isNumericColumn(rows, c, lookup) ? ">1" : "3");
        }
        return spec.toString();
    }

    private static boolean isNumericColumn(List<List<String>> rows, int column, Function<String, String> lookup) {
        boolean found = false;
        for (List<String> row : rows.subList(1, rows.size())) {
            String cell = resolveAttributes(row.get(column), lookup);
            if (cell.isEmpty()) {
                continue;
            }
            if (!NUMBER.matcher(cell).matches() && !FORMULA.matcher(cell).find()) {
                return false;
            }
            found = true;
        }
        return found;
    }

    /** Replaces the references to known attributes with their values, leaving the others in place. */
    private static String resolveAttributes(String cell, Function<String, String> lookup) {
        Matcher matcher = ATTRIBUTE_REFERENCE.matcher(cell);
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String value = lookup.apply(matcher.group(1).toLowerCase(Locale.ROOT));
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(value != null ? value : matcher.group()));
        }
        matcher.appendTail(resolved);
        return resolved.toString().trim();
    }

    private static List<String> evaluateLastRow(List<List<String>> rows, String tableName,
                                                Function<String, String> lookup,
                                                Map<String, String> attributes) {
        List<List<String>> dataRows = rows.subList(1, rows.size() - 1);
        List<String> evaluated = new ArrayList<>();
        List<String> lastRow = rows.get(rows.size() - 1);
        for (int c = 0; c < lastRow.size(); c++) {
            Matcher matcher = FORMULA.matcher(lastRow.get(c));
            StringBuilder cell = new StringBuilder();
            while (matcher.find()) {
                String value = evaluate(matcher, dataRows, c, lookup);
                if (matcher.group(1) != null) {
                    String name = attributeName(matcher.group(1), tableName);
                    if (attributes.put(name, value) != null) {
                        throw new IllegalArgumentException("attribute saved twice: " + name);
                    }
                }
                matcher.appendReplacement(cell, Matcher.quoteReplacement(value));
            }
            matcher.appendTail(cell);
            evaluated.add(cell.toString());
        }
        return evaluated;
    }

    private static String evaluate(Matcher formula, List<List<String>> dataRows, int column,
                                   Function<String, String> lookup) {
        String function = formula.group(2);
        String argument = formula.group(3).trim();
        if (!"sum".equals(function)) {
            throw new IllegalArgumentException("unknown function: " + function);
        }
        if (!COLUMN_OFFSET.matcher(argument).matches()) {
            throw new IllegalArgumentException("invalid argument for sum: " + argument);
        }
        int target = column + Integer.parseInt(argument);
        if (target < 0 || target >= dataRows.get(0).size()) {
            throw new IllegalArgumentException("column outside the table: " + formula.group());
        }
        return sumColumn(dataRows, target, lookup);
    }

    /** Adds up a column, keeping the decimal separator used by its cells. Empty cells count as zero. */
    private static String sumColumn(List<List<String>> dataRows, int column, Function<String, String> lookup) {
        BigDecimal total = BigDecimal.ZERO;
        boolean comma = false;
        boolean dot = false;
        for (List<String> row : dataRows) {
            String cell = resolveAttributes(row.get(column), lookup);
            if (cell.isEmpty()) {
                continue;
            }
            Matcher undefined = ATTRIBUTE_REFERENCE.matcher(cell);
            if (undefined.find()) {
                throw new IllegalArgumentException("attribute not defined yet: " + undefined.group(1));
            }
            if (!NUMBER.matcher(cell).matches()) {
                throw new IllegalArgumentException("not a number: " + cell);
            }
            comma |= cell.contains(",");
            dot |= cell.contains(".");
            total = total.add(new BigDecimal(cell.replace(',', '.')));
        }
        if (comma && dot) {
            throw new IllegalArgumentException("mixed decimal separators in column " + (column + 1));
        }
        String result = total.toPlainString();
        return comma ? result.replace('.', ',') : result;
    }

    /** Asciidoctor resolves attribute references in lower case, so names are stored that way. */
    private static String attributeName(String name, String tableName) {
        if (!SELF.equals(name)) {
            return name.toLowerCase(Locale.ROOT);
        }
        if (tableName == null || !tableName.matches(ATTRIBUTE_NAME)) {
            throw new IllegalArgumentException("\"" + SELF + "\" needs a valid table name, found: " + tableName);
        }
        return tableName.toLowerCase(Locale.ROOT);
    }
}
