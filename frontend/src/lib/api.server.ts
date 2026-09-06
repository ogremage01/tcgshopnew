import { cookies } from "next/headers";
import { getServerApiBaseUrl } from "@/lib/server-api";

type ServerApiInit = {
  headers?: HeadersInit;
  cache?: RequestCache;
  revalidate?: number | false;
  tags?: string[];
};

const baseURL = getServerApiBaseUrl().replace(/\/$/, "");

function makeUrl(path: string): string {
  if (/^https?:\/\//.test(path)) return path;
  return `${baseURL}${path}`;
}

async function request<T>(
  path: string,
  init?: RequestInit & ServerApiInit,
  options?: { withAuth?: boolean },
): Promise<T> {
  const { revalidate, tags, headers, ...rest } = init ?? {};
  const withAuth = options?.withAuth ?? true;
  let token: string | undefined;
  if (withAuth) {
    const cookieStore = await cookies();
    token = cookieStore.get("access-token")?.value;
  }
  const res = await fetch(makeUrl(path), {
    ...rest,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(headers ?? {}),
    },
    next: {
      revalidate,
      tags,
    },
  });

  if (!res.ok) {
    const text = await res.text().catch(() => "");
    throw new Error(text || `Request failed with status ${res.status}`);
  }

  if (res.status === 204) return undefined as T;

  const contentType = res.headers.get("content-type") ?? "";
  const bodyText = await res.text();
  if (!bodyText.trim()) return undefined as T;

  if (contentType.includes("application/json")) {
    return JSON.parse(bodyText) as T;
  }

  try {
    return JSON.parse(bodyText) as T;
  } catch {
    throw new Error(
      `Expected JSON response but received "${contentType || "unknown"}" from ${path}`
    );
  }
}

/** 서버 컴포넌트 전용 fetch 래퍼 */
export const serverApi = {
  get: <T>(path: string, init?: ServerApiInit): Promise<T> =>
    request<T>(path, { method: "GET", ...init }),
  post: <T>(path: string, body: unknown, init?: ServerApiInit): Promise<T> =>
    request<T>(path, {
      method: "POST",
      body: JSON.stringify(body),
      ...init,
    }),
};

/** unstable_cache 등 동적 컨텍스트 밖에서 호출하는 공개 API용 fetch */
export const serverApiPublic = {
  get: <T>(path: string, init?: ServerApiInit): Promise<T> =>
    request<T>(path, { method: "GET", ...init }, { withAuth: false }),
};
