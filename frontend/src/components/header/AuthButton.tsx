"use client";

import { useTranslations } from "next-intl";
import { usePathname } from "next/navigation";
import { useAuthStore } from "@/stores/auth-store";
import { Button } from "@/components/ui/button";
import { useRouter } from "@/i18n/navigation";
import { shouldRedirectHomeAfterLogout } from "@/lib/logout-redirect";

export default function AuthButton() {
  const t = useTranslations();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);
  const logout = useAuthStore((s) => s.logout);
  const router = useRouter();
  const pathname = usePathname() ?? "";
  const login = () => {
    router.push("/login");
  };

  const handleLogout = () => {
    const redirectHome = shouldRedirectHomeAfterLogout(pathname);
    logout({
      onComplete: () => {
        if (redirectHome) {
          router.push("/");
        }
      },
    });
  };

  return (
    <div className="flex items-center gap-2">
      {isAuthenticated ? (
        <Button variant="outline" size="sm" onClick={handleLogout}>
          {t("nav.logout")}
        </Button>
      ) : (
        <Button variant="outline" size="sm" onClick={login}>
          {t("nav.login")}
        </Button>
      )}
    </div>
  );
}
