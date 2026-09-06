import { getServerApiBaseUrl } from "@/lib/server-api";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

/**
 * admin SSE 스트리밍 프록시.
 * same-origin으로 연결해 CORS 없이 백엔드 text/event-stream을 pipe한다.
 */
export async function GET(
  request: Request,
  context: { params: Promise<{ connectionId: string }> },
) {
  const { connectionId } = await context.params;
  const backendUrl = `${getServerApiBaseUrl()}/api/admin/alarm/subscribe/${connectionId}`;
  const authorization = request.headers.get("authorization");

  let backendResponse: Response;
  try {
    backendResponse = await fetch(backendUrl, {
      headers: {
        ...(authorization ? { Authorization: authorization } : {}),
        Accept: "text/event-stream",
      },
      cache: "no-store",
      signal: request.signal,
    });
  } catch (error) {
    console.error("SSE proxy: backend unreachable", { backendUrl, error });
    return new Response(null, { status: 502 });
  }

  if (!backendResponse.ok || !backendResponse.body) {
    return new Response(null, { status: backendResponse.status });
  }

  return new Response(backendResponse.body, {
    status: backendResponse.status,
    headers: {
      "Content-Type":
        backendResponse.headers.get("Content-Type") ??
        "text/event-stream; charset=utf-8",
      "Cache-Control": "no-cache, no-transform",
      "X-Accel-Buffering": "no",
    },
  });
}
