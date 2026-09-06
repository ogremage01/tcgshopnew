import {
  isValidPhoneNumber as libIsValidPhoneNumber,
  type CountryCode,
} from "libphonenumber-js";

/** 국내(기본 KR)·국제(+E.164) 전화번호 유효성 검사 */
export function isValidPhoneNumber(
  phone: string,
  defaultCountry: CountryCode = "KR",
): boolean {
  const trimmed = phone.trim();
  if (!trimmed) return false;
  try {
    return libIsValidPhoneNumber(trimmed, defaultCountry);
  } catch {
    return false;
  }
}

/**
 * 토스페이먼츠 customerMobilePhone용 정규화.
 * `-` 등 특수문자 없이 숫자만, 8~15자. 조건을 못 맞추면 undefined.
 */
export function toTossMobilePhone(phone: string | undefined | null): string | undefined {
  if (!phone) return undefined;
  const digits = phone.replace(/\D/g, "");
  if (digits.length < 8 || digits.length > 15) return undefined;
  return digits;
}
