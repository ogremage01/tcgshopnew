"use client";

import { useEffect, type ReactNode } from "react";
import { useRouter } from "@/i18n/navigation";
import { useAuthStore } from "@/stores/auth-store";

export default function MypageLayout({ children }: Readonly<{ children: ReactNode }>) {
  const router = useRouter();
  const sessionChecked = useAuthStore((s) => s.sessionChecked);
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  useEffect(() => {
    if (!sessionChecked) return;
    if (!useAuthStore.getState().isAuthenticated) {
      router.replace("/login");
    }
  }, [sessionChecked, router]);

  if (!sessionChecked || !isAuthenticated) {
    return null;
  }

  return children;
}
