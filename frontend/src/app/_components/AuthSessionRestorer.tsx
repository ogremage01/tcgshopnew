"use client";

import { useEffect } from "react";
import { api } from "@/lib/api";
import { useAuthStore } from "@/stores/auth-store";
import type { AuthResponseDto } from "@/types/user";
import { userFromAuthDto } from "@/types/user";
import { usePathname } from "next/navigation";

export default function AuthSessionRestorer() {
  const token = useAuthStore((s) => s.token);
  const setAuth = useAuthStore((s) => s.setAuth);
  const clear = useAuthStore((s) => s.clear);
  const markSessionChecked = useAuthStore((s) => s.markSessionChecked);
  const pathname = usePathname() ?? "";

  useEffect(() => {
    if (token) {
      markSessionChecked();
      return;
    }
    // 인증 페이지에서는 refresh 재시도로 이동 루프가 생기지 않게 스킵
    if (pathname.endsWith("/login") || pathname.endsWith("/register")) {
      markSessionChecked();
      return;
    }

    api
      .post<AuthResponseDto>("/api/auth/refresh", {})
      .then((result) => {
        if (!result?.token || !result?.user) {
          clear();
          return;
        }

        setAuth(result.token, userFromAuthDto(result.user));
      })
      .catch(() => {
        clear();
      })
      .finally(() => {
        markSessionChecked();
      });
  }, [token, setAuth, clear, markSessionChecked, pathname]);

  return null;
}

