import { create } from "zustand";

export interface User {
  id: string;
  email: string;
  name: string;
  role: string;
  point: number;
}

interface AuthStore {
  token: string | null;
  user: User | null;
  setAuth: (token: string, user: User) => void;
  clear: () => void;
  logout: (options?: { onComplete?: () => void }) => void;
  isAuthenticated: boolean;
  isOnTokenRefresh: boolean;
  /** refresh 쿠키 복원 시도 완료 여부 (새 탭에서 로그인 오판 방지) */
  sessionChecked: boolean;
  markSessionChecked: () => void;
}

export const useAuthStore = create<AuthStore>()(
  (set) => ({
    token: null,
    user: null,
    isAuthenticated: false,
    isOnTokenRefresh: false,
    sessionChecked: false,
    markSessionChecked: () => set({ sessionChecked: true }),
    setAuth: (token, user) =>
      set({ token, user, isAuthenticated: true, sessionChecked: true }),
    clear: () =>
      set({ token: null, user: null, isAuthenticated: false, sessionChecked: true }),
    logout: (options) => {
      // refresh 쿠키 무효화가 끝나기 전에 clear()만 하면 AuthSessionRestorer가
      // /api/auth/refresh로 세션을 다시 채울 수 있으므로, 서버 로그아웃 완료 후 clear.
      void (async () => {
        try {
          const baseURL = process.env.NEXT_PUBLIC_API_URL ?? "";
          const url = `${baseURL}/api/auth/logout`;
          await fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({}),
          });
        } catch {
          // ignore
        } finally {
          useAuthStore.getState().clear();
          options?.onComplete?.();
        }
      })();
    },
  })
);
