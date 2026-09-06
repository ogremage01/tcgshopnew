"use client";

import { useShallow } from "zustand/react/shallow";
import {
  useAdminAlarmSseStore,
  type AdminAlarmSseStatus,
} from "@/stores/admin-alarm-sse-store";

function formatRelativeTime(timestamp: number | null): string {
  if (timestamp == null) return "-";
  const seconds = Math.floor((Date.now() - timestamp) / 1000);
  if (seconds < 5) return "방금";
  if (seconds < 60) return `${seconds}초 전`;
  const minutes = Math.floor(seconds / 60);
  return `${minutes}분 전`;
}

const STATUS_LABEL: Record<AdminAlarmSseStatus, string> = {
  idle: "대기",
  connecting: "연결 중",
  connected: "연결됨",
  reconnecting: "재연결 중",
  error: "오류",
};

const STATUS_COLOR: Record<AdminAlarmSseStatus, string> = {
  idle: "bg-gray-400",
  connecting: "bg-yellow-400 animate-pulse",
  connected: "bg-green-400",
  reconnecting: "bg-yellow-400 animate-pulse",
  error: "bg-red-500",
};

/**
 * admin SSE 연결 상태 표시.
 * heartbeat(30초) 수신 여부로 실제 스트림 생존을 확인합니다.
 */
export default function AdminAlarmConnectionStatus() {
  const { status, connectionId, lastHeartbeatAt, lastEventAt, alarmCount, errorMessage } =
    useAdminAlarmSseStore(
      useShallow((s) => ({
        status: s.status,
        connectionId: s.connectionId,
        lastHeartbeatAt: s.lastHeartbeatAt,
        lastEventAt: s.lastEventAt,
        alarmCount: s.alarmCount,
        errorMessage: s.errorMessage,
      })),
    );

  const title = [
    `SSE ${STATUS_LABEL[status]}`,
    connectionId != null ? `ID: ${connectionId}` : null,
    `마지막 이벤트: ${formatRelativeTime(lastEventAt)}`,
    lastHeartbeatAt != null
      ? `heartbeat: ${formatRelativeTime(lastHeartbeatAt)}`
      : "heartbeat: 없음",
    `수신 알림: ${alarmCount}건`,
    errorMessage ? `오류: ${errorMessage}` : null,
  ]
    .filter(Boolean)
    .join("\n");

  return (
    <div
      className="flex items-center gap-1.5 rounded-md bg-white/15 px-2 py-1 text-xs text-white shrink-0"
      title={title}
    >
      <span
        className={`h-2 w-2 rounded-full shrink-0 ${STATUS_COLOR[status]}`}
        aria-hidden
      />
      <span className="hidden sm:inline">SSE {STATUS_LABEL[status]}</span>
      {status === "connected" && lastHeartbeatAt != null && (
        <span className="hidden md:inline text-white/80">
          · hb {formatRelativeTime(lastHeartbeatAt)}
        </span>
      )}
    </div>
  );
}
