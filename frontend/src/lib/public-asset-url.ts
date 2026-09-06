/** 백엔드 `file.path.card-images` 루트에 두는 플레이스홀더 (예: d:/shop/card-images/default-card.png) */
export const DEFAULT_CARD_IMAGE_PATH = "card-images/default-card.png"

export function defaultCardImageUrl(): string {
  return publicAssetUrl(DEFAULT_CARD_IMAGE_PATH)
}

/** 자산 베이스 URL: ASSET_BASE → API_URL → 동일 오리진(빈 문자열) */
function assetBaseUrl(): string {
  const assetBase = (process.env.NEXT_PUBLIC_ASSET_BASE_URL ?? "").replace(/\/$/, "")
  if (assetBase) return assetBase
  return (process.env.NEXT_PUBLIC_API_URL ?? "").replace(/\/$/, "")
}

/**
 * API가 돌려주는 백엔드 상대 정적 경로(예: uploads/..., card-images/...)를
 * 브라우저가 요청할 수 있는 절대/상대 URL로 만듭니다.
 *
 * - `NEXT_PUBLIC_ASSET_BASE_URL` (또는 하위 호환 `NEXT_PUBLIC_API_URL`)이 없으면
 *   동일 오리진(예: /uploads, /card-images) + Next rewrites
 * - 있으면 해당 오리진에 붙임
 */
export function publicAssetUrl(relativePath: string): string {
  const trimmed = String(relativePath).replace(/^\/+/, "")
  if (!trimmed) return ""
  const base = assetBaseUrl()
  if (base) return `${base}/${trimmed}`
  return `/${trimmed}`
}

/**
 * API·DB에서 오는 이미지/정적 자산 URL 정규화:
 * - 절대 URL(https, //, data:)이면 그대로 사용
 * - 상대 경로면 publicAssetUrl로 보정
 */
export function resolveAssetUrl(url?: string): string {
  const raw = String(url ?? "").trim()
  if (!raw) return ""
  if (/^https?:\/\//i.test(raw)) return raw
  // 프로토콜 상대 URL(//host/...) — publicAssetUrl이 앞 슬래시를 제거해 베이스 URL이 잘못 붙는 것을 막음
  if (raw.startsWith("//")) return raw
  if (raw.startsWith("data:")) return raw
  return publicAssetUrl(raw)
}

/** URL이 없거나 로드 실패 시 사용할 최종 src (card-images/default-card.png) */
export function resolveProductImageSrc(imageUrl?: string | null): string {
  return resolveAssetUrl(imageUrl ?? undefined) || defaultCardImageUrl()
}

/** img onError — 무한 루프 방지 후 default-card.png로 교체 */
export function productImageOnError(e: { currentTarget: HTMLImageElement }): void {
  const img = e.currentTarget
  if (img.dataset.productImageFallback === "1") return
  img.dataset.productImageFallback = "1"
  img.src = defaultCardImageUrl()
}
