import { defineRouting } from "next-intl/routing";

// 라우팅 설정
export const routing = defineRouting({
  // 지원 로케일
  locales: ["ko", "en"],
  // 기본 로케일
  defaultLocale: "en",
  // 로케일 프리픽스
  localePrefix: "always",
  // 로케일 검색 비활성화
  localeDetection: false,
});
