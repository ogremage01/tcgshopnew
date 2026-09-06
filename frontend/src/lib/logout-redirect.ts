/** 로그아웃 후 메인으로 보낼 경로 (next/navigation pathname, locale 포함) */
const LOGOUT_REDIRECT_HOME_PATH = /\/(mypage|cart)(\/|$)/;

export function shouldRedirectHomeAfterLogout(pathname: string): boolean {
  return LOGOUT_REDIRECT_HOME_PATH.test(pathname);
}
