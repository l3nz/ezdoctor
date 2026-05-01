package com.github.l3nz.ezdoctor;

import org.asciidoctor.Asciidoctor;
import org.junit.Test;

import java.io.File;
import java.net.URL;

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

    @Test
    public void resolveOutput_defaultRevFallback() {
        // --rev with no :revnumber: in document defaults to "1"
        File out = Engine.resolveOutput(new File("manual.adoc"), null, "1", ".pdf");
        assertEquals("manual-1.pdf", out.getName());
    }

    // --- readRevnumber (integration, requires AsciidoctorJ) ---

    @Test
    public void readRevnumber_implicitRevisionLine() {
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            File doc = resourceFile("with-revision.adoc");
            assertEquals("1.2", Engine.readRevnumber(asciidoctor, doc));
        }
    }

    @Test
    public void readRevnumber_missingRevision() {
        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            File doc = resourceFile("without-revision.adoc");
            assertNull(Engine.readRevnumber(asciidoctor, doc));
        }
    }

    private static File resourceFile(String name) {
        URL url = EngineTest.class.getClassLoader().getResource(name);
        assertNotNull("Test resource not found: " + name, url);
        return new File(url.getFile());
    }
}
