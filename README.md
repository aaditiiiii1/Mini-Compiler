# Mini-Compiler — Full-Stack 7-Phase Arithmetic Compiler

A complete **Compiler Design** project featuring a 7-phase arithmetic expression compiler,
a lightweight Java REST API backend, and a professional React + TypeScript frontend with
**light / dark theme**, SVG icons, and a per-phase compiler explorer.

---

## Features

| Layer | Technology | Highlights |
|---|---|---|
| **Compiler** | Java 17 | 7 phases, floats, variables, `%` modulo, error recovery, constant folding |
| **Backend** | Java 17 (`com.sun.net.httpserver`) | 4 REST endpoints, no external dependencies, CORS |
| **Frontend** | React 18 + TypeScript + Vite | Light/dark theme, SVG icons, 7-tab phase inspector, symbol table, history |
| **Tests** | Pure Java + Vitest | 50 backend + 11 frontend tests |

---

## Compiler Phases

```
[1] Lexer  →  [2] Parser  →  [3] AST Build  →  [4] Semantic Analysis
             →  [5] TAC Gen  →  [6] Constant Folding  →  [7] Evaluation
```

Operators: `+` `-` `*` `/` `%` `( )` · unary `-` · floats · variables (`x = 5 * 2`)

---

## Project Structure

```
CD Project/
├── MiniCompiler.java          ← standalone CLI compiler (root, no server needed)
├── README.md
├── .gitignore
│
├── backend/
│   ├── MiniCompiler.java      ← compiler core (used by HTTP server)
│   ├── CompilerServer.java    ← HTTP server (port 8080)
│   ├── MiniCompilerTest.java  ← 50 unit test cases (pure Java)
│   └── start.bat              ← Windows: compile + start server
│
└── frontend/                  ← React 18 + TypeScript + Vite
    ├── src/
    │   ├── App.tsx            ← main component (theme toggle, SVG icons, all phases)
    │   ├── App.css            ← CSS custom properties — light + dark themes
    │   ├── api.ts             ← TypeScript API client
    │   └── __tests__/
    │       └── api.test.ts    ← 11 Vitest tests
    ├── index.html
    ├── vite.config.ts         ← /compile /vars /health → proxy to :8080
    └── package.json
```

## UI

The frontend features a **dual-theme** interface (dark by default, toggle to light):
- Collapsible **pipeline bar** showing all 7 compiler phases
- **Token chips** — colour-coded by type (number, operator, identifier, etc.)
- **AST viewer** with syntax-highlighted tree lines
- **TAC code block** with line numbers and per-token colouring
- **Constant-folding diff** — shows instructions saved
- **Symbol table** — live variable watch, click to re-use
- **History** — last 30 expressions, click to re-run

---

## Quick Start

### 1. Backend (Java server)

```bat
cd backend
start.bat
```

Server starts at **http://localhost:8080**

Requires Java 17+. No Maven or external jars needed.

### 2. Frontend (React dev server)

```bat
cd frontend
npm install
npm run dev
```

Open **http://localhost:5173** — the Vite dev server proxies `/compile`, `/vars`, `/health` to the Java backend automatically.

---

## CLI Mode (no server needed)

```bat
javac -encoding UTF-8 MiniCompiler.java
java MiniCompiler
```

Then type expressions at the prompt. Commands: `vars`, `clear`, `exit`.

---

## Tests

### Backend (50 tests, pure Java)

```bat
cd backend
javac -encoding UTF-8 MiniCompiler.java MiniCompilerTest.java
java MiniCompilerTest
```

Expected: `Results: 50 passed, 0 failed out of 50`

### Frontend (11 tests, Vitest)

```bat
cd frontend
npm test
```

---

## REST API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/compile` | `{ "expression": "..." }` → `CompileResult` |
| `GET` | `/vars` | All session variables as JSON |
| `POST` | `/vars/clear` | Clear session variables |
| `GET` | `/health` | `{ "status": "ok" }` |

---

## Production Deployment

- **Frontend:** `npm run build` → deploy `dist/` to Netlify or Vercel.
- **Backend:** Compile and run `java CompilerServer` on Railway, Render, or any VPS with Java 17+.
- Set the env var `VITE_API_URL=https://your-backend.domain.com` before building the frontend.

---

## Tech Stack

- **Java 17** — compiler core + HTTP server (`com.sun.net.httpserver`, zero dependencies)
- **React 18** + **TypeScript** + **Vite 8**
- **CSS Custom Properties** — full light/dark theme with `[data-theme]`
- **Vitest** + **@testing-library/react** — frontend unit tests
- **Feather-style SVG icons** — no emoji, no icon fonts, no external deps

---

*Compiler Design Project — Java 17 + React 18 + TypeScript*
