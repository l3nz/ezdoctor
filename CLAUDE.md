# ezdoctor — Claude.md

## Project Goals

**ezdoctor** is a single, self-contained uberjar that converts AsciiDoc documents to PDF from the command line.

### Key Requirements

- **Single uberjar distribution**: All dependencies bundled into one executable JAR
- **AsciiDoc → PDF conversion**: CLI tool to convert `.adoc` files to PDF output
- **Diagram support**: Includes diagram libraries (PlantUML, etc.) for rendering diagrams in AsciiDoc
- **PDF backend**: Uses AsciidoctorJ with PDF backend for output generation
- **No JDK required**: Standalone package with embedded minimal JRE (via jlink)
- **Java 11 source compatibility**: All Java source must compile and run on Java 11. Do not use language features introduced after Java 11 (no records, no sealed classes, no text blocks, no pattern matching, etc.). Also avoid `var` — use explicit types.

## Developement

### Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.
- Ask before adding or updating dependencies.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

### Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

### Make code testable and understandable

When creating new code or refactoring existing code:
- Try and create pure functions that are tested easily, and then wrap them in impure functions that simply apply what the pure function decided
- Do not depend on local filesystem and local time
- Write those tests
- Always run tests after a change
- Document packages and functions. The documentation should explain what they do, not how.


### Java Namespace

All Java code should use the namespace **`com.github.l3nz.ezdoctor`** (derived from GitHub repo: https://github.com/l3nz/ezdoctor)

### Compiling / running

Uses Gradle 9 with Kotlin build.gradle.kts


## Development Tasks

- [ ] Create integration tests for AsciiDoc → PDF conversion
- [ ] Test diagram rendering (PlantUML) in generated PDFs
- [ ] Test error handling (missing files, invalid AsciiDoc, write permissions)
- [ ] Create sample `.adoc` documents with various features (headings, code blocks, images, diagrams)
- [ ] Add command-line argument validation and help text
- [ ] Build fat JAR (`./gradlew fatJar`)
- [ ] Build standalone bundle with embedded JRE (`./gradlew buildStandalone`)
- [ ] Verify standalone bundle works on target platforms
- [ ] Document CLI usage and examples

## Architecture

- **src/**: Java source (`com.github.l3nz.ezdoctor`)
- **examples/**: Sample `.adoc` documents; run `./run-examples.sh` to convert all to PDF
- **build/libs/ezdoctor-all.jar**: Fat JAR with all dependencies bundled
- **build/standalone/ezdoctor/**: Standalone bundle with embedded JRE + launcher scripts (Unix/Windows)

