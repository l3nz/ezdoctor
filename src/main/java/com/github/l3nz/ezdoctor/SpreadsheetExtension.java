package com.github.l3nz.ezdoctor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.ast.ContentModel;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.extension.BlockProcessor;
import org.asciidoctor.extension.Contexts;
import org.asciidoctor.extension.Name;
import org.asciidoctor.extension.Reader;
import org.asciidoctor.extension.Treeprocessor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adds [spreadsheet] blocks to AsciiDoc. Attributes saved by their formulas can be
 * referenced anywhere in the document, even before the block that defines them.
 */
public final class SpreadsheetExtension {

    private SpreadsheetExtension() {
    }

    /** Enables spreadsheet blocks. An instance must parse only one document after this call. */
    static void register(Asciidoctor asciidoctor) {
        Collected collected = new Collected();
        asciidoctor.javaExtensionRegistry()
                .block(new SpreadsheetBlock(collected))
                .treeprocessor(new AttributeWriter(collected));
    }

    /** What the spreadsheets of a document have produced so far. */
    static final class Collected {
        final Map<String, String> attributes = new HashMap<>();
        private final Set<String> tableNames = new HashSet<>();

        /** Records a table's name and attributes, rejecting names already used in the document. */
        void add(String tableName, Map<String, String> newAttributes) {
            if (tableName != null && !tableNames.add(tableName)) {
                throw new IllegalArgumentException("duplicate table name: " + tableName);
            }
            for (Map.Entry<String, String> attribute : newAttributes.entrySet()) {
                if (attributes.putIfAbsent(attribute.getKey(), attribute.getValue()) != null) {
                    throw new IllegalArgumentException("attribute saved twice: " + attribute.getKey());
                }
            }
        }
    }

    /** Replaces a spreadsheet block with the table it describes. */
    @Name("spreadsheet")
    @Contexts(Contexts.LISTING)
    @ContentModel(ContentModel.RAW)
    public static final class SpreadsheetBlock extends BlockProcessor {
        private final Collected collected;

        SpreadsheetBlock(Collected collected) {
            this.collected = collected;
        }

        @Override
        public Object process(StructuralNode parent, Reader reader, Map<String, Object> attributes) {
            Object name = attributes.get("name");
            String tableName = (name != null) ? name.toString() : null;
            parseContent(parent, render(reader.readLines(), tableName, attributes.get("title"), parent.getDocument()));
            return null;
        }

        /** The value of an attribute at this point of the parsing: computed by a spreadsheet, or set in the document. */
        private String attributeValue(Document document, String name) {
            String computed = collected.attributes.get(name);
            if (computed != null) {
                return computed;
            }
            Object value = document.getAttribute(name);
            return (value != null) ? value.toString() : null;
        }

        /** The AsciiDoc replacing the block: its table or, if the block is invalid, a warning. */
        private List<String> render(List<String> source, String tableName, Object title, Document document) {
            try {
                Spreadsheet.Result result = Spreadsheet.expand(source, tableName, name -> attributeValue(document, name));
                collected.add(tableName, result.attributes);
                List<String> lines = new ArrayList<>(result.lines);
                if (title != null) {
                    lines.add(0, "." + title);
                }
                return lines;
            } catch (IllegalArgumentException e) {
                return Spreadsheet.errorLines(e.getMessage());
            }
        }
    }

    /** Sets the collected attributes once parsing is over, because Asciidoctor discards any set earlier. */
    public static final class AttributeWriter extends Treeprocessor {
        private final Collected collected;

        AttributeWriter(Collected collected) {
            this.collected = collected;
        }

        @Override
        public Document process(Document document) {
            for (Map.Entry<String, String> attribute : collected.attributes.entrySet()) {
                document.setAttribute(attribute.getKey(), attribute.getValue(), true);
            }
            return document;
        }
    }
}
