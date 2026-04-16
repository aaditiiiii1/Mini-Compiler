const API_BASE = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export interface CompileResult {
  input: string;
  tokens: string[];        // "TYPE:value"
  ast: string[];           // tree lines
  astJson: string;
  tac: string[];
  optimisedTac: string[];
  warnings: string[];
  runtimeErrors: string[];
  result: number;
  ok: boolean;
}

export async function compileExpression(expression: string): Promise<CompileResult> {
  const res = await fetch(`${API_BASE}/compile`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ expression }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({ error: res.statusText }));
    throw new Error(err.error ?? "Server error");
  }
  return res.json();
}

export async function getVars(): Promise<Record<string, number>> {
  const res = await fetch(`${API_BASE}/vars`);
  return res.json();
}

export async function clearVars(): Promise<void> {
  await fetch(`${API_BASE}/vars/clear`, { method: "POST" });
}

export async function healthCheck(): Promise<boolean> {
  try {
    const res = await fetch(`${API_BASE}/health`);
    return res.ok;
  } catch {
    return false;
  }
}
