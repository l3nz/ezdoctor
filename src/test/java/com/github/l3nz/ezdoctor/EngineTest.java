package com.github.l3nz.ezdoctor;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class EngineTest {

    // --- resolveOutput ---

    @Test
    public void resolveOutput_defaultName() {
        File out = Engine.resolveOutput(new File("/docs/guide.adoc"), null, null, ".pdf");
        assertEquals("guide.pdf", out.getName());
    }

    @Test
    public void resolveOutput_withRevnumber() {
        File out = Engine.resolveOutput(new File("/docs/guide.adoc"), null, "2.3", ".pdf");
        assertEquals("guide-2.3.pdf", out.getName());
    }

    @Test
    public void resolveOutput_explicitOutputIgnoresRev() {
        File explicit = new File("/out/manual.pdf");
        File out = Engine.resolveOutput(new File("/docs/guide.adoc"), explicit, "2.3", ".pdf");
        assertEquals(explicit, out);
    }

    @Test
    public void resolveOutput_stripsAdocExtension() {
        File out = Engine.resolveOutput(new File("report.adoc"), null, null, ".pdf");
        assertEquals("report.pdf", out.getName());
    }

    @Test
    public void resolveOutput_htmlExtension() {
        File out = Engine.resolveOutput(new File("report.adoc"), null, null, ".html");
        assertEquals("report.html", out.getName());
    }

    @Test
    public void resolveOutput_epubWithRev() {
        File out = Engine.resolveOutput(new File("manual.adoc"), null, "1.0", ".epub");
        assertEquals("manual-1.0.epub", out.getName());
    }
}
