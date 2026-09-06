/** 로케일 prefix(ko|en) 다음에 오는 비공개 경로 — Googlebot `*` 와일드카드 지원 */
export const ROBOTS_DISALLOW_PATHS = [
  "/admin",
  "/*/cart",
  "/*/cart/checkout",
  "/*/login",
  "/*/register",
  "/*/mypage",
  "/*/guest-order-check",
] as const;
