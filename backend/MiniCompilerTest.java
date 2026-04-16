import java.util.*;

// MiniCompilerTest - pure-Java test runner
public class MiniCompilerTest {

    // tiny test harness
    static int pass = 0, fail = 0;

    static void eq(String label, double expected, double actual) {
        if (Math.abs(expected - actual) < 1e-9) {
            System.out.printf("  [PASS] %s%n", label);
            pass++;
        } else {
            System.out.printf("  [FAIL] %s  expected=%.6f  got=%.6f%n", label, expected, actual);
            fail++;
        }
    }
    static void isTrue(String label, boolean cond) {
        if (cond) { System.out.printf("  [PASS] %s%n", label); pass++; }
        else      { System.out.printf("  [FAIL] %s%n", label);  fail++; }
    }
    static void section(String title) {
        System.out.println("\n=== " + title + " ===");
    }

    // ── helpers ───────────────────────────────────────────────────────
    static double run(String expr) {
        Map<String, Double> sym = new LinkedHashMap<>();
        return MiniCompiler.compile(expr, sym).result;
    }
    static double run(String expr, Map<String, Double> sym) {
        return MiniCompiler.compile(expr, sym).result;
    }
    static MiniCompiler.CompileResult cr(String expr) {
        return MiniCompiler.compile(expr, new LinkedHashMap<>());
    }
    static MiniCompiler.CompileResult cr(String expr, Map<String, Double> sym) {
        return MiniCompiler.compile(expr, sym);
    }

    // ─────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        System.out.println("Running MiniCompiler Tests...\n");

        // ── 1. Basic arithmetic ──────────────────────────────────────
        section("1. Basic Arithmetic");
        eq("1 + 2",         3,    run("1 + 2"));
        eq("10 - 3",        7,    run("10 - 3"));
        eq("4 * 5",        20,    run("4 * 5"));
        eq("20 / 4",        5,    run("20 / 4"));
        eq("17 % 5",        2,    run("17 % 5"));

        // ── 2. Operator precedence ───────────────────────────────────
        section("2. Operator Precedence");
        eq("3 + 5 * 2",    13,    run("3 + 5 * 2"));
        eq("(3 + 5) * 2",  16,    run("(3 + 5) * 2"));
        eq("10 - 2 * 3",    4,    run("10 - 2 * 3"));
        eq("100 / 5 / 4",   5,    run("100 / 5 / 4"));

        // ── 3. Floats ────────────────────────────────────────────────
        section("3. Floating-Point");
        eq("3.14 * 2",      6.28, run("3.14 * 2"));
        eq("1.5 + 2.5",     4.0,  run("1.5 + 2.5"));
        eq("10.0 / 4",      2.5,  run("10.0 / 4"));

        // ── 4. Unary minus ───────────────────────────────────────────
        section("4. Unary Minus");
        eq("-5 + 10",       5,    run("-5 + 10"));
        eq("-(3 + 2)",     -5,    run("-(3 + 2)"));
        eq("-(-4)",         4,    run("-(-4)"));

        // ── 5. Variables ─────────────────────────────────────────────
        section("5. Variables");
        Map<String, Double> sym = new LinkedHashMap<>();
        run("x = 6 * 7", sym);
        eq("x = 6*7 → 42",             42, sym.get("x"));
        eq("x + 4 uses stored x",       46, run("x + 4", sym));
        run("pi = 3.14", sym);
        eq("pi stored",               3.14, sym.get("pi"));
        eq("pi * 2",                  6.28, run("pi * 2", sym));

        // ── 6. Modulo ────────────────────────────────────────────────
        section("6. Modulo Operator");
        eq("10 % 3",        1,    run("10 % 3"));
        eq("9 % 3",         0,    run("9 % 3"));
        eq("7 % 7",         0,    run("7 % 7"));
        MiniCompiler.CompileResult modZero = cr("5 % 0");
        isTrue("5 % 0 returns 0",         modZero.result == 0);
        isTrue("5 % 0 has runtime error",  !modZero.runtimeErrors.isEmpty());

        // ── 7. Division by zero ──────────────────────────────────────
        section("7. Division By Zero");
        MiniCompiler.CompileResult divZero = cr("10 / 0");
        eq("10/0 result = 0", 0, divZero.result);
        isTrue("10/0 has runtime error", !divZero.runtimeErrors.isEmpty());

        // ── 8. Error recovery ────────────────────────────────────────
        section("8. Error Recovery");
        MiniCompiler.CompileResult danglingOp = cr("3 +");
        isTrue("3+ has warnings",          !danglingOp.warnings.isEmpty());
        isTrue("3+ result is 3",            danglingOp.result == 3);

        MiniCompiler.CompileResult unexpectedOp = cr("3 + * 5");
        isTrue("3+*5 has warnings",         !unexpectedOp.warnings.isEmpty());

        MiniCompiler.CompileResult missingParen = cr("(3 + 2");
        isTrue("(3+2 has warnings",         !missingParen.warnings.isEmpty());
        eq("(3+2 auto-closed = 5",        5, missingParen.result);

        // 9. Lexer - Invalid Characters
        section("9. Lexer - Invalid Characters");
        MiniCompiler.CompileResult invChar = cr("3 + @5");
        isTrue("3+@5 has lexer warning",    invChar.warnings.stream()
               .anyMatch(w -> w.contains("Lexer")));

        // ── 10. Constant Folding ─────────────────────────────────────
        section("10. Constant Folding");
        MiniCompiler.CompileResult fold = cr("2 * 3 + 4 * 5");
        eq("2*3+4*5 = 26",        26, fold.result);
        isTrue("Folding reduces TAC instructions",
               fold.optimisedTac.size() <= fold.tac.size());

        // ── 11. TAC generation ───────────────────────────────────────
        section("11. TAC Generation");
        MiniCompiler.CompileResult tac = cr("3 + 5 * 2");
        isTrue("TAC not empty",            !tac.tac.isEmpty());
        isTrue("TAC contains temp var",     tac.tac.stream().anyMatch(l -> l.contains("t")));

        // ── 12. Token output ─────────────────────────────────────────
        section("12. Token Output");
        MiniCompiler.CompileResult tok = cr("x + 3");
        isTrue("Token list not empty",     !tok.tokens.isEmpty());
        isTrue("Has IDENTIFIER token",      tok.tokens.stream().anyMatch(t -> t.startsWith("IDENTIFIER")));
        isTrue("Has NUMBER token",          tok.tokens.stream().anyMatch(t -> t.startsWith("NUMBER")));
        isTrue("Has PLUS token",            tok.tokens.stream().anyMatch(t -> t.startsWith("PLUS")));

        // ── 13. JSON serialisation ───────────────────────────────────
        section("13. JSON Serialisation");
        String json = cr("2 + 3").toJson();
        isTrue("JSON has input field",      json.contains("\"input\""));
        isTrue("JSON has result field",     json.contains("\"result\""));
        isTrue("JSON has ok field",         json.contains("\"ok\""));
        isTrue("JSON has ast field",        json.contains("\"ast\""));
        isTrue("JSON has tac field",        json.contains("\"tac\""));

        // ── 14. Undefined variable ───────────────────────────────────
        section("14. Undefined Variable");
        MiniCompiler.CompileResult undef = cr("y + 5");
        isTrue("undefined var has runtime error",  !undef.runtimeErrors.isEmpty());
        eq("undefined var evaluates to 5",          5, undef.result);

        // ── 15. Complex expressions ──────────────────────────────────
        section("15. Complex Expressions");
        eq("(1+2)*(3+4)",       21, run("(1+2)*(3+4)"));
        eq("100/5*2-10",        30, run("100/5*2-10"));
        eq("-3 * -4",           12, run("-3 * -4"));

        // ─── Summary ─────────────────────────────────────────────────
        System.out.println("\n" + "=".repeat(44));
        System.out.printf("  Results: %d passed, %d failed out of %d%n", pass, fail, pass + fail);
        System.out.println("=".repeat(44));
        System.exit(fail > 0 ? 1 : 0);
    }
}
