# Turtle Academy LOGO Semantics

## Dialect source
Target the Turtle Academy playground dialect of LOGO.

## Core supported constructs
### Procedures
Support these declaration forms:
- `to PROCNAME ... end`
- `define PROCNAME ... end` if practical

Primary path: `to ... end`

### Variables
Recognize and support:
- `make "var expr`
- `name expr "var`
- `localmake "var expr`
- `:var`
- `thing "var` optionally

Primary semantic handling:
- declarations via `make`, `name`, `localmake`
- reads via `:var`

### Control and grouping
Recognize at least:
- `repeat X [ ... ]`
- `for [i start limit step] [ ... ]`
- `if expr [ ... ]`
- `ifelse expr [ ... ] [ ... ]`
- `dotimes [i times] [ ... ]`
- `while [expr] [ ... ]`
- `until [expr] [ ... ]`
- bracketed lists `[ ... ]`

## Built-in command catalog baseline
Use a static catalog for highlighting and completion.

### Turtle motion
`forward`, `fd`, `back`, `bk`, `left`, `lt`, `right`, `rt`, `home`, `setx`, `sety`, `setxy`, `set`, `pos`, `setheading`, `seth`, `arc`, `ellipse`

### Motion queries
`pos`, `xcor`, `ycor`, `heading`, `towards`

### Turtle and window control
`showturtle`, `st`, `hideturtle`, `ht`, `clean`, `cs`, `clearscreen`, `fill`, `filled`, `label`, `setlabelheight`, `wrap`, `window`, `fence`

### Turtle/window queries
`shownp`, `shown?`, `labelsize`

### Pen and background control
`penup`, `pu`, `pendown`, `pd`, `setcolor`, `setpencolor`, `setwidth`, `setpensize`, `changeshape`, `csh`

### Pen queries
`pendownp`, `pendown?`, `pencolor`, `pc`, `pensize`

### Lists
`list`, `first`, `butfirst`, `last`, `butlast`, `item`, `pick`

### Math
`sum`, `minus`, `random`, `modulo`, `power`

### Receivers / input
`readword`, `readlist`

### Predicates
`word`, `word?`, `listp`, `list?`, `arrayp`, `array?`, `numberp`, `number?`, `emptyp`, `empty?`, `equalp`, `equal?`, `notequalp`, `notequal?`, `beforep`, `before?`, `substringp`, `substring?`

## Scope assumptions
Because the assignment is single-file:
1. Procedure declarations are file-level symbols.
2. Parameters are visible only inside their owning procedure.
3. `localmake` defines a local variable.
4. `make` follows one explicit documented rule and must be applied consistently.
5. `:name` resolves to the nearest visible variable or parameter.
6. Built-ins are always available and are not user declarations.
7. References never leave the current file.

## Feature-specific behavior
### Semantic tokens
Highlight at least:
- declaration keywords (`to`, `end`, etc.)
- built-in commands
- user-defined procedures
- parameters
- variables
- numbers

### Go-to-declaration
Support:
- procedure call -> procedure declaration
- variable read -> variable or parameter declaration

### References
Support semantic references only, not plain text matches.

### Completion
Completion sources:
1. built-in command catalog
2. user-defined procedures in the file
3. visible parameters
4. visible variables

### Diagnostics
Emit diagnostics for:
- unknown procedure
- unknown variable
- duplicate procedure declaration
- malformed declaration blocks
- other unambiguous structural problems

## Caution
Comment syntax was not clearly confirmed from the fetched Turtle Academy material.
Do not make comment support a contractual core feature unless separately verified.