"use client";

import { useCallback, useEffect, useState } from "react";
import { Bell, BellOff } from "lucide-react";
import { Button } from "@/components/ui/button";
import { toast } from "sonner";
import {
  getNotificationPermission,
  isNotificationSupported,
  requestNotificationPermission,
} from "@/lib/admin-alarm-notify";

/**
 * admin 헤더용 OS 알림 권한 버튼.
 * SSE는 연결만으로는 윈도우 알림이 안 뜨므로, 사용자가 한 번 허용해야 합니다.
 */
export default function AdminNotificationPrompt() {
  const [permission, setPermission] = useState<
    NotificationPermission | "unsupported" | "loading"
  >("loading");

  useEffect(() => {
    setPermission(getNotificationPermission());
  }, []);

  const handleEnable = useCallback(async () => {
    if (!isNotificationSupported()) {
      toast.error("이 브라우저는 OS 알림을 지원하지 않습니다.");
      return;
    }

    if (Notification.permission === "denied") {
      toast.error(
        "알림이 차단되어 있습니다. 브라우저 주소창 옆 설정에서 허용해 주세요.",
      );
      return;
    }

    const granted = await requestNotificationPermission();
    setPermission(getNotificationPermission());

    if (granted) {
      toast.success("새 주문 OS 알림이 켜졌습니다.");
      return;
    }

    toast.message("OS 알림 권한이 허용되지 않았습니다. toast 알림은 계속 표시됩니다.");
  }, []);

  if (permission === "loading" || permission === "unsupported") {
    return null;
  }

  if (permission === "granted") {
    return (
      <Button
        type="button"
        variant="secondary"
        size="sm"
        className="shrink-0 gap-1.5"
        disabled
        title="OS 알림이 켜져 있습니다"
      >
        <Bell className="h-4 w-4" />
        <span className="hidden sm:inline">알림 ON</span>
      </Button>
    );
  }

  return (
    <Button
      type="button"
      variant="secondary"
      size="sm"
      className="shrink-0 gap-1.5"
      onClick={() => void handleEnable()}
      title="새 주문을 윈도우 알림으로 받으려면 클릭"
    >
      <BellOff className="h-4 w-4" />
      <span className="hidden sm:inline">알림 켜기</span>
    </Button>
  );
}
