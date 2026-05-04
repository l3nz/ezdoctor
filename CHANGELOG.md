# Changelog

## [Unreleased]

### Added
- Build metadata (version, git commit, git branch, build time) embedded into JAR via `version.properties`
- Version string shown in `--version` output and help text

## [0.1.0] — initial release

### Added
- AsciiDoc → PDF, HTML, and EPUB conversion via `pdf`, `html`, `epub` subcommands
- Theme support for HTML and PDF output (`--theme` flag) — fixes #5
- `--rev` flag to embed a revision marker in output — fixes #1
- Picocli-based command-line interface — fixes #10
- Diagram support (PlantUML, Ditaa) via asciidoctorj-diagram
- Fat JAR build (`./gradlew fatJar`) with all dependencies bundled
- Standalone bundle with embedded JRE via jlink (`./gradlew buildStandalone`)
- Sample theme and example documents
- MIT license
