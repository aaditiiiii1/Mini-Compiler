# Mini-Compiler: Arithmetic Compiler

This project implements a complete compiler design system for arithmetic expressions, featuring a 7-phase compilation pipeline, a Java-based REST API backend, and a React TypeScript frontend with theme support and interactive visualization.

## Features

- **Compiler Core**: 7-phase arithmetic expression compiler in Java
- **Backend**: Lightweight REST API server using Java's built-in HTTP server
- **Frontend**: Modern React application with TypeScript and Vite
- **Testing**: Comprehensive unit tests for both backend and frontend
- **Deployment**: Configured for Vercel (frontend) and Render (backend)

## Compiler Phases

1. Lexical Analysis: Tokenization of input expressions
2. Syntax Analysis: Recursive-descent parsing
3. AST Construction: Building Abstract Syntax Tree
4. Semantic Analysis: Variable validation and error checking
5. Intermediate Code Generation: Three-Address Code (TAC)
6. Optimization: Constant folding
7. Evaluation: AST-based expression evaluation

Supported operations: arithmetic operators (+, -, *, /, %), variables, parentheses, unary negation, floating-point numbers.

## Project Structure

```
MiniCompiler-Studio/
├── MiniCompiler.java              # Standalone CLI compiler
├── README.md                      # Project documentation
├── render.yaml                    # Render deployment configuration
│
├── backend/                       # Java backend
│   ├── CompilerServer.java        # HTTP server implementation
│   ├── MiniCompiler.java          # Compiler core logic
│   ├── MiniCompilerTest.java      # Unit tests
│   ├── start.bat                  # Windows startup script
│   ├── MANIFEST.MF                # JAR manifest
│   └── Dockerfile                 # Docker configuration
│
└── frontend/                      # React frontend
    ├── index.html                 # HTML template
    ├── package.json               # Dependencies
    ├── vite.config.ts             # Vite configuration
    ├── tsconfig.json              # TypeScript configuration
    ├── .env.example               # Environment variables template
    ├── public/                    # Static assets
    └── src/
        ├── main.tsx               # Application entry point
        ├── App.tsx                # Main component
        ├── App.css                # Styles with themes
        ├── api.ts                 # API client
        └── __tests__/             # Test files
            ├── api.test.ts        # API tests
            └── setup.ts           # Test setup
```

## Prerequisites

- Java 17 or higher
- Node.js 18 or higher
- npm or yarn

## Installation and Setup

### Backend Setup

1. Navigate to the backend directory:
   ```
   cd backend
   ```

2. Compile and run the server:
   ```
   start.bat
   ```

   The server will start on http://localhost:8080

### Frontend Setup

1. Navigate to the frontend directory:
   ```
   cd frontend
   ```

2. Install dependencies:
   ```
   npm install
   ```

3. Start the development server:
   ```
   npm run dev
   ```

   The application will be available at http://localhost:5173

## Usage

### Web Interface

Access the web interface at http://localhost:5173 to:
- Enter arithmetic expressions
- View compilation phases step-by-step
- Inspect tokens, AST, TAC, and evaluation results
- Manage variables and view symbol table
- Toggle between light and dark themes

### API Endpoints

The backend provides the following REST endpoints:

- `POST /compile`: Compile an expression
- `GET /vars`: Retrieve symbol table
- `POST /vars/clear`: Clear symbol table
- `GET /health`: Health check

### CLI Mode

For command-line usage without the server:

```
javac -encoding UTF-8 MiniCompiler.java
java MiniCompiler
```

## Testing

### Backend Tests

Run the Java unit tests:

```
cd backend
javac -encoding UTF-8 MiniCompiler.java MiniCompilerTest.java
java MiniCompilerTest
```

### Frontend Tests

Run the frontend tests:

```
cd frontend
npm run test
```

## Deployment

### Frontend (Vercel)

The frontend is configured for deployment on Vercel:

1. Connect the GitHub repository to Vercel
2. Set the root directory to `frontend`
3. Configure environment variables if needed
4. Deploy

### Backend (Render)

The backend is configured for deployment on Render:

1. Connect the GitHub repository to Render
2. Create a new Web Service
3. Set root directory to `backend`
4. Use Docker runtime
5. Deploy

## Technologies Used

- **Backend**: Java 17, HTTP Server API
- **Frontend**: React 18, TypeScript, Vite
- **Styling**: CSS with custom properties for theming
- **Testing**: Pure Java tests, Vitest
- **Deployment**: Vercel, Render, Docker

## License

This project is developed for educational purposes in compiler design.

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

- **Java 17** - compiler core + HTTP server (`com.sun.net.httpserver`, zero dependencies)
- **React 18** + **TypeScript** + **Vite 8**
- **CSS Custom Properties** - full light/dark theme with `[data-theme]`
- **Vitest** + **@testing-library/react** - frontend unit tests
- **Feather-style SVG icons** - no emoji, no icon fonts, no external deps

---

*Compiler Design Project - Java 17 + React 18 + TypeScript*
