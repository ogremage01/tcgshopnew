const JUSO_API_URL = {
  ko: "https://business.juso.go.kr/addrlink/addrLinkApi.do",
  en: "https://business.juso.go.kr/addrlink/addrEngApi.do",
} as const;

export function getPostalCodeApiConfig(locale: string) {
  const isKo = locale === "ko";
  return {
    apiUrl: isKo ? JUSO_API_URL.ko : JUSO_API_URL.en,
    confmKey: isKo
      ? process.env.POSTAL_CODE_SERVICE_API_KO_KEY
      : process.env.POSTAL_CODE_SERVICE_API_EN_KEY,
  };
}
