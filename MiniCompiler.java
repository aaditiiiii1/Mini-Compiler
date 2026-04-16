import java.util.*;

/**
 * MiniCompiler — A 7-phase mini arithmetic expression compiler.
 *
 * Compiler Phases:
 *   1. Lexical Analysis  — tokenise input into meaningful symbols
 *   2. Syntax Analysis   — recursive-descent parser
 *   3. AST Construction  — build an Abstract Syntax Tree
 *   4. Semantic Analysis — undefined variables, division-by-zero checks
 *   5. Intermediate Code — Three-Address Code (TAC) generation
 *   6. Optimisation      — Constant Folding on the AST
 *   7. Evaluation        — AST-walk evaluator
 *
 * Operators  : + - * / % (modulo), unary -
 * Types      : integers, floating-point
 * Variables  : assignment  x = expr , then re-use x
 * Commands   : vars | clear | exit
 */
public class MiniCompiler {

    // =================== TOKEN TYPES ====================
    enum TokenType {
        NUMBER, PLUS, MINUS, MUL, DIV, MOD,
        LPAREN, RPAREN, ASSIGN, IDENTIFIER, EOF
    }

    static class Token {
        final TokenType type;
        final String    value;
        final int       col;

        Token(TokenType type, String value, int col) {
            this.type  = type;
            this.value = value;
            this.col   = col;
        }

        @Override public String toString() { return value; }
    }

    // =================== LEXER ==========================
    static class Lexer {
        private final String input;
        private int pos = 0;
        final List<String> warnings = new ArrayList<>();
        boolean hasError = false;

        Lexer(String input) { this.input = input.trim(); }

        List<Token> tokenize() {
            List<Token> tokens = new ArrayList<>();
            while (pos < input.length()) {
                char c = input.charAt(pos);
                if      (Character.isWhitespace(c)) { pos++; }
                else if (Character.isDigit(c)
                      || (c == '.' && pos + 1 < input.length()
                                   && Character.isDigit(input.charAt(pos + 1))))
                    { tokens.add(readNumber()); }
                else if (Character.isLetter(c) || c == '_') { tokens.add(readIdent()); }
                else if (c == '+') { tokens.add(tok(TokenType.PLUS,   "+")); }
                else if (c == '-') { tokens.add(tok(TokenType.MINUS,  "-")); }
                else if (c == '*') { tokens.add(tok(TokenType.MUL,    "*")); }
                else if (c == '/') { tokens.add(tok(TokenType.DIV,    "/")); }
                else if (c == '%') { tokens.add(tok(TokenType.MOD,    "%")); }
                else if (c == '(') { tokens.add(tok(TokenType.LPAREN, "(")); }
                else if (c == ')') { tokens.add(tok(TokenType.RPAREN, ")")); }
                else if (c == '=') { tokens.add(tok(TokenType.ASSIGN, "=")); }
                else {
                    warn("[Lexer  WARN] Invalid char '" + c + "' at col " + pos + " -- skipped.");
                    pos++;
                }
            }
            tokens.add(new Token(TokenType.EOF, "EOF", pos));
            return tokens;
        }

        private Token tok(TokenType t, String v) { return new Token(t, v, pos++); }

        private Token readIdent() {
            int start = pos;
            StringBuilder sb = new StringBuilder();
            while (pos < input.length()
                   && (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_'))
                sb.append(input.charAt(pos++));
            return new Token(TokenType.IDENTIFIER, sb.toString(), start);
        }

        private Token readNumber() {
            int start = pos;
            StringBuilder sb = new StringBuilder();
            while (pos < input.length() && Character.isDigit(input.charAt(pos)))
                sb.append(input.charAt(pos++));
            if (pos < input.length() && input.charAt(pos) == '.') {
                sb.append(input.charAt(pos++));
                while (pos < input.length() && Character.isDigit(input.charAt(pos)))
                    sb.append(input.charAt(pos++));
            }
            return new Token(TokenType.NUMBER, sb.toString(), start);
        }

        private void warn(String msg) { warnings.add(msg); hasError = true; }
    }

    // =================== AST NODES ======================
    interface ASTNode {
        String toJson();
    }

    static class NumberNode implements ASTNode {
        double value;
        NumberNode(double value) { this.value = value; }
        public String toJson() { return "{\"type\":\"Number\",\"value\":" + fmt(value) + "}"; }
    }

    static class IdentifierNode implements ASTNode {
        final String name;
        IdentifierNode(String name) { this.name = name; }
        public String toJson() { return "{\"type\":\"Identifier\",\"name\":\"" + esc(name) + "\"}"; }
    }

    static class UnaryOpNode implements ASTNode {
        final String op;
        ASTNode operand;
        UnaryOpNode(String op, ASTNode operand) { this.op = op; this.operand = operand; }
        public String toJson() {
            return "{\"type\":\"Unary\",\"op\":\"" + esc(op) + "\",\"operand\":" + operand.toJson() + "}";
        }
    }

    static class BinaryOpNode implements ASTNode {
        final String op;
        ASTNode left, right;
        BinaryOpNode(String op, ASTNode left, ASTNode right) {
            this.op = op; this.left = left; this.right = right;
        }
        public String toJson() {
            return "{\"type\":\"BinaryOp\",\"op\":\"" + esc(op)
                 + "\",\"left\":" + left.toJson()
                 + ",\"right\":" + right.toJson() + "}";
        }
    }

    static class AssignNode implements ASTNode {
        final String name;
        ASTNode expr;
        AssignNode(String name, ASTNode expr) { this.name = name; this.expr = expr; }
        public String toJson() {
            return "{\"type\":\"Assign\",\"name\":\"" + esc(name) + "\",\"expr\":" + expr.toJson() + "}";
        }
    }

    // =================== SYMBOL TABLE ===================
    static final Map<String, Double> symbolTable = new LinkedHashMap<>();

    // =================== PARSER =========================
    static class Parser {
        private final List<Token> tokens;
        private int pos = 0;
        final List<String> warnings = new ArrayList<>();
        boolean hasError = false;

        Parser(List<Token> tokens) { this.tokens = tokens; }

        private Token cur() {
            return pos < tokens.size() ? tokens.get(pos) : tokens.get(tokens.size() - 1);
        }
        private Token advance() {
            Token t = cur();
            if (pos < tokens.size() - 1) pos++;
            return t;
        }
        private boolean isMulOp() {
            return cur().type == TokenType.MUL
                || cur().type == TokenType.DIV
                || cur().type == TokenType.MOD;
        }

        ASTNode parse() {
            ASTNode node = expression();
            while (cur().type != TokenType.EOF) {
                if (cur().type == TokenType.RPAREN) {
                    warn("[Parser WARN] Extra ')' at col " + cur().col + " -- skipped.");
                    advance();
                } else {
                    warn("[Parser WARN] Unexpected token '" + cur().value + "' -- skipped.");
                    advance();
                }
            }
            return node;
        }

        // Grammar:
        //  expression -> ID '=' expression | addSub
        //  addSub     -> mulDiv (('+' | '-') mulDiv)*
        //  mulDiv     -> unary  (('*' | '/' | '%') unary)*
        //  unary      -> '-' unary | '+' unary | primary
        //  primary    -> NUMBER | IDENTIFIER | '(' expression ')'

        private ASTNode expression() {
            if (cur().type == TokenType.IDENTIFIER
                    && pos + 1 < tokens.size()
                    && tokens.get(pos + 1).type == TokenType.ASSIGN) {
                String name = advance().value;
                advance(); // consume '='
                return new AssignNode(name, addSub());
            }
            return addSub();
        }

        private ASTNode addSub() {
            ASTNode left = mulDiv();
            while (cur().type == TokenType.PLUS || cur().type == TokenType.MINUS) {
                String op = advance().value;
                // Only recover if there is truly nothing to the right
                if (cur().type == TokenType.EOF || cur().type == TokenType.RPAREN) {
                    warn("[Recovery] Dangling '" + op + "' -- inserting 0.");
                    return new BinaryOpNode(op, left, new NumberNode(0));
                }
                left = new BinaryOpNode(op, left, mulDiv());
            }
            return left;
        }

        private ASTNode mulDiv() {
            ASTNode left = unary();
            while (isMulOp()) {
                String op = advance().value;
                // Only recover if we genuinely have no right-hand operand coming.
                // A leading '-' or '+' after * / % is a valid unary prefix — do NOT recover.
                if (cur().type == TokenType.EOF || cur().type == TokenType.RPAREN) {
                    warn("[Recovery] Missing operand after '" + op + "' -- inserting 1.");
                    return new BinaryOpNode(op, left, new NumberNode(1));
                }
                left = new BinaryOpNode(op, left, unary());
            }
            return left;
        }

        private ASTNode unary() {
            if (cur().type == TokenType.MINUS) { advance(); return new UnaryOpNode("-", primary()); }
            if (cur().type == TokenType.PLUS)  { advance(); return primary(); }
            return primary();
        }

        private ASTNode primary() {
            Token t = cur();
            if (t.type == TokenType.NUMBER) {
                advance();
                return new NumberNode(Double.parseDouble(t.value));
            }
            if (t.type == TokenType.IDENTIFIER) {
                advance();
                return new IdentifierNode(t.value);
            }
            if (t.type == TokenType.LPAREN) {
                advance();
                ASTNode node = addSub();
                if (cur().type == TokenType.RPAREN) advance();
                else warn("[Recovery] Missing ')' -- auto-closed.");
                return node;
            }
            if (t.type == TokenType.MUL || t.type == TokenType.DIV || t.type == TokenType.MOD
             || t.type == TokenType.PLUS || t.type == TokenType.MINUS) {
                warn("[Recovery] Unexpected operator '" + t.value + "' where operand expected -- inserting 0.");
                advance();
                return new NumberNode(0);
            }
            return new NumberNode(0);
        }

        private void warn(String msg) { warnings.add(msg); hasError = true; }
    }

    // =================== CONSTANT FOLDING ===============
    static ASTNode constantFold(ASTNode node) {
        if (node instanceof BinaryOpNode b) {
            b.left  = constantFold(b.left);
            b.right = constantFold(b.right);
            if (b.left instanceof NumberNode l && b.right instanceof NumberNode r) {
                double res = applyBinOp(b.op, l.value, r.value);
                if (!Double.isNaN(res)) return new NumberNode(res);
            }
            return b;
        }
        if (node instanceof UnaryOpNode u) {
            u.operand = constantFold(u.operand);
            if (u.operand instanceof NumberNode n) return new NumberNode(-n.value);
            return u;
        }
        if (node instanceof AssignNode a) { a.expr = constantFold(a.expr); return a; }
        return node;
    }

    static double applyBinOp(String op, double l, double r) {
        return switch (op) {
            case "+" -> l + r;
            case "-" -> l - r;
            case "*" -> l * r;
            case "/" -> r == 0 ? Double.NaN : l / r;
            case "%" -> r == 0 ? Double.NaN : l % r;
            default  -> Double.NaN;
        };
    }

    // =================== TAC GENERATOR ==================
    static class CodeGenerator {
        private int tempCount = 0;
        private final List<String> code = new ArrayList<>();

        String newTemp() { return "t" + (++tempCount); }

        String generate(ASTNode node) {
            if (node instanceof NumberNode n)      return fmt(n.value);
            if (node instanceof IdentifierNode id) return id.name;
            if (node instanceof UnaryOpNode u) {
                String operand = generate(u.operand);
                String temp = newTemp();
                code.add(temp + " = -" + operand);
                return temp;
            }
            if (node instanceof BinaryOpNode b) {
                String left  = generate(b.left);
                String right = generate(b.right);
                String temp  = newTemp();
                code.add(temp + " = " + left + " " + b.op + " " + right);
                return temp;
            }
            if (node instanceof AssignNode a) {
                String expr = generate(a.expr);
                code.add(a.name + " = " + expr);
                return a.name;
            }
            return "?";
        }

        List<String> getCode() { return code; }
    }

    // =================== EVALUATOR ======================
    static class EvalResult {
        final double value;
        final List<String> errors;
        EvalResult(double v, List<String> e) { value = v; errors = e; }
    }

    static EvalResult evalAst(ASTNode node, Map<String, Double> sym) {
        List<String> errors = new ArrayList<>();
        double val = evalNode(node, sym, errors);
        return new EvalResult(val, errors);
    }

    private static double evalNode(ASTNode node, Map<String, Double> sym, List<String> errors) {
        if (node instanceof NumberNode n)  return n.value;
        if (node instanceof IdentifierNode id) {
            if (sym.containsKey(id.name)) return sym.get(id.name);
            errors.add("[Semantic ERROR] Undefined variable '" + id.name + "' -- using 0.");
            return 0;
        }
        if (node instanceof UnaryOpNode u) return -evalNode(u.operand, sym, errors);
        if (node instanceof BinaryOpNode b) {
            double l = evalNode(b.left,  sym, errors);
            double r = evalNode(b.right, sym, errors);
            return switch (b.op) {
                case "+" -> l + r;
                case "-" -> l - r;
                case "*" -> l * r;
                case "%" -> { if (r==0) { errors.add("[Runtime ERROR] Modulo by zero -- result is 0."); yield 0; } yield l%r; }
                case "/" -> { if (r==0) { errors.add("[Runtime ERROR] Division by zero -- result is 0."); yield 0; } yield l/r; }
                default  -> 0;
            };
        }
        if (node instanceof AssignNode a) {
            double val = evalNode(a.expr, sym, errors);
            sym.put(a.name, val);
            return val;
        }
        return 0;
    }

    // =================== COMPILE RESULT =================
    static class CompileResult {
        final String        input;
        final List<String>  tokens        = new ArrayList<>();
        final List<String>  ast           = new ArrayList<>();
        String              astJson       = "{}";
        final List<String>  tac           = new ArrayList<>();
        final List<String>  optimisedTac  = new ArrayList<>();
        final List<String>  warnings      = new ArrayList<>();
        final List<String>  runtimeErrors = new ArrayList<>();
        double result = 0;
        boolean ok = true;

        CompileResult(String input) { this.input = input; }

        String toJson() {
            return "{"
                 + "\"input\":"         + jStr(input)         + ","
                 + "\"tokens\":"        + jArr(tokens)        + ","
                 + "\"ast\":"           + jArr(ast)           + ","
                 + "\"astJson\":"       + astJson             + ","
                 + "\"tac\":"           + jArr(tac)           + ","
                 + "\"optimisedTac\":"  + jArr(optimisedTac)  + ","
                 + "\"warnings\":"      + jArr(warnings)      + ","
                 + "\"runtimeErrors\":" + jArr(runtimeErrors) + ","
                 + "\"result\":"        + fmt(result)          + ","
                 + "\"ok\":"            + ok
                 + "}";
        }

        static String jStr(String s) {
            return "\"" + s.replace("\\","\\\\").replace("\"","\\\"")
                           .replace("\n","\\n").replace("\r","") + "\"";
        }
        static String jArr(List<String> list) {
            if (list.isEmpty()) return "[]";
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(jStr(list.get(i)));
            }
            return sb.append("]").toString();
        }
    }

    // =================== AST TEXT PRINTER ===============
    static class AstPrinter {
        final List<String> lines = new ArrayList<>();
        void print(ASTNode n, String indent, boolean last) {
            String b = last ? "L-- " : "+-- ";
            String x = last ? "    " : "|   ";
            if (n instanceof NumberNode nd)    { lines.add(indent + b + "NUM("    + fmt(nd.value) + ")"); }
            else if (n instanceof IdentifierNode id) { lines.add(indent + b + "ID("     + id.name + ")"); }
            else if (n instanceof UnaryOpNode u) {
                lines.add(indent + b + "UNARY(" + u.op + ")");
                print(u.operand, indent + x, true);
            } else if (n instanceof BinaryOpNode bi) {
                lines.add(indent + b + "OP("    + bi.op + ")");
                print(bi.left,  indent + x, false);
                print(bi.right, indent + x, true);
            } else if (n instanceof AssignNode a) {
                lines.add(indent + b + "ASSIGN(" + a.name + " =)");
                print(a.expr, indent + x, true);
            }
        }
    }

    // =================== DEEP CLONE =====================
    static ASTNode cloneAst(ASTNode n) {
        if (n instanceof NumberNode nd)      return new NumberNode(nd.value);
        if (n instanceof IdentifierNode id)  return new IdentifierNode(id.name);
        if (n instanceof UnaryOpNode u)      return new UnaryOpNode(u.op, cloneAst(u.operand));
        if (n instanceof BinaryOpNode b)     return new BinaryOpNode(b.op, cloneAst(b.left), cloneAst(b.right));
        if (n instanceof AssignNode a)       return new AssignNode(a.name, cloneAst(a.expr));
        return new NumberNode(0);
    }

    // =================== MAIN COMPILE API ===============
    /**
     * compile() — runs all 7 phases and returns a structured CompileResult.
     * Called by both CLI and the HTTP API server.
     */
    static CompileResult compile(String input, Map<String, Double> sym) {
        CompileResult cr = new CompileResult(input);

        // Phase 1 — Lexical Analysis
        Lexer lexer = new Lexer(input);
        List<Token> tokenList = lexer.tokenize();
        cr.warnings.addAll(lexer.warnings);
        if (lexer.hasError) cr.ok = false;
        for (Token t : tokenList)
            if (t.type != TokenType.EOF)
                cr.tokens.add(t.type.name() + ":" + t.value);

        // Phase 2 — Syntax Analysis + Phase 3 — AST
        Parser parser = new Parser(tokenList);
        ASTNode ast = parser.parse();
        cr.warnings.addAll(parser.warnings);
        if (parser.hasError) cr.ok = false;

        AstPrinter printer = new AstPrinter();
        printer.print(ast, "", true);
        cr.ast.addAll(printer.lines);
        cr.astJson = ast.toJson();

        // Phase 4 — Semantic (live in evaluator)

        // Phase 5 — TAC on original AST
        CodeGenerator cgOrig = new CodeGenerator();
        cgOrig.generate(ast);
        cr.tac.addAll(cgOrig.getCode());

        // Phase 6 — Constant Folding + optimised TAC
        ASTNode opt = constantFold(cloneAst(ast));
        CodeGenerator cgOpt = new CodeGenerator();
        cgOpt.generate(opt);
        cr.optimisedTac.addAll(cgOpt.getCode());

        // Phase 7 — Evaluation
        EvalResult er = evalAst(opt, sym);
        cr.result = er.value;
        cr.runtimeErrors.addAll(er.errors);
        if (!er.errors.isEmpty()) cr.ok = false;

        return cr;
    }

    // =================== UTILITIES ======================
    static String fmt(double v) {
        return (v == Math.floor(v) && !Double.isInfinite(v))
               ? String.valueOf((long) v) : String.valueOf(v);
    }

    static String esc(String s) {
        return s.replace("\\","\\\\").replace("\"","\\\"");
    }

    // =================== CLI ============================
    public static void main(String[] args) {
        System.out.println("+-----------------------------------------------+");
        System.out.println("|          Mini-Compiler  Simulation             |");
        System.out.println("|  7 Phases: Lex->Parse->AST->TAC->Fold->Eval    |");
        System.out.println("+-----------------------------------------------+");
        System.out.println("|  Operators : + - * / % ()  floats  unary -     |");
        System.out.println("|  Variables : x = 5 * 2      then use  x + 1    |");
        System.out.println("|  Commands  : vars | clear | exit                |");
        System.out.println("+-----------------------------------------------+\n");

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print(">> ");
                String input = sc.nextLine().trim();

                if      (input.equalsIgnoreCase("exit"))  { break; }
                else if (input.isEmpty())                 { continue; }
                else if (input.equalsIgnoreCase("vars")) {
                    if (symbolTable.isEmpty()) System.out.println("  (no variables defined)");
                    else symbolTable.forEach((k, v) -> System.out.println("  " + k + " = " + fmt(v)));
                    continue;
                }
                else if (input.equalsIgnoreCase("clear")) {
                    symbolTable.clear();
                    System.out.println("  Symbol table cleared.");
                    continue;
                }

                CompileResult cr = compile(input, symbolTable);

                printSep("Phase 1  Lexical Analysis");
                System.out.print("  Tokens : ");
                cr.tokens.forEach(t -> System.out.print("[" + t.split(":")[1] + "] "));
                System.out.println();

                printSep("Phase 2+3  Syntax Analysis + AST");
                cr.ast.forEach(line -> System.out.println("  " + line));

                printSep("Phase 4  Semantic Analysis");
                if (cr.warnings.isEmpty()) System.out.println("  No warnings.");
                else cr.warnings.forEach(w -> System.out.println("  " + w));

                printSep("Phase 5  Intermediate Code (TAC)");
                if (cr.tac.isEmpty()) System.out.println("  (single literal -- no TAC)");
                else cr.tac.forEach(l -> System.out.println("  " + l));

                printSep("Phase 6  Constant Folding (Optimised TAC)");
                if (cr.optimisedTac.size() < cr.tac.size())
                    System.out.println("  Folded " + (cr.tac.size() - cr.optimisedTac.size()) + " instruction(s).");
                if (cr.optimisedTac.isEmpty()) System.out.println("  (already optimal)");
                else cr.optimisedTac.forEach(l -> System.out.println("  " + l));

                printSep("Phase 7  Evaluation");
                cr.runtimeErrors.forEach(e -> System.out.println("  " + e));
                System.out.println("  Result = " + fmt(cr.result));
                System.out.println("  Status = " + (cr.ok ? "OK" : "Completed with recovery"));
                System.out.println();
            }
        }
    }

    static void printSep(String label) {
        System.out.println("\n-- " + label + " " + "-".repeat(Math.max(0, 48 - label.length())));
    }
}
