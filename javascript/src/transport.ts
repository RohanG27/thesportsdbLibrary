/** A plain HTTP response: what the library needs from a transport. */
export interface HttpResponse {
  readonly status: number;
  readonly body: string;
  readonly headers: Readonly<Record<string, string>>;
}

/**
 * Performs GET requests. Implement it to use another HTTP stack or to fake the network in tests.
 * Reject (throw) when no response arrives; resolve every HTTP status, including 4xx and 5xx.
 */
export interface Transport {
  get(url: string, headers: Readonly<Record<string, string>>): Promise<HttpResponse>;
}

/** The default transport, on the global fetch (Node 18+, Deno, Bun, browsers). */
export function fetchTransport(timeoutMs = 30_000, fetchImpl: typeof fetch = globalThis.fetch): Transport {
  return {
    async get(url, headers) {
      const response = await fetchImpl(url, { headers, signal: AbortSignal.timeout(timeoutMs) });
      const out: Record<string, string> = {};
      response.headers.forEach((value, key) => {
        out[key] = value;
      });
      return { status: response.status, body: await response.text(), headers: out };
    },
  };
}
