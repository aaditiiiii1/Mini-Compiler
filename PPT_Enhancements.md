# PPT Enhancements — Mini-Compiler Project
## Slide-by-Slide Recommendations (Updated for Full-Stack + 7 Phases)

---

### Slide 1 — Title Slide

**Current:** Plain text title.

**Recommended changes:**
- Title: **"Mini-Compiler — Full-Stack 7-Phase Arithmetic Compiler"**
- Subtitle: *Lexer · Parser · AST · Semantic · TAC · Constant Folding · Evaluator*
- Add a tech-stack badge row: `Java 17` · `React 18` · `TypeScript` · `Vite` · `REST API`
- Add a GitHub/demo URL if available.

---

### Slide 2 — Project Overview

**Current:** Text-only description.

**Recommended changes:**
- Replace dense text with a three-column layout:

  | Compiler Core | Backend API | Frontend UI |
  |---|---|---|
  | 7 compiler phases | Java HTTP server | React + TypeScript |
  | Floats, variables | 4 REST endpoints | Dark GitHub theme |
  | Error recovery | JSON responses | Live 7-tab output |

- Add a 1-sentence mission: *"Translate arithmetic expressions through 7 compiler phases and visualise every stage interactively."*

---

### Slide 3 — System Architecture *(NEW)*

**Add this slide with a three-tier diagram:**

```
  Browser (React/Vite)
       |  POST /compile { expression }
       |  GET  /vars
       |  POST /vars/clear
       v
  Java HTTP Server (port 8080)
       |  MiniCompiler.compile(expr, symbolTable)
       v
  MiniCompiler.java — 7-phase pipeline
       |  returns CompileResult (JSON)
       v
  Browser renders: tokens, AST, TAC, result
```

- No external framework needed — `com.sun.net.httpserver` is JDK built-in.
- Hosting path: Frontend → Netlify/Vercel; Backend → Railway/Render.

---

### Slide 4 — 7-Phase Compiler Pipeline *(WAS: 5 phases)*

**Recommended changes:**
- Update from 5 to 7 phases with a horizontal numbered pipeline graphic:

```
[1] Lexer → [2] Parser → [3] AST Build → [4] Semantic → [5] TAC Gen → [6] Constant Fold → [7] Eval
```

- Color-code each bubble: Lexer+Parser (blue), AST (green), Semantic (orange), TAC+Fold (purple), Eval (red/green).
- Add a constant folding comparison row:

  | Before Folding | After Folding |
  |---|---|
  | `t1=2*3, t2=4*5, t3=t1+t2` (3 instructions) | `result=26` (0 instructions — fully folded) |

---

### Slide 5 — Phase 1: Lexical Analysis

**Recommended changes:**
- Add color-coded token stream for `x = 3 * (pi + 2)`:
  ```
  [IDENTIFIER:x] [ASSIGN:=] [NUMBER:3] [MUL:*]
  [LPAREN:(] [IDENTIFIER:pi] [PLUS:+] [NUMBER:2] [RPAREN:)]
  ```
- Token chip colors: NUMBER=blue, OPERATOR=red, PAREN=purple, IDENTIFIER=green, ASSIGN=orange.
- Mention: `%` modulo operator is now supported.
- Error recovery: invalid char `@` → lexer warning, compilation continues.

---

### Slide 6 — Phase 2+3: Parsing & AST

**Current:** Missing AST visualisation.

**Recommended changes:**
- Add an ASCII-art AST for `3 + 5 * 2`:
  ```
  L-- OP(+)
      +-- NUM(3)
      L-- OP(*)
          +-- NUM(5)
          L-- NUM(2)
  ```
- Simplified LL grammar rules box:
  ```
  expr   → assign | addSub
  addSub → mulDiv (('+' | '-') mulDiv)*
  mulDiv → unary  (('*' | '/' | '%') unary)*
  unary  → '-' unary | primary
  primary→ NUMBER | '(' expr ')' | IDENTIFIER
  ```
- Recovery rules: missing operand inserts `0` or `1`; unclosed `(` auto-closes.

---

### Slide 7 — Phase 4: Semantic Analysis *(NEW SLIDE)*

**Add this slide:**
- Semantic checks performed:
  1. **Undefined variable** — `y + 5` when `y` not assigned → runtime error, evaluation uses 0.
  2. **Division/Modulo by zero** — `10 / 0`, `5 % 0` → runtime error, returns 0.
  3. **Type coercion** — all values normalized to `double`; integers display without `.0`.
  4. **Warning aggregation** — all warnings collected into a list, reported together.
- The `ok` flag: `true` = clean compile, `false` = recovered with warnings.

---

### Slide 8 — Phase 5: Three-Address Code (TAC)

**Recommended changes:**
- Full example for `(2 + 3) * (4 - 1)`:
  ```
  t1 = 2 + 3
  t2 = 4 - 1
  t3 = t1 * t2
  result = t3
  ```
- Variable assignment: `x = 3 * 7` → `t1 = 3 * 7`, `x = t1`.
- One operator per instruction; temp variables named `t1`, `t2`, ...

---

### Slide 9 — Phase 6: Constant Folding *(NEW SLIDE)*

**Add this slide:**

*"Constant folding replaces compile-time constant sub-expressions with their computed values before execution."*

- Before vs. After for `2 * 3 + 4 * 5`:

  | Before Folding | After Folding |
  |---|---|
  | `t1 = 2 * 3` | *(eliminated)* |
  | `t2 = 4 * 5` | *(eliminated)* |
  | `t3 = t1 + t2` | *(eliminated)* |
  | `result = t3` | `result = 26` |

- Mixed example `x + 2 * 3`: constant `2*3` folds to `6`; the `x + 6` part cannot fold (variable).

---

### Slide 10 — Phase 7: Evaluation

**Recommended changes:**
- Show recursive `evalAst()` traversal on the AST tree.
- `EvalResult` structure: `{ value: double, runtimeErrors: List<String> }`.
- Result display: integers show as `42`; floats as `3.14`.
- Status badges: `Clean compile` (green) · `Recovered` (amber) · `Runtime Error` (red).

---

### Slide 11 — Web Interface Demo *(NEW SLIDE)*

**Add this slide:**
- Screenshot/mockup of the React UI:
  - **Left panel:** expression input field, example chips, symbol table, history list.
  - **Right panel:** 7 numbered phase tabs, active tab shows phase output.
- Key UX features to highlight:
  - One-click example chips (including error cases)
  - 7-tab phase inspector — click any tab to see that compiler stage
  - Live symbol table — updates as variables are assigned each compile
  - Clickable history — re-run any past expression
  - Result banner: green (OK) · amber (warning) · red (error)
  - Server status dot: green when backend is reachable

---

### Slide 12 — REST API Endpoints *(NEW SLIDE)*

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/compile` | Compile expression → `CompileResult` JSON |
| `GET` | `/vars` | Return all session variables |
| `POST` | `/vars/clear` | Clear all session variables |
| `GET` | `/health` | Server health check |

**Request:**
```json
POST /compile
{ "expression": "x = 6 * 7" }
```

**Response (abbreviated):**
```json
{
  "input": "x = 6 * 7",
  "result": 42,
  "ok": true,
  "tokens": ["IDENTIFIER:x", "ASSIGN:=", "NUMBER:6", "MUL:*", "NUMBER:7"],
  "tac": ["t1 = 6 * 7", "x = t1"],
  "optimisedTac": ["x = 42"]
}
```

---

### Slide 13 — Test Coverage *(NEW SLIDE)*

**Backend — Java (pure, no JUnit required):**
- **50 test cases** across 15 categories.
- Categories: basic arithmetic, operator precedence, floats, unary minus, variables, modulo, division-by-zero, error recovery, lexer errors, constant folding, TAC generation, token output, JSON serialization, undefined variables, complex expressions.
- Run: `java MiniCompilerTest` — exits 0 on all pass.

**Frontend — Vitest:**
- **11 unit tests** for the TypeScript API layer.
- Mocked `fetch` — tests HTTP method, URL, request body, response shape, and error paths.
- Run: `npm test` in the `frontend/` directory.

Output: `Results: 50 passed, 0 failed out of 50`

---

### Slide 14 — Deployment Guide *(NEW SLIDE)*

```
Local Development
─────────────────
1. cd backend && start.bat         → Java server on  http://localhost:8080
2. cd frontend && npm run dev      → Vite dev server on  http://localhost:5173
3. Open  http://localhost:5173

Production Hosting
──────────────────
Frontend  →  Netlify / Vercel       (npm run build → deploy dist/)
Backend   →  Railway / Render       (java -jar compiler-server.jar)

Environment variable
  VITE_API_URL=https://your-backend.railway.app
```

---

### Slide 15 — Limitations & Future Work

**Current limitations:**
- Single-statement input per call (no multi-line programs)
- No string or boolean types
- Variables exist only per HTTP session (no persistence)
- No function/subroutine definitions

**Possible extensions:**
- If/else conditional expressions
- While-loop support
- Export session history as `.json`
- Full syntax highlighting via CodeMirror (already installed in frontend)
- Deploy to public URL for live demo sharing

---

### General Design Recommendations

1. **Color theme:** Match the dark GitHub UI — `#0d1117` background, `#58a6ff` blue accent, `#3fb950` green for pass, `#f85149` red for errors.
2. **Code font:** Use JetBrains Mono or Fira Code for all code/token snippets.
3. **Diagrams:** Replace text diagrams with draw.io / Mermaid / PowerPoint SmartArt vector graphics.
4. **Animations:** Pipeline slide — animate each phase bubble lighting up sequentially left to right.
5. **DEMO slide:** Insert a blank "DEMO" slide where presenter switches to the running app.
6. **Footer:** Consistent on every slide — *"Mini-Compiler · Compiler Design · [Year]"*
7. **Token chips:** Recreate the colored token chips as PowerPoint table cells with colored fills matching the UI.
