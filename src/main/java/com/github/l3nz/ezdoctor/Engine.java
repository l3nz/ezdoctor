package com.github.l3nz.ezdoctor;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.RevisionInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Wraps AsciidoctorJ conversion. One Asciidoctor instance is created per convert() call.
 */
public class Engine {

    /**
     * Resolved theme: name and directory containing the theme file.
     * Both fields are null when no theme is active.
     * When tempDir is true, dir was created by us and is deleted after conversion.
     */
    static final class ThemeConfig {
        final String name;
        final File dir;
        final boolean tempDir;

        private ThemeConfig(String name, File dir, boolean tempDir) {
            this.name = name;
            this.dir = dir;
            this.tempDir = tempDir;
        }

        boolean hasTheme() { return name != null; }

        static ThemeConfig none() { return new ThemeConfig(null, null, false); }
    }

    /**
     * Converts an AsciiDoc file to the given backend format.
     *
     * @param input          source .adoc file
     * @param explicitOutput explicit output path, or null to derive from input
     * @param backend        AsciidoctorJ backend name (e.g. "pdf", "html5", "epub3")
     * @param outputExt      output file extension including dot (e.g. ".pdf")
     * @param styleValue     theme name or path; null if not set. Ignored for epub3.
     * @param embedRev       if true, reads :revnumber: and appends it to the output filename
     * @return the output file that was written
     */
    public static File convert(File input, File explicitOutput,
                               String backend, String outputExt,
                               String styleValue,
                               boolean embedRev) {
        ThemeConfig theme;
        if ("pdf".equals(backend)) {
            theme = resolveTheme(styleValue, ".yml");
        } else if ("html5".equals(backend)) {
            theme = resolveTheme(styleValue, ".css");
        } else {
            theme = ThemeConfig.none();
        }

        try (Asciidoctor asciidoctor = Asciidoctor.Factory.create()) {
            asciidoctor.requireLibrary("asciidoctor-diagram");

            String revnumber = null;
            if (embedRev) {
                String rev = readRevnumber(asciidoctor, input);
                revnumber = (rev != null) ? rev : "1";
            }

            File outputFile = resolveOutput(input, explicitOutput, revnumber, outputExt);

            Attributes attributes;
            if ("pdf".equals(backend)) {
                attributes = buildPdfAttributes(theme);
            } else if ("html5".equals(backend)) {
                attributes = buildHtmlAttributes(theme);
            } else {
                attributes = buildBaseAttributes();
            }

            // External theme dirs are outside the document's directory, which SafeMode.SAFE blocks.
            SafeMode safeMode = theme.hasTheme() && theme.dir != null ? SafeMode.UNSAFE : SafeMode.SAFE;

            Options options = Options.builder()
                    .safe(safeMode)
                    .backend(backend)
                    .mkDirs(true)
                    .attributes(attributes)
                    .toFile(outputFile)
                    .build();

            asciidoctor.convertFile(input, options);
            return outputFile;
        } finally {
            if (theme.tempDir && theme.dir != null) {
                deleteDir(theme.dir);
            }
        }
    }

    /**
     * Resolves a theme by name or file path.
     * 1. If styleValue is a path to an existing file: copies it to a temp dir.
     * 2. If ~/.ezdoctor/<styleValue>-theme.<ext> exists: uses that directory.
     * 3. Otherwise: passes the name through so AsciidoctorJ looks in the document's directory.
     */
    static ThemeConfig resolveTheme(String styleValue, String ext) {
        if (styleValue == null) {
            return ThemeConfig.none();
        }

        File asFile = new File(styleValue);
        if (asFile.isFile()) {
            String name = resolveThemeName(styleValue);
            try {
                File tempDir = Files.createTempDirectory("ezdoctor-theme").toFile();
                Files.copy(asFile.toPath(), new File(tempDir, name + "-theme" + ext).toPath());
                return new ThemeConfig(name, tempDir, true);
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy theme file: " + e.getMessage(), e);
            }
        }

        File userTheme = new File(System.getProperty("user.home"), ".ezdoctor/" + styleValue + "-theme" + ext);
        if (userTheme.isFile()) {
            return new ThemeConfig(styleValue, userTheme.getParentFile(), false);
        }

        return new ThemeConfig(styleValue, null, false);
    }

    /**
     * Derives the theme name from a file path or plain name.
     * "/path/to/mycompany-theme.yml" -> "mycompany"
     * "/path/to/mycompany.css"       -> "mycompany"
     * "mycompany"                    -> "mycompany"
     */
    static String resolveThemeName(String value) {
        String name = new File(value).getName();
        if (name.endsWith(".yml") || name.endsWith(".css")) {
            name = name.substring(0, name.lastIndexOf('.'));
        }
        if (name.endsWith("-theme")) {
            name = name.substring(0, name.length() - 6);
        }
        return name;
    }

    static String readRevnumber(Asciidoctor asciidoctor, File input) {
        Document doc = asciidoctor.loadFile(input, Options.builder().safe(SafeMode.SAFE).build());
        RevisionInfo rev = doc.getRevisionInfo();
        String number = rev.getNumber();
        return (number != null && !number.isEmpty()) ? number : null;
    }

    static File resolveOutput(File input, File explicitOutput, String revnumber, String outputExt) {
        if (explicitOutput != null) {
            return explicitOutput;
        }
        String base = input.getName().replaceFirst("\\.adoc$", "");
        String name = (revnumber != null) ? base + "-" + revnumber + outputExt : base + outputExt;
        File parent = input.getParentFile();
        return new File(parent != null ? parent : new File("."), name);
    }

    private static Attributes buildPdfAttributes(ThemeConfig theme) {
        if (theme.hasTheme() && theme.dir != null) {
            return Attributes.builder()
                    .attribute("source-highlighter", "rouge")
                    .attribute("pdf-theme", theme.name)
                    .attribute("pdf-themesdir", theme.dir.getAbsolutePath())
                    .build();
        }
        if (theme.hasTheme()) {
            return Attributes.builder()
                    .attribute("source-highlighter", "rouge")
                    .attribute("pdf-theme", theme.name)
                    .build();
        }
        return buildBaseAttributes();
    }

    private static Attributes buildHtmlAttributes(ThemeConfig theme) {
        if (theme.hasTheme() && theme.dir != null) {
            return Attributes.builder()
                    .attribute("source-highlighter", "rouge")
                    .attribute("stylesheet", theme.name + "-theme.css")
                    .attribute("stylesdir", theme.dir.getAbsolutePath())
                    .build();
        }
        if (theme.hasTheme()) {
            return Attributes.builder()
                    .attribute("source-highlighter", "rouge")
                    .attribute("stylesheet", theme.name + "-theme.css")
                    .build();
        }
        return buildBaseAttributes();
    }

    private static Attributes buildBaseAttributes() {
        return Attributes.builder()
                .attribute("source-highlighter", "rouge")
                .build();
    }

    private static void deleteDir(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                f.delete();
            }
        }
        dir.delete();
    }
}
