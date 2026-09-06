export { useAdminProductSync } from "./useAdminProductSync"
export type { UseAdminProductSyncInitialData } from "./useAdminProductSync"
export { useAdminProductPriceConfig } from "./useAdminProductPriceConfig"
export { useAdminProductStorage } from "./useAdminProductStorage"

import { useAdminProductSync } from "./useAdminProductSync"
import { useAdminProductPriceConfig } from "./useAdminProductPriceConfig"
import { useAdminProductStorage } from "./useAdminProductStorage"
import type { ProductConfigBootstrap } from "@/app/admin/products/config/_hooks/useProductConfigBootstrap"

/**
 * 설정 페이지 전체 상태 (도메인 훅을 합친 것)
 * @param bootstrap — 부가하면 마운트 시 읽기 API를 생략
 */
export function useAdminProductConfig(bootstrap?: ProductConfigBootstrap) {
    const sync = useAdminProductSync(
        bootstrap
            ? { initialData: { games: bootstrap.sync.games, lastTime: bootstrap.sync.lastTime } }
            : undefined
    )
    const price = useAdminProductPriceConfig(
        bootstrap
            ? { initialData: { minimumPrices: bootstrap.price.minimumPrices, grade: bootstrap.price.grade, currencyRates: bootstrap.price.currencyRates } }
            : undefined
    )
    const storage = useAdminProductStorage(
        bootstrap ? { initialList: bootstrap.storage } : undefined
    )
    return { ...sync, ...price, ...storage }
}
