import type { useTranslations } from "next-intl";

type Translate = ReturnType<typeof useTranslations>;

/**
 * 백엔드 `messageCode`(점으로 구분된 키) 또는 `message` 문자열을
 * 현재 로케일의 `common.json` 문구로 바꿉니다.
 * - `messageCode`가 번역 키로 존재하면 `t(messageCode)` 사용
 * - 그 외 짧은 원문 `message`는 그대로 표시
 * - 없으면 `fallbackKey` (예: register.error)
 */
export function translateApiErrorForUser(
  t: Translate,
  keyOrMsg: string | undefined,
  fallbackKey: string,
): string {
  if (keyOrMsg == null || keyOrMsg === "") {
    return t(fallbackKey);
  }
  const withHas = t as unknown as { has?: (key: string) => boolean };
  if (typeof withHas.has === "function" && withHas.has(keyOrMsg)) {
    return t(keyOrMsg as Parameters<Translate>[0]);
  }
  if (!keyOrMsg.includes("error.")) {
    return keyOrMsg;
  }
  return t(fallbackKey);
}
