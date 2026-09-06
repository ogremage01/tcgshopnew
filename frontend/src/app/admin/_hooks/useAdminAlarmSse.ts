"use client";

import { useEffect, useRef } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/stores/auth-store";
import { useAdminAlarmSseStore } from "@/stores/admin-alarm-sse-store";
import { ADMIN_ROLE } from "@/lib/admin-auth";
import { buildAdminAlarmSubscribeUrl } from "@/lib/admin-alarm-sse-url";
import { parseAlarmData, parseSseChunk } from "@/lib/admin-sse-parse";
import { showOrderAlarm } from "@/lib/admin-alarm-notify";

const RECONNECT_DELAY_MS = 3_000;

/**
 * admin SSE 구독 hook.
 * layout에서 mount되면 서버와 text/event-stream 연결을 유지하고,
 * alarm 이벤트 수신 시 toast 또는 OS 알림을 띄웁니다.
 */
export function useAdminAlarmSse() {
  const router = useRouter();
  const routerRef = useRef(router);
  routerRef.current = router;

  const token = useAuthStore((s) => s.token);
  const sessionChecked = useAuthStore((s) => s.sessionChecked);
  const role = useAuthStore((s) => s.user?.role);

  useEffect(() => {
    const {
      setConnecting,
      setConnected,
      setHeartbeat,
      setAlarmReceived,
      setReconnecting,
      setError,
      reset,
    } = useAdminAlarmSseStore.getState();

    if (!sessionChecked || role !== ADMIN_ROLE || !token) {
      reset();
      return;
    }

    let stopped = false;
    let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
    let abortController: AbortController | null = null;

    const scheduleReconnect = () => {
      if (stopped) return;
      setReconnecting();
      reconnectTimer = setTimeout(() => {
        void connect();
      }, RECONNECT_DELAY_MS);
    };

    const connect = async () => {
      if (stopped) return;

      abortController?.abort();
      abortController = new AbortController();

      const connectionId = Date.now();
      const url = buildAdminAlarmSubscribeUrl(connectionId);
      setConnecting(connectionId);

      try {
        const response = await fetch(url, {
          headers: {
            Authorization: `Bearer ${token}`,
            Accept: "text/event-stream",
          },
          signal: abortController.signal,
          credentials: "include",
        });

        if (!response.ok || !response.body) {
          const message = `구독 실패 (${response.status})`;
          console.error("SSE subscribe failed", { url, status: response.status });
          setError(message);
          scheduleReconnect();
          return;
        }

        setConnected();

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let buffer = "";

        while (!stopped) {
          const { done, value } = await reader.read();
          if (done) break;

          buffer += decoder.decode(value, { stream: true });
          buffer = buffer.replace(/\r\n/g, "\n");

          const chunks = buffer.split("\n\n");
          buffer = chunks.pop() ?? "";

          for (const chunk of chunks) {
            if (!chunk.trim()) continue;

            const { eventName, data } = parseSseChunk(chunk);

            if (eventName === "heartbeat") {
              setHeartbeat();
              continue;
            }

            if (eventName === "alarm") {
              const alarm = parseAlarmData(data);
              if (!alarm) {
                console.warn("SSE alarm parse failed", data);
                continue;
              }

              setAlarmReceived();
              showOrderAlarm(alarm, (path) => routerRef.current.push(path));
            }
          }
        }

        if (!stopped) {
          console.warn("admin alarm SSE stream closed, reconnecting...", {
            connectionId,
          });
          scheduleReconnect();
        }
      } catch (err) {
        if (stopped || abortController.signal.aborted) return;
        const message = err instanceof Error ? err.message : "연결 오류";
        console.error("SSE fetch failed", { url, err });
        setError(message);
        scheduleReconnect();
      }
    };

    void connect();

    return () => {
      stopped = true;
      if (reconnectTimer) clearTimeout(reconnectTimer);
      abortController?.abort();
      reset();
    };
  }, [sessionChecked, role, token]);
}
