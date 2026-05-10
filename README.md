# ezdoctor

Create beautiful documents with diagrams in PDF, HTML, and EPUB formats from the command line — with no Ruby installation required.

AsciiDoc offers significant advantages over Markdown when it comes to creating professional PDFs, primarily because it was designed from the ground up for structured, publication-quality document authoring. While Markdown excels at simple web content, it lacks a standardized specification and has no native support for rich semantic elements that technical documentation demands, such as cross-references, footnotes, admonitions, table of contents generation, and proper figure captions. AsciiDoc provides all of these out of the box, with a consistent, unambiguous syntax that does not fragment into incompatible flavors the way Markdown does. 

Asciidoctor PDF converts documents directly into polished, typographically refined PDFs with full control over themes, page layouts, headers, footers, and numbering, no intermediate HTML step required. On top of that, Asciidoctor's built-in integration with diagram libraries means that adding technical visuals like a finite state machine or a sequence diagram is as simple as writing a labeled code block, rendered automatically and embedded perfectly in the output PDF. 

This makes AsciiDoc the clear choice for technical writers and documentation teams who need a single source format that scales from a simple README all the way to a multi-chapter book or formal specification.

## What is this?

**ezdoctor** is a self-contained command-line tool that wraps [AsciidoctorJ](https://github.com/asciidoctor/asciidoctorj) and bundles everything - HTML, PDF and ebooks, plus the diagram libraries that produce a gazillion visualizations, plus syntax highlightning, plus theming - into a single executable JAR. You get the full power of Asciidoctor — diagrams, syntax highlighting, custom themes — without touching Ruby, Gems, or Bundler.

## Why bother?

The native Asciidoctor toolchain is Ruby-based. Getting it to work reliably across machines and CI environments typically involves:

- Installing a specific Ruby version
- Managing gems with Bundler
- Fighting version conflicts between `asciidoctor-pdf`, `asciidoctor-diagram`, Prawn, and friends
- Repeating this on every new machine or CI runner

ezdoctor sidesteps all of that. It ships as a fat JAR with JRuby and all required gems bundled in. If you have Java 11+, you have everything you need.

## Installing

- Download from release
- Unpack
- Create ~/.ezdoctor
- Copy JAR there


Then create an alias like (for modern Javas)

```bash
alias ezdoc="java --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --enable-native-access=ALL-UNNAMED --sun-misc-unsafe-memory-access=allow -Xmx512m -XX:TieredStopAtLevel=1 -XX:+UseSerialGC -Djruby.compile.mode=OFF -jar ~/.ezdoctor/ezdoctor.jar "
```

And add it to your `profile_rc` or similar.


## Usage

```
ezdoc <command> [options] <input.adoc> [output]
```

### Commands

| Command | Output |
|---------|--------|
| `pdf`   | PDF via the AsciidoctorJ PDF backend |
| `html`  | HTML5 |
| `epub`  | EPUB3 |

### Options

All commands accept:

| Option | Description |
|--------|-------------|
| `<input.adoc>` | Source AsciiDoc file (required) |
| `[output]` | Output file path (optional — defaults to input name with new extension) |
| `--rev` | Embed the document's `:revnumber:` in the output filename (e.g. `guide-1.2.pdf`) |

PDF and HTML also accept:

| Option | Description |
|--------|-------------|
| `--theme <name>` | Apply a theme (see [Themes](#themes) below) |

Top-level flags:

| Flag | Description |
|------|-------------|
| `--help` | Show help |
| `--version` | Show version |

### Examples

```bash
# Basic conversion
ezdoc pdf guide.adoc
ezdoc html guide.adoc
ezdoc epub guide.adoc

# Explicit output path
ezdoc pdf guide.adoc /tmp/guide.pdf

# Embed revision number in filename → guide-1.2.pdf
ezdoc pdf --rev guide.adoc

# Apply a theme
ezdoc pdf --theme mycompany guide.adoc
ezdoc html --theme mycompany guide.adoc
```

## Themes

### PDF themes

PDF themes are YAML files following the [AsciidoctorJ PDF theming guide](https://docs.asciidoctor.org/pdf-converter/latest/theme/). A theme file is named `<name>-theme.yml`.

ezdoctor looks for themes in this order:

1. **Full file path** — if `--theme` is a path to an existing `.yml` file, ezdoctor copies it to a temp directory and uses it directly.
2. **User theme directory** — if `~/.ezdoctor/<name>-theme.yml` exists, it is used.
3. **Document directory** — otherwise the name is passed through to AsciidoctorJ, which looks for `<name>-theme.yml` next to the source file.

Example `~/.ezdoctor/mycompany-theme.yml`:

```yaml
extends: default
base:
  font-color: '#333333'
heading:
  font-color: '#003366'
  font-style: bold
```

Then:

```bash
ezdoc pdf --theme mycompany guide.adoc
```

### HTML themes

HTML themes are CSS files named `<name>-theme.css`. The same lookup order applies:

1. Full path to an existing `.css` file
2. `~/.ezdoctor/<name>-theme.css`
3. Name passed through; AsciidoctorJ looks next to the source file

AsciidoctorJ's built-in HTML stylesheet is based on [Asciidoctor's default stylesheet](https://docs.asciidoctor.org/asciidoctor/latest/html-backend/default-stylesheet/). Your CSS file replaces it entirely.

## Revision numbers

AsciiDoc documents can declare a revision using the standard header syntax:

```asciidoc
= My Document
Author Name
v1.2, 2026-05-01: Release notes
```

The `{revnumber}` attribute (`1.2` here) can be used inside the document and also embedded in the output filename with `--rev`:

```bash
ezdoc pdf --rev guide.adoc        # → guide-1.2.pdf
ezdoc html --rev guide.adoc       # → guide-1.2.html
```

If the document has no revision set, `--rev` falls back to `1`.

## Building from source

Requires Java 11+ and Gradle (wrapper included).

```bash
# Build everything - runs tests
./gradlew clean buildTgz

#-> Generates 
#.  build/dist/ezdoctor-0.1.0.tar.gz
# and
#   build/libs/ezdoctor-0.1.0-all.jar

# Convert all example documents
./run-examples.sh
```

## Links

- [AsciidoctorJ](https://github.com/asciidoctor/asciidoctorj) — Java wrapper around Asciidoctor
- [AsciidoctorJ PDF](https://github.com/asciidoctor/asciidoctorj-pdf) — PDF backend
- [AsciidoctorJ PDF Theming Guide](https://docs.asciidoctor.org/pdf-converter/latest/theme/) — full reference for PDF theme YAML
- [AsciidoctorJ Diagram](https://github.com/asciidoctor/asciidoctorj-diagram) — PlantUML, Ditaa, and more
- [AsciiDoc Syntax Quick Reference](https://docs.asciidoctor.org/asciidoc/latest/syntax-quick-reference/)
