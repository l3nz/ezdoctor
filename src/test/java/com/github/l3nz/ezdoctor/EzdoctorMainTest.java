package com.github.l3nz.ezdoctor;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class EzdoctorMainTest {

    // --- PdfCommand.resolveOutput ---

    @Test
    public void resolveOutput_defaultName() {
        File out = PdfCommand.resolveOutput(new File("/docs/guide.adoc"), null, null);
        assertEquals("guide.pdf", out.getName());
    }

    @Test
    public void resolveOutput_withRevnumber() {
        File out = PdfCommand.resolveOutput(new File("/docs/guide.adoc"), null, "2.3");
        assertEquals("guide-2.3.pdf", out.getName());
    }

    @Test
    public void resolveOutput_explicitOutputIgnoresRev() {
        File explicit = new File("/out/manual.pdf");
        File out = PdfCommand.resolveOutput(new File("/docs/guide.adoc"), explicit, "2.3");
        assertEquals(explicit, out);
    }

    @Test
    public void resolveOutput_stripsAdocExtension() {
        File out = PdfCommand.resolveOutput(new File("report.adoc"), null, null);
        assertEquals("report.pdf", out.getName());
    }
}
