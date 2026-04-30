# Tortoise Language Server

A Language Server Protocol implementation for the Turtle Academy LOGO language.

Tortoise Language Server provides editor tooling for LOGO through the Language Server Protocol (LSP), with a setup documented for IntelliJ IDEA via LSP4IJ.

## Table of contents

- [Features](#features)
- [Project status](#project-status)
- [Prerequisites](#prerequisites)
- [Build and run](#build-and-run)
- [Connect to an LSP client](#connect-to-an-lsp-client)
- [Usage](#usage)
- [Supported language subset](#supported-language-subset)
- [Architecture](#architecture)
- [Project layout](#project-layout)
- [Testing](#testing)
- [Limitations and non-goals](#limitations-and-non-goals)
- [Screenshots](#screenshots)
- [Development notes](#development-notes)
- [License](#license)

## Features

- Syntax highlighting via semantic tokens
- Go-to-declaration for procedures and variables
- References
- Diagnostics
- Completion

## Prerequisites

Before building or running the server, install:

- Java: TODO
- A JetBrains IDE (for the documented client setup): IntelliJ IDEA
- LSP4IJ plugin: [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/23257-lsp4ij)

## Build and run

### Build the project

From the project root run:

- Windows:
  ```bat
  .\gradlew.bat build
  ```
- macOS/Linux:
  ```bash
  ./gradlew build
  ```

### Build the server launcher

From the project root run:

- Windows:
  ```bat
  .\gradlew.bat installDist
  ```
- macOS/Linux:
  ```bash
  ./gradlew installDist
  ```

### Run the server manually

In case of applying to other clients.
After `installDist`, run the generated launcher from the project root:

- Windows:
  ```bat
  build\install\tortoise-ls\bin\tortoise-ls.bat
  ```
- macOS/Linux:
  ```bash
  ./build/install/tortoise-ls/bin/tortoise-ls
  ```

## Connect to an LSP client

Any LSP-compatible client can be used, but the setup below documents IntelliJ IDEA with LSP4IJ.

For the general LSP4IJ user-defined language server flow, see the official documentation:
[User-defined language server](https://github.com/redhat-developer/lsp4ij/blob/main/docs/UserDefinedLanguageServer.md)


### Configue Tortoise language server in Intelij

Make sure that you ran `installDis` and have LSP4IJ then,

Open:

`Settings > Languages & Frameworks > Language Servers`

Create a new user-defined language server with the following values.

#### Server

- Name: `tortoise-ls`
- Command:
  - Windows: absolute path to `tortoise-ls\build\install\tortoise-ls\bin\tortoise-ls.bat`
  - macOS/Linux: absolute path to `tortoise-ls/build/install/tortoise-ls/bin/tortoise-ls`

#### Mappings

Add one file name pattern mapping:

- File name pattern: `*.logo`
- Language ID: `logo`

Leave the other fields empty unless explicit server configuration is added later.


## Usage

### Open a LOGO file

Open a file with the `.logo` extension in IntelliJ IDEA after configuring the language server.

### Expected editor functionality

TODO

### Example program

```logo
to square :size
  repeat 4 [
    forward :size
    right 90
  ]
end

square 100
```





## Supported language subset

The server currently targets a practical subset of the Turtle Academy LOGO dialect.

Supported at a high level:

- Procedure declarations and references
- Variable declarations and references
- Basic structural analysis needed for navigation and diagnostics
- Semantic token classification for supported language constructs

Detailed grammar and semantic assumptions:
TODO

## Architecture

The implementation is organized around a small semantic core reused by all editor features.

High-level flow:

1. Source text is synchronized from the client
2. The document is analyzed
3. Semantic information is reused by feature handlers
4. LSP responses are produced from that shared analysis

Detailed architecture explanation:
TODO



## Project layout

### Repository tree

```text
.
├── README.md
├── build.gradle.kts
├── settings.gradle.kts
├── gradle/
├── src/
│   ├── main/
│   │   ├── kotlin/
│   │   │   └── TODO
│   │   └── resources/
│   │       └── TODO
│   └── test/
│       ├── kotlin/
│       │   └── TODO
│       └── resources/
│           └── TODO
└── TODO
```

### Layout lookup


| Path                  | Purpose                                          |
| --------------------- | ------------------------------------------------ |
| `README.md`           | Project overview, setup, and usage documentation |
| `build.gradle.kts`    | Gradle build configuration                       |
| `settings.gradle.kts` | Gradle project settings                          |
| `src/main/kotlin`     | Production source code                           |
| `src/main/resources`  | Runtime resources                                |
| `src/test/kotlin`     | Automated tests                                  |
| `src/test/resources`  | Test fixtures and sample inputs                  |
| `gradle/`             | Gradle wrapper support files                     |
| `TODO`                | TODO                                             |


### Package-level structure


| Package / area | Responsibility |
| -------------- | -------------- |
| `TODO`         | TODO           |
| `TODO`         | TODO           |
| `TODO`         | TODO           |
| `TODO`         | TODO           |


## Testing

### Run tests

From the project root run:

- Windows:
  ```bat
  .\gradlew.bat test
  ```
- macOS/Linux:
  ```bash
  ./gradlew test
  ```

### Test strategy

TODO

## Limitations and non-goals

- Single-file support only
- Focused on a subset of Turtle Academy LOGO
- TODO
- TODO

## Screenshots









## Development notes

TODO

## License

TODO