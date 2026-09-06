import { useState, useEffect, useCallback } from "react"
import { apiClient } from "@/lib/api"
import type { SyncLogDto } from "@/types/syncLog"
import type {
    GradePricingPolicyDto,
    PriceConfigDto,
    StorageDto,
    TcgPSyncGameDto,
} from "@/types/product"

// 이 파일은 상품 설정 페이지 초기 로딩용 집계 응답 데이터를 관리하는 훅입니다.

// 이 타입은 상품 설정 페이지 초기 로딩용 집계 응답 타입입니다.
export type ProductConfigBootstrap = {
    sync: {
        games: TcgPSyncGameDto[]
        lastTime: {
            lastPriceSyncTime: SyncLogDto | null
            lastMetadataSyncTime: SyncLogDto | null
            lastOpenBinderSyncTime: SyncLogDto | null
            lastImageDownloadSyncTime: SyncLogDto | null
            lastPriceLinkOverwriteSyncTime: SyncLogDto | null
        }
    }
    storage: StorageDto[]
    price: {
        /** 게임별 최소 가격 목록 (configGame = GameEnum.game 풀네임) */
        minimumPrices: PriceConfigDto[]
        grade: GradePricingPolicyDto[]
        /** 게임별 환율 목록 (configGame = GameEnum.game 풀네임) */
        currencyRates: PriceConfigDto[]
    }
}

// 상품 설정 페이지 초기 로딩용 집계 응답 데이터를 조회하는 함수입니다.
export async function fetchProductConfigBootstrap(): Promise<ProductConfigBootstrap> {
    const { data } = await apiClient.get<ProductConfigBootstrap>(`/api/admin/product/metadata/config/bootstrap`)
    return data
}

// 상품 설정 페이지 초기 로딩용 집계 응답 데이터를 관리하는 훅입니다.
export function useProductConfigBootstrap() {
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState<Error | null>(null)
    const [data, setData] = useState<ProductConfigBootstrap | null>(null)

    const load = useCallback((withPendingState = true) => {
        if (withPendingState) {
            setLoading(true)
            setError(null)
        }
        return fetchProductConfigBootstrap()
            .then((d) => {
                setData(d)
            })
            .catch((e: unknown) => {
                setData(null)
                setError(e instanceof Error ? e : new Error(String(e)))
            })
            .finally(() => {
                setLoading(false)
            })
    }, [])

    useEffect(() => {
        let active = true
        void fetchProductConfigBootstrap()
            .then((d) => {
                if (!active) return
                setData(d)
                setError(null)
            })
            .catch((e: unknown) => {
                if (!active) return
                setData(null)
                setError(e instanceof Error ? e : new Error(String(e)))
            })
            .finally(() => {
                if (!active) return
                setLoading(false)
            })

        return () => {
            active = false
        }
    }, [])

    return { loading, error, data, refetch: load }
}
