import { toast } from "sonner";
import type { Alarm } from "@/types/alarm";

/** 백엔드 link 값을 admin 라우트 경로로 맞춥니다. */
export function normalizeAdminLink(link: string): string {
  if (!link) return "/admin/order/list";
  if (link.startsWith("/")) return link;
  if (link.startsWith("admin/")) return `/${link}`;
  return `/admin/${link}`;
}

/** 이 브라우저가 OS 알림(Notification API)을 지원하는지 확인합니다. */
export function isNotificationSupported(): boolean {
  return typeof window !== "undefined" && "Notification" in window;
}

/** 현재 알림 권한 상태. 지원하지 않으면 "unsupported". */
export function getNotificationPermission():
  | NotificationPermission
  | "unsupported" {
  if (!isNotificationSupported()) return "unsupported";
  return Notification.permission;
}

/**
 * OS 알림 권한을 요청합니다.
 * 브라우저 정책상 사용자 클릭(버튼) 안에서 호출하는 것이 안전합니다.
 */
export async function requestNotificationPermission(): Promise<boolean> {
  if (!isNotificationSupported()) return false;
  if (Notification.permission === "granted") return true;
  if (Notification.permission === "denied") return false;

  const result = await Notification.requestPermission();
  return result === "granted";
}

type NavigateHandler = (path: string) => void;

/**
 * SSE alarm 수신 시 화면 알림을 표시합니다.
 *
 * - 탭을 보고 있을 때: sonner toast
 * - 탭이 백그라운드 + OS 알림 권한 있음: 윈도우(데스크톱) 알림
 * - 백그라운드인데 권한 없음: toast (돌아왔을 때 확인 가능)
 */
export function showOrderAlarm(alarm: Alarm, onNavigate: NavigateHandler): void {
  const path = normalizeAdminLink(alarm.link);

  const showToast = () => {
    toast(alarm.title, {
      description: alarm.content,
      action: {
        label: "보기",
        onClick: () => onNavigate(path),
      },
    });
  };

  // 탭을 보고 있을 때는 toast, 백그라운드+권한 있으면 OS 알림도 추가
  showToast();

  if (
    typeof document !== "undefined" &&
    document.hidden &&
    isNotificationSupported() &&
    Notification.permission === "granted"
  ) {
    const notification = new Notification(alarm.title, {
      body: alarm.content,
      icon: "/favicon.svg",
      // 같은 주문 알림이 여러 번 뜨지 않도록 tag로 묶음
      tag: path,
    });

    notification.onclick = () => {
      window.focus();
      onNavigate(path);
      notification.close();
    };
  }
}
