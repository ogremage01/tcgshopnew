"use client";

import { useEffect } from "react";

/** BFCache 복원 시 전체 reload 대신 앱 전역 이벤트로 데이터 재동기화를 유도한다. */
export const BFCACHE_RESTORE_EVENT = "app:bfcache-restore";

export default function DisableBFCache() {
  useEffect(() => {
    const handler = (e: PageTransitionEvent) => {
      if (e.persisted) {
        window.dispatchEvent(new Event(BFCACHE_RESTORE_EVENT));
      }
    };

    window.addEventListener("pageshow", handler);
    return () => {
      window.removeEventListener("pageshow", handler);
    };
  }, []);

  return null;
}
