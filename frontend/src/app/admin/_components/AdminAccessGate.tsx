"use client";

import type { ReactNode } from "react";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import AuthSessionRestorer from "@/app/_components/AuthSessionRestorer";
import { ADMIN_ROLE } from "@/lib/admin-auth";
import { useAuthStore } from "@/stores/auth-store";

const FORBIDDEN_PATH = "/forbidden";

export default function AdminAccessGate({ children }: Readonly<{ children: ReactNode }>) {
  const router = useRouter();
  const sessionChecked = useAuthStore((s) => s.sessionChecked);
  const userRole = useAuthStore((s) => s.user?.role);
  const isAllowed = userRole === ADMIN_ROLE;

  useEffect(() => {
    if (!sessionChecked || isAllowed) {
      return;
    }

    router.replace(FORBIDDEN_PATH);
  }, [isAllowed, router, sessionChecked]);

  if (!sessionChecked || !isAllowed) {
    return <AuthSessionRestorer />;
  }

  return (
    <>
      <AuthSessionRestorer />
      {children}
    </>
  );
}
