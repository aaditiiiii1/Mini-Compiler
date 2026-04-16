import './App.css'
import { useState, useEffect, useRef, useCallback } from 'react'
import { compileExpression, getVars, clearVars, healthCheck, type CompileResult } from './api'

// SVG Icons (no emoji)
const Icon = {
  Logo: () => (
    <svg viewBox="0 0 24 24"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
  ),
  Sun: () => (
    <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
  ),
  Moon: () => (
    <svg viewBox="0 0 24 24"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>
  ),
  Play: () => (
    <svg viewBox="0 0 24 24"><polygon points="5 3 19 12 5 21 5 3"/></svg>
  ),
  Trash: () => (
    <svg viewBox="0 0 24 24"><polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14H6L5 6"/><path d="M10 11v6M14 11v6"/><path d="M9 6V4h6v2"/></svg>
  ),
  X: () => (
    <svg viewBox="0 0 24 24"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
  ),
  Input: () => (
    <svg viewBox="0 0 24 24"><path d="M12 20h9"/><path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4Z"/></svg>
  ),
  Tokens: () => (
    <svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>
  ),
  Tree: () => (
    <svg viewBox="0 0 24 24"><path d="M12 3v3M6 21v-3a6 6 0 0 1 12 0v3"/><circle cx="12" cy="9" r="3"/><circle cx="6" cy="21" r="1"/><circle cx="12" cy="21" r="1"/><circle cx="18" cy="21" r="1"/></svg>
  ),
  Search: () => (
    <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
  ),
  Code: () => (
    <svg viewBox="0 0 24 24"><polyline points="16 18 22 12 16 6"/><polyline points="8 6 2 12 8 18"/></svg>
  ),
  Zap: () => (
    <svg viewBox="0 0 24 24"><polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/></svg>
  ),
  Check: () => (
    <svg viewBox="0 0 24 24"><polyline points="20 6 9 17 4 12"/></svg>
  ),
  Package: () => (
    <svg viewBox="0 0 24 24"><path d="M16.5 9.4l-9-5.19M21 16V8l-9-5-9 5v8l9 5 9-5z"/><path d="M3.27 6.96 12 12.01l8.73-5.05M12 22.08V12"/></svg>
  ),
  History: () => (
    <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
  ),
  Scope: () => (
    <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><line x1="2" y1="12" x2="22" y2="12"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10A15.3 15.3 0 0 1 12 2z"/></svg>
  ),
}

// ══════════════════════════════════════════════════════════════
// CONSTANTS
// ══════════════════════════════════════════════════════════════
const EXAMPLES = [
  { expr: '3 + 5 * 2',    tip: 'Precedence' },
  { expr: '(10 - 4) * 3', tip: 'Grouping' },
  { expr: '3.14 * 2',     tip: 'Float' },
  { expr: '-5 + 10',      tip: 'Unary' },
  { expr: 'x = 6 * 7',    tip: 'Assign' },
  { expr: 'x + 4',        tip: 'Variable' },
  { expr: '15 % 4',       tip: 'Modulo' },
  { expr: '10 / 0',       tip: 'Div by 0' },
  { expr: '3 + * 5',      tip: 'Recovery' },
  { expr: '(2 + 3',       tip: 'Unclosed' },
]

const PHASES = [
  { id: 'tokens',       label: 'Lexer',    num: '1',   icon: <Icon.Tokens />, desc: 'Tokenises the input into a stream of classified symbols (numbers, operators, identifiers, parentheses).' },
  { id: 'ast',          label: 'AST',      num: '2-3', icon: <Icon.Tree />,   desc: 'Recursive-descent parser builds an Abstract Syntax Tree, respecting operator precedence and associativity.' },
  { id: 'semantic',     label: 'Semantic', num: '4',   icon: <Icon.Search />, desc: 'Checks for undefined variables, division/modulo by zero, and collects all warnings before evaluation.' },
  { id: 'tac',          label: 'TAC',      num: '5',   icon: <Icon.Code />,   desc: 'Generates Three-Address Code — one operator per instruction, using temporary variables t1, t2 …' },
  { id: 'folding',      label: 'Folding',  num: '6',   icon: <Icon.Zap />,    desc: 'Constant Folding optimizer eliminates compile-time constant sub-expressions, reducing instruction count.' },
  { id: 'eval',         label: 'Result',   num: '7',   icon: <Icon.Check />,  desc: 'AST-walk evaluator resolves all values with the current symbol table and returns the final numeric result.' },
]

const PIPE = ['Lex', 'Parse', 'AST', 'Semantic', 'TAC', 'Fold', 'Eval']

// ── Helpers ──────────────────────────────────────────────────
function highlightAst(line: string) {
  return line
    .replace(/NUM\(([^)]+)\)/g,    '<b class="an">NUM($1)</b>')
    .replace(/ID\(([^)]+)\)/g,     '<b class="ai">ID($1)</b>')
    .replace(/OP\(([^)]+)\)/g,     '<b class="ao">OP($1)</b>')
    .replace(/UNARY\(([^)]+)\)/g,  '<b class="au">UNARY($1)</b>')
    .replace(/ASSIGN\(([^)]+)\)/g, '<b class="aa">ASSIGN($1)</b>')
    .replace(/([\|└─├+L\s]+)/g,    '<span class="at">$1</span>')
}

function highlightTac(raw: string) {
  return raw
    .replace(/\b(t\d+|result)\b/g, '<span class="ct">$1</span>')
    .replace(/([+\-*/%=])/g,       '<span class="cop">$1</span>')
    .replace(/\b(\d+\.?\d*)\b/g,   '<span class="cn">$1</span>')
    .replace(/\b([a-zA-Z_]\w*)\b(?!<)/g, (m) =>
      /^(t\d+|result)$/.test(m) ? m : `<span class="cid">${m}</span>`)
}

function fmtNum(n: number) {
  if (!isFinite(n)) return String(n)
  return parseFloat(n.toPrecision(10)).toString()
}

interface HistoryItem { expr: string; result: number; ok: boolean }

// ══════════════════════════════════════════════════════════════
// COMPONENT
// ══════════════════════════════════════════════════════════════
export default function App() {
  const [theme,  setTheme]  = useState<'dark'|'light'>('dark')
  const [expr,   setExpr]   = useState('')
  const [result, setResult] = useState<CompileResult | null>(null)
  const [loading,setLoading]= useState(false)
  const [online, setOnline] = useState(false)
  const [phase,  setPhase]  = useState('tokens')
  const [history,setHistory]= useState<HistoryItem[]>([])
  const [vars,   setVars]   = useState<Record<string,number>>({})
  const [fetchErr,setFetchErr] = useState<string|null>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  // Apply theme attribute to root
  useEffect(() => { document.documentElement.setAttribute('data-theme', theme) }, [theme])

  // Health check every 10s
  useEffect(() => {
    const check = async () => setOnline(await healthCheck())
    check()
    const id = setInterval(check, 10000)
    return () => clearInterval(id)
  }, [])

  // Refresh symbol table after each compile
  useEffect(() => { getVars().then(setVars).catch(() => {}) }, [result])

  const run = useCallback(async (expression = expr) => {
    const trimmed = expression.trim()
    if (!trimmed) return
    setLoading(true)
    setFetchErr(null)
    try {
      const cr = await compileExpression(trimmed)
      setResult(cr)
      setPhase('tokens')
      setHistory(h => [{ expr: trimmed, result: cr.result, ok: cr.ok }, ...h.slice(0, 29)])
    } catch (e) {
      setFetchErr(e instanceof Error ? e.message : 'Connection failed — is the backend running?')
    } finally {
      setLoading(false)
    }
  }, [expr])

  const handleKey = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); run() }
  }

  const reset = async () => {
    await clearVars().catch(() => {})
    setVars({}); setResult(null); setFetchErr(null); setExpr('')
    inputRef.current?.focus()
  }

  // ── phase index for pipeline bar ──
  const phaseIdx = PHASES.findIndex(p => p.id === phase)

  // ── stats ──
  const tokenCount = result?.tokens.length ?? 0
  const issueCount = (result?.warnings.length ?? 0) + (result?.runtimeErrors.length ?? 0)
  const folded     = result ? result.tac.length - result.optimisedTac.length : 0

  // ══════════════════════════════════════════════════════════════
  // Phase renderers
  // ══════════════════════════════════════════════════════════════

  const renderTokens = () => {
    if (!result?.tokens.length)
      return <Empty icon={<Icon.Tokens/>} title="No tokens yet" sub="Run an expression above" />
    return (
      <>
        <div className="token-grid">
          {result.tokens.map((t, i) => {
            const c = t.indexOf(':')
            const type = t.slice(0, c)
            const val  = t.slice(c + 1)
            return (
              <span key={i} className={`tok tok-${type}`}>
                <span className="tok-type">{type}</span>
                <span className="tok-val">{val}</span>
              </span>
            )
          })}
        </div>
        <div className="tok-count">{tokenCount} token{tokenCount !== 1 ? 's' : ''} produced</div>
      </>
    )
  }

  const renderAst = () => {
    if (!result?.ast.length)
      return <Empty icon={<Icon.Tree/>} title="No AST yet" sub="Run an expression to build the syntax tree" />
    return (
      <div className="ast-block">
        {result.ast.map((line, i) => (
          <div key={i} dangerouslySetInnerHTML={{ __html: highlightAst(line) }} />
        ))}
      </div>
    )
  }

  const renderSemantic = () => {
    if (!result)
      return <Empty icon={<Icon.Search/>} title="No diagnostics yet" sub="Run an expression to see semantic checks" />
    const warns = result.warnings ?? []
    const errs  = result.runtimeErrors ?? []
    if (warns.length === 0 && errs.length === 0)
      return <div className="msg-list"><div className="msg msg-ok">No warnings or errors — clean compile</div></div>
    return (
      <div className="msg-list">
        {warns.map((w, i) => <div key={'w'+i} className="msg msg-warn">Warning: {w}</div>)}
        {errs.map((e, i)  => <div key={'e'+i} className="msg msg-err">Error: {e}</div>)}
      </div>
    )
  }

  const renderTacBlock = (lines: string[], title: string) => {
    if (!result)
      return <Empty icon={<Icon.Code/>} title={`No ${title} yet`} sub="Run an expression to generate intermediate code" />
    if (lines.length === 0)
      return <div className="msg-list"><div className="msg msg-ok">Single literal — no instructions needed</div></div>
    return (
      <div className="code-wrap">
        <div className="code-titlebar">
          <div className="code-dots">
            <div className="cd cd-r"/><div className="cd cd-y"/><div className="cd cd-g"/>
          </div>
          <span className="code-title">{title}</span>
          <span className="code-count">{lines.length} instruction{lines.length!==1?'s':''}</span>
        </div>
        <div className="code-body">
          {lines.map((l, i) => (
            <div key={i} className="cl" dangerouslySetInnerHTML={{ __html: highlightTac(l) }} />
          ))}
        </div>
      </div>
    )
  }

  const renderFolding = () => {
    if (!result)
      return <Empty icon={<Icon.Zap/>} title="No optimised code yet" sub="Run an expression to see constant folding" />
    return (
      <>
        {folded > 0 && (
          <div className="fold-notice">
            {folded} instruction{folded > 1 ? 's' : ''} folded — {Math.round(folded / result.tac.length * 100)}% reduction
          </div>
        )}
        {renderTacBlock(result.optimisedTac, 'Optimised TAC')}
      </>
    )
  }

  const renderEval = () => {
    if (!result)
      return <Empty icon={<Icon.Check/>} title="Result appears here" sub="Phase 7 evaluates the AST and returns the final value" />
    const hasErr  = result.runtimeErrors.length > 0
    const hasWarn = result.warnings.length > 0
    const cls     = hasErr ? 'err' : hasWarn ? 'warn' : 'ok'
    const label   = hasErr ? 'Runtime Error' : hasWarn ? 'Recovered' : 'Clean Compile'
    return (
      <>
        <div className={`result-banner ${cls}`}>
          <div>
            <div className="result-lbl">Result</div>
            <div className="result-val">{fmtNum(result.result)}</div>
          </div>
          <div className="result-divider" />
          <div>
            <div className="result-lbl">Status</div>
            <div className="result-status">{label}</div>
          </div>
        </div>
        {(hasErr || hasWarn) && (
          <div className="msg-list" style={{ marginTop: 10 }}>
            {result.warnings.map((w,i) => <div key={'w'+i} className="msg msg-warn">Warning: {w}</div>)}
            {result.runtimeErrors.map((e,i) => <div key={'e'+i} className="msg msg-err">Error: {e}</div>)}
          </div>
        )}
      </>
    )
  }

  const phaseRenderers: Record<string, React.ReactNode> = {
    tokens:   renderTokens(),
    ast:      renderAst(),
    semantic: renderSemantic(),
    tac:      renderTacBlock(result?.tac ?? [], 'Three-Address Code'),
    folding:  renderFolding(),
    eval:     renderEval(),
  }

  const activePhase = PHASES.find(p => p.id === phase)!

  return (
    <div className="app">

      {/* ══ HEADER ══════════════════════════════════════════════ */}
      <header className="header">
        <div className="header-left">
          <div className="logo-icon"><Icon.Logo /></div>
          <div>
            <div className="header-name">Mini-Compiler</div>
            <div className="header-sub">Compiler Design Project</div>
          </div>
          <span className="badge badge-accent">7 Phases</span>
          <span className="badge badge-green">Java + React</span>
        </div>

        <div className="header-right">
          <div className={`server-pill ${online ? 'online' : 'offline'}`}>
            <span className="server-dot" />
            {online ? 'Backend Online' : 'Backend Offline'}
          </div>
          <button
            className="theme-btn"
            onClick={() => setTheme(t => t === 'dark' ? 'light' : 'dark')}
            title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}
          >
            {theme === 'dark' ? <Icon.Sun /> : <Icon.Moon />}
          </button>
        </div>
      </header>

      {/* ══ PIPELINE BAR ════════════════════════════════════════ */}
      <div className="pipeline-bar">
        {PIPE.map((label, i) => (
          <div key={label} className="pipe-step">
            <div className={`pipe-node ${result ? (i < phaseIdx ? 'done' : i === phaseIdx ? 'active' : 'done') : ''}`}>
              {i + 1} {label}
            </div>
            {i < PIPE.length - 1 && <span className="pipe-arrow">→</span>}
          </div>
        ))}
      </div>

      {/* ══ MAIN ════════════════════════════════════════════════ */}
      <main className="main">

        {/* ── LEFT COLUMN ── */}
        <div className="left-col">

          {/* Input Card */}
          <div className="card">
            <div className="card-header">
              <span className="card-header-icon"><Icon.Input /></span>
              <span className="card-header-label">Expression Input</span>
            </div>
            <div className="card-body">
              <div className="input-wrap">
                <textarea
                  ref={inputRef}
                  className="editor-input"
                  value={expr}
                  onChange={e => setExpr(e.target.value)}
                  onKeyDown={handleKey}
                  placeholder="e.g.  3 + 5 * 2    or    x = 6 * 7"
                  rows={2}
                  spellCheck={false}
                  autoFocus
                />
                <div className="kbd-hint">
                  <span className="kbd">Enter</span>&nbsp;to run
                </div>
              </div>

              <div className="input-hint">
                Supports&nbsp;
                <code>+</code> <code>-</code> <code>*</code> <code>/</code> <code>%</code>
                &nbsp;· parentheses · floats · unary <code>-</code> · variables (<code>x&nbsp;=&nbsp;5</code>)
              </div>

              {result && (
                <div className="stats-row">
                  <div className="stat-chip sc-blue">
                    <span className="stat-lbl">Tokens</span>
                    <span className="stat-val">{tokenCount}</span>
                  </div>
                  <div className={`stat-chip ${issueCount > 0 ? 'sc-warn' : 'sc-green'}`}>
                    <span className="stat-lbl">Issues</span>
                    <span className="stat-val">{issueCount}</span>
                  </div>
                  {folded > 0 && (
                    <div className="stat-chip sc-green">
                      <span className="stat-lbl">Folded</span>
                      <span className="stat-val">{folded}</span>
                    </div>
                  )}
                  <div className={`stat-chip ${result.ok ? 'sc-green' : 'sc-warn'}`}>
                    <span className="stat-lbl">Status</span>
                    <span className="stat-val">{result.ok ? 'Clean' : 'Recovered'}</span>
                  </div>
                </div>
              )}

              <div className="examples-section">
                <div className="examples-label">Quick Examples</div>
                <div className="chip-row">
                  {EXAMPLES.map(ex => (
                    <span
                      key={ex.expr}
                      className="chip"
                      title={ex.tip}
                      onClick={() => { setExpr(ex.expr); setTimeout(() => run(ex.expr), 0) }}
                    >
                      {ex.expr}
                    </span>
                  ))}
                </div>
              </div>

              <div className="btn-row">
                <button className="btn btn-primary" onClick={() => run()} disabled={loading || !expr.trim()}>
                  {loading ? <span className="spinner"/> : <Icon.Play />}
                  {loading ? 'Compiling…' : 'Compile & Run'}
                </button>
                <button className="btn btn-ghost" onClick={() => { setExpr(''); inputRef.current?.focus() }}>
                  <Icon.X /> Clear
                </button>
                <button className="btn btn-danger" onClick={reset} title="Clear variables and history">
                  <Icon.Trash /> Reset
                </button>
              </div>

              {fetchErr && <div className="error-notice">{fetchErr}</div>}
            </div>
          </div>

          {/* Symbol Table */}
          <div className="card">
            <div className="card-header">
              <span className="card-header-icon"><Icon.Package /></span>
              <span className="card-header-label">Symbol Table</span>
              {Object.keys(vars).length > 0 && (
                <span className="card-header-right">
                  <span className="badge badge-accent">{Object.keys(vars).length} var{Object.keys(vars).length > 1 ? 's' : ''}</span>
                </span>
              )}
            </div>
            {Object.keys(vars).length === 0
              ? <Empty icon={<Icon.Package/>} title="No variables defined" sub={<>Assign with <code>x = 5 * 2</code></>} />
              : (
                <table className="vars-table">
                  <thead><tr><th>Name</th><th>Value</th><th>Type</th></tr></thead>
                  <tbody>
                    {Object.entries(vars).map(([k, v]) => (
                      <tr key={k} onClick={() => { setExpr(k); inputRef.current?.focus() }} title="Click to use in input">
                        <td className="vn">{k}</td>
                        <td className="vv">{fmtNum(v)}</td>
                        <td className="vt">{Number.isInteger(v) ? 'int' : 'float'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )
            }
          </div>

          {/* History */}
          <div className="card">
            <div className="card-header">
              <span className="card-header-icon"><Icon.History /></span>
              <span className="card-header-label">History</span>
              {history.length > 0 && (
                <span className="card-header-right">
                  <span className="badge badge-accent">{history.length}</span>
                </span>
              )}
            </div>
            {history.length === 0
              ? <Empty icon={<Icon.History/>} title="No history yet" sub="Your compiled expressions will appear here" />
              : (
                <div className="history-list">
                  {history.map((item, i) => (
                    <div key={i} className="history-row" title="Click to re-run"
                      onClick={() => { setExpr(item.expr); setTimeout(() => run(item.expr), 0) }}>
                      <span className="h-idx">#{i+1}</span>
                      <span className="h-expr">{item.expr}</span>
                      <span className="h-val">= {fmtNum(item.result)}</span>
                      <span className="h-dot" style={{ background: item.ok ? 'var(--green)' : 'var(--warn)' }} />
                    </div>
                  ))}
                </div>
              )
            }
          </div>
        </div>

        {/* ── RIGHT COLUMN (Phase Panel) ── */}
        <div className="right-col">
          <div className="phase-panel">

            {/* Tabs */}
            <div className="phase-tab-bar">
              {PHASES.map(p => (
                <button
                  key={p.id}
                  className={`ptab ${phase === p.id ? 'active' : ''}`}
                  onClick={() => setPhase(p.id)}
                >
                  <span className="p-num">{p.num}</span>
                  {p.icon}
                  {p.label}
                </button>
              ))}
            </div>

            {/* Phase info + content */}
            <div className="phase-info">
              <div className="phase-desc">{activePhase.desc}</div>
            </div>

            <div className="phase-content">
              {phaseRenderers[phase]}
            </div>
          </div>
        </div>

      </main>

      {/* ══ FOOTER ══════════════════════════════════════════════ */}
      <footer className="footer">
        <span>Mini-Compiler</span>
        <span className="footer-sep">·</span>
        <span>Compiler Design Project</span>
        <span className="footer-sep">·</span>
        <span>Lex → Parse → AST → Semantic → TAC → Fold → Eval</span>
        <span className="footer-sep">·</span>
        <span>Java 17 + React 18 + TypeScript</span>
      </footer>
    </div>
  )
}

// ── Empty state helper ────────────────────────────────────────
function Empty({ icon, title, sub }: { icon: React.ReactNode; title: string; sub: React.ReactNode }) {
  return (
    <div className="empty">
      <div className="empty-icon">{icon}</div>
      <div className="empty-title">{title}</div>
      <div className="empty-sub">{sub}</div>
    </div>
  )
}
