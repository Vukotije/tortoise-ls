# Implementation Plan

## Product decisions
- Kotlin, not Java
- Single Gradle module for the whole project
- stdio transport only
- IntelliJ + LSP4IJ as the main client path
- Single-file Turtle Academy LOGO only
- No cross-file resolution
- No rename, no formatting

## Shipped features
1. Semantic tokens
2. Go-to-declaration
3. References
4. Diagnostics
5. Completion

## Why this set
These features all reuse the same semantic core:
- lexer
- parser
- AST
- symbol table
- resolver

That keeps the project coherent and interview-defensible.

## Analysis model
- Document opens -> full text stored with version
- Document changes -> full replacement applied through `TextSyncStrategy`
- Analysis reruns eagerly on open/change
- Latest result is cached per document version
- Diagnostics are republished after analysis
- Feature requests read the cached analysis

## Recommended package layout
- `server.bootstrap`
- `server.protocol`
- `application.documents`
- `application.analysis`
- `application.features`
- `language.lexer`
- `language.parser`
- `language.ast`
- `language.symbols`
- `language.resolve`
- `language.diagnostics`
- `language.completion`
- `shared.text`
- `shared.model`
- `testutil`

## Delivery order
1. Bootstrap + initialize
2. Document store + sync
3. Lexer
4. Parser
5. AST + ranges
6. Symbols + scope resolution
7. Diagnostics
8. Semantic tokens
9. Go-to-declaration
10. References
11. Completion
12. Tests and polishing

## Coding style
- Small classes with clear ownership
- No god service
- No speculative frameworking
- Clear names over clever code
- Comments only for semantic decisions or protocol subtleties