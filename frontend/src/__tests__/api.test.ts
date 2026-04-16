import { describe, it, expect, vi, beforeEach } from 'vitest'
import {
  compileExpression,
  getVars,
  clearVars,
  healthCheck,
  type CompileResult,
} from '../api'

// ── helpers ───────────────────────────────────────────────────────────

const mockResult: CompileResult = {
  input: '3 + 5',
  tokens: ['NUMBER:3', 'PLUS:+', 'NUMBER:5'],
  ast: ['L-- OP(+)', '    +-- NUM(3)', '    L-- NUM(5)'],
  astJson: '{}',
  tac: ['t1 = 3 + 5'],
  optimisedTac: [],
  warnings: [],
  runtimeErrors: [],
  result: 8,
  ok: true,
}

function mockFetch(body: unknown, status = 200) {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
    ok: status >= 200 && status < 300,
    status,
    json: () => Promise.resolve(body),
    text: () => Promise.resolve(JSON.stringify(body)),
  } as Response))
}

beforeEach(() => vi.restoreAllMocks())

// ── compileExpression ─────────────────────────────────────────────────

describe('compileExpression()', () => {
  it('calls POST /compile with correct JSON body', async () => {
    mockFetch(mockResult)
    await compileExpression('3 + 5')
    expect(fetch).toHaveBeenCalledOnce()
    const [url, opts] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(url).toMatch(/\/compile$/)
    expect(opts.method).toBe('POST')
    expect(JSON.parse(opts.body)).toEqual({ expression: '3 + 5' })
  })

  it('returns the parsed CompileResult', async () => {
    mockFetch(mockResult)
    const res = await compileExpression('3 + 5')
    expect(res.result).toBe(8)
    expect(res.ok).toBe(true)
    expect(res.tokens).toHaveLength(3)
  })

  it('throws when server returns non-2xx', async () => {
    mockFetch({ message: 'Server Error' }, 500)
    await expect(compileExpression('bad')).rejects.toThrow()
  })
})

// ── getVars ───────────────────────────────────────────────────────────

describe('getVars()', () => {
  it('calls GET /vars', async () => {
    mockFetch({ x: 42, pi: 3.14 })
    await getVars()
    expect(fetch).toHaveBeenCalledOnce()
    const [url, opts] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(url).toMatch(/\/vars$/)
    expect(opts?.method ?? 'GET').toBe('GET')
  })

  it('returns key-value map of variables', async () => {
    mockFetch({ x: 42 })
    const v = await getVars()
    expect(v).toEqual({ x: 42 })
  })

  it('returns empty object when no vars', async () => {
    mockFetch({})
    const v = await getVars()
    expect(Object.keys(v)).toHaveLength(0)
  })
})

// ── clearVars ─────────────────────────────────────────────────────────

describe('clearVars()', () => {
  it('calls POST /vars/clear', async () => {
    mockFetch({})
    await clearVars()
    const [url, opts] = (fetch as ReturnType<typeof vi.fn>).mock.calls[0]
    expect(url).toMatch(/\/vars\/clear$/)
    expect(opts.method).toBe('POST')
  })
})

// ── healthCheck ───────────────────────────────────────────────────────

describe('healthCheck()', () => {
  it('returns true when server is up', async () => {
    mockFetch({ status: 'ok' })
    expect(await healthCheck()).toBe(true)
  })

  it('returns false when fetch throws (server down)', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('connect ECONNREFUSED')))
    expect(await healthCheck()).toBe(false)
  })
})

// ── CompileResult shape ───────────────────────────────────────────────

describe('CompileResult interface', () => {
  it('maps all expected fields', async () => {
    mockFetch(mockResult)
    const r = await compileExpression('3 + 5')
    expect(r).toMatchObject({
      input:        expect.any(String),
      tokens:       expect.any(Array),
      ast:          expect.any(Array),
      tac:          expect.any(Array),
      optimisedTac: expect.any(Array),
      warnings:     expect.any(Array),
      runtimeErrors:expect.any(Array),
      result:       expect.any(Number),
      ok:           expect.any(Boolean),
    })
  })

  it('error result has ok=false and runtimeErrors', async () => {
    const bad: CompileResult = { ...mockResult, ok: false, runtimeErrors: ['Division by zero'] }
    mockFetch(bad)
    const r = await compileExpression('10 / 0')
    expect(r.ok).toBe(false)
    expect(r.runtimeErrors.length).toBeGreaterThan(0)
  })
})
