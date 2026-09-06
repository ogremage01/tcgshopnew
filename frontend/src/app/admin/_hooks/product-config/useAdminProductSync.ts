import { useState, useEffect, useCallback } from "react"
import { apiClient } from "@/lib/api"
import { AxiosError } from "axios"
import { SyncLogDto } from "@/types/syncLog"
import { TcgPSyncGameDto } from "@/types/product"

export type UseAdminProductSyncInitialData = {
    games: TcgPSyncGameDto[]
    lastTime: {
        lastPriceSyncTime: SyncLogDto | null
        lastMetadataSyncTime: SyncLogDto | null
        lastOpenBinderSyncTime: SyncLogDto | null
        lastImageDownloadSyncTime: SyncLogDto | null
        lastPriceLinkOverwriteSyncTime: SyncLogDto | null
    }
}

/**
 * TCGPlayer 동기화: 게임 목록, 마지막 동기 시각, 동기화 로그·액션
 * @param options.initialData — 설정 시 마운트 시 GET 생략 (부트스트랩과 함께 사용)
 */
export function useAdminProductSync(options?: { initialData?: UseAdminProductSyncInitialData }) {
    const init = options?.initialData
    const [syncGameList, setSyncGameList] = useState<TcgPSyncGameDto[]>(init?.games ?? [])
    const [lastPriceSyncTime, setLastPriceSyncTime] = useState<SyncLogDto | null>(init?.lastTime.lastPriceSyncTime ?? null)
    const [lastMetadataSyncTime, setLastMetadataSyncTime] = useState<SyncLogDto | null>(init?.lastTime.lastMetadataSyncTime ?? null)
    const [lastOpenBinderSyncTime, setLastOpenBinderSyncTime] = useState<SyncLogDto | null>(init?.lastTime.lastOpenBinderSyncTime ?? null)
    const [lastImageDownloadSyncTime, setLastImageDownloadSyncTime] = useState<SyncLogDto | null>(init?.lastTime.lastImageDownloadSyncTime ?? null)
    const [lastPriceLinkOverwriteSyncTime, setLastPriceLinkOverwriteSyncTime] = useState<SyncLogDto | null>(init?.lastTime.lastPriceLinkOverwriteSyncTime ?? null)
    const [loadingSyncGames, setLoadingSyncGames] = useState(!init)
    const [loadingLastSync, setLoadingLastSync] = useState(!init)

    const loadLastSyncTime = useCallback(() => {
        return apiClient.get(`/api/admin/product/metadata/sync/last-time`).then((response) => {
            const body = response.data as {
                lastPriceSyncTime: SyncLogDto | null
                lastMetadataSyncTime: SyncLogDto | null
                lastOpenBinderSyncTime: SyncLogDto | null
                lastImageDownloadSyncTime: SyncLogDto | null
                lastPriceLinkOverwriteSyncTime: SyncLogDto | null
            }
            setLastPriceSyncTime(body.lastPriceSyncTime)
            setLastMetadataSyncTime(body.lastMetadataSyncTime)
            setLastOpenBinderSyncTime(body.lastOpenBinderSyncTime)
            setLastImageDownloadSyncTime(body.lastImageDownloadSyncTime)
            setLastPriceLinkOverwriteSyncTime(body.lastPriceLinkOverwriteSyncTime)
        })
    }, [])



    const loadSyncGameList = useCallback(() => {
        return apiClient.get(`/api/admin/product/metadata/sync/game/list`).then((response) => {
            const body = response.data as TcgPSyncGameDto[]
            setSyncGameList(body)
        })
    }, [])

    useEffect(() => {
        if (init) {
            return
        }
        void loadSyncGameList().finally(() => setLoadingSyncGames(false))
        void loadLastSyncTime().finally(() => setLoadingLastSync(false))
    }, [init, loadSyncGameList, loadLastSyncTime])



    const handleMetadataSync = useCallback(() => {
        const confirmed = confirm("동기화를 시작하시겠습니까?")
        if (!confirmed) return
        void apiClient
            .post("/api/admin/product/metadata/sync")
            .then(() => {
                alert("동기화가 완료되었습니다")
                return loadLastSyncTime()
            })
            .catch((error) => {
                console.error(error)
                const status = (error as AxiosError).response?.status
                if (status === 409) {
                    alert("이미 동기화가 진행중입니다.")
                } else {
                    console.error(error)
                    alert("동기화에 실패했습니다")
                }
            })
    }, [loadLastSyncTime])

    const handleSinglePriceSync = useCallback(() => {
        const confirmed = confirm("동기화를 시작하시겠습니까? 이 작업은 굉장히 오래 걸릴 수 있습니다.")
        if (confirmed) {
            void apiClient
                .post("/api/admin/product/metadata/tcg-p-prices/sync")
                .then(() => {
                    alert("동기화를 백그라운드에서 시작했습니다.")
                })
                .catch((error) => {
                    console.error(error)
                    const status = (error as AxiosError).response?.status
                    if (status === 409) {
                        alert("이미 동기화가 진행중입니다.")
                    } else {
                        console.error(error)
                        alert("동기화 시작 요청에 실패했습니다.")
                    }
                })
        }
    }, [])

    const handleTcgPImagesDownload = useCallback(() => {
        void apiClient.post("/api/admin/product/metadata/tcg-p-images/download").then((response) => {
            //console.log(response)
            alert(response.data)
        }).catch((error) => {
            console.error(error)
            const status = (error as AxiosError).response?.status
            if (status === 409) {
                alert("이미 다운로드가 진행중입니다.")
            } else {
                console.error(error)
                alert("다운로드 시작 요청에 실패했습니다.")
            }
        })
    }, [])

    const handleOpenBinderImagesDownload = useCallback(() => {
        const confirmed = confirm("외부 카드 이미지를 내부 저장소로 다운로드합니다. 완료 후 실패한 MTG 이미지는 Scryfall에서 이어서 받습니다. 진행할까요?")
        if (!confirmed) return
        void apiClient.post("/api/admin/product/metadata/openbinder-images/download").then((response) => {
            //console.log(response)
            alert(response.data)
        }).catch((error) => {
            console.error(error)
            const status = (error as AxiosError).response?.status
            if (status === 409) {
                alert("이미 외부 카드 이미지 다운로드가 진행 중입니다.")
            } else {
                alert("외부 카드 이미지 다운로드 시작 요청에 실패했습니다.")
            }
        })
    }, [])

    const handleScryfallImagesDownload = useCallback(() => {
        const confirmed = confirm("외부 이미지 다운로드에 실패한 MTG 이미지를 Scryfall에서 받아옵니다. 진행할까요?")
        if (!confirmed) return
        void apiClient.post("/api/admin/product/metadata/scryfall-images/download").then((response) => {
            alert(response.data)
        }).catch((error) => {
            console.error(error)
            const status = (error as AxiosError).response?.status
            if (status === 409) {
                alert("이미 Scryfall 이미지 다운로드가 진행 중입니다.")
            } else {
                alert("Scryfall 이미지 다운로드 시작 요청에 실패했습니다.")
            }
        })
    }, [])

    const handleOpenBinderPricesSync = useCallback(() => {
        void apiClient.post("/api/admin/product/metadata/sync/openbinder-prices").then((response) => {
            console.log(response)
            alert(response.data)
        })
            .catch((error) => {
                console.error(error)
                const status = (error as AxiosError).response?.status
                if (status === 409) {
                    alert("이미 동기화가 진행중입니다.")
                } else {
                    console.error(error)
                    alert("동기화 시작 요청에 실패했습니다.")
                }
            })
    }, [])

    const handleCheckCodeRebuild = useCallback(() => {
        const ok = confirm(
            "check_code/check_code_refined만 초기화 후 재계산합니다. ID(tcgPPriceId/obPriceId)는 건드리지 않습니다. 시작할까요?"
        )
        if (!ok) return
        void apiClient
            .post("/api/admin/product/metadata/sync/price-check-code-rebuild", null, { timeout: 60_000 })
            .then((response) => {
                alert(response.data as string)
            })
            .catch((error) => {
                console.error(error)
                const status = (error as AxiosError).response?.status
                if (status === 409) {
                    alert("다른 가격 링크 작업이 실행 중입니다.")
                } else {
                    alert("요청에 실패했습니다.")
                }
            })
    }, [])

    const handleCheckCodeRebuildSelective = useCallback(() => {
        const ok = confirm(
            "NULL 초기화 없이 재계산합니다. check_code와 check_code_refined가 같으면 둘 다 갱신하고, 다르면(예: DUPLICATE_CODE) check_code만 갱신합니다. 시작할까요?"
        )
        if (!ok) return
        void apiClient
            .post("/api/admin/product/metadata/sync/price-check-code-rebuild-selective", null, { timeout: 60_000 })
            .then((response) => {
                alert(response.data as string)
            })
            .catch((error) => {
                console.error(error)
                const status = (error as AxiosError).response?.status
                if (status === 409) {
                    alert("다른 가격 링크 작업이 실행 중입니다.")
                } else {
                    alert("요청에 실패했습니다.")
                }
            })
    }, [])

    const handleLinkRebuild = useCallback(() => {
        const ok = confirm(
            "tcgPPriceId/obPriceId만 초기화 후 재연결합니다. check_code는 건드리지 않습니다. 시작할까요?"
        )
        if (!ok) return
        void apiClient
            .post("/api/admin/product/metadata/sync/price-link-rebuild", null, { timeout: 60_000 })
            .then((response) => {
                alert(response.data as string)
            })
            .catch((error) => {
                console.error(error)
                const status = (error as AxiosError).response?.status
                if (status === 409) {
                    alert("다른 가격 링크 작업이 실행 중입니다.")
                } else {
                    alert("요청에 실패했습니다.")
                }
            })
    }, [])

    const handleFullRebuild = useCallback(() => {
        const ok = confirm(
            "check_code 재빌드 후 링크 재연결까지 전체 재빌드를 실행합니다. 완료는 싱크 로그에서 확인하세요. 시작할까요?"
        )
        if (!ok) return
        void apiClient
            .post("/api/admin/product/metadata/sync/price-full-rebuild", null, { timeout: 60_000 })
            .then((response) => {
                alert(response.data as string)
            })
            .catch((error) => {
                console.error(error)
                const status = (error as AxiosError).response?.status
                if (status === 409) {
                    alert("다른 가격 링크 작업이 실행 중입니다.")
                } else {
                    alert("요청에 실패했습니다.")
                }
            })
    }, [])

    const handlePriceLinkRescanNulls = useCallback(() => {
        const ok = confirm(
            "NULL 미연결 행만 갭 정비 스캔으로 다시 링크합니다. 다른 링크 작업과 동시에 실행할 수 없습니다. 시작할까요?"
        )
        if (!ok) return
        void apiClient
            .post("/api/admin/product/metadata/sync/price-link-rescan-nulls", null, { timeout: 60_000 })
            .then((response) => {
                alert(response.data as string)
            })
            .catch((error) => {
                console.error(error)
                const status = (error as AxiosError).response?.status
                if (status === 409) {
                    alert("다른 가격 링크 작업이 실행 중입니다.")
                } else {
                    alert("요청에 실패했습니다.")
                }
            })
    }, [])

    const handleSaveAllPricesToUnion = useCallback(() => {
        const ok = confirm(
            "모든 가격을 union_prices에 저장하는 작업을 백그라운드에서 시작합니다. 진행할까요?"
        )
        if (!ok) return
        void apiClient
            .post("/api/admin/product/metadata/sync/price-union-save", null, { timeout: 60_000 })
            .then((response) => {
                alert(response.data as string)
            })
            .catch((error) => {
                console.error(error)
                const status = (error as AxiosError).response?.status
                if (status === 409) {
                    alert("이미 union 저장 작업이 진행 중입니다.")
                } else {
                    alert("요청에 실패했습니다.")
                }
            })
    }, [])

    const handleUnionPublicIdBackfill = useCallback(() => {
        const ok = confirm(
            "현재 union_prices 데이터의 public_id 누락값을 즉시 백필합니다. 진행할까요?"
        )
        if (!ok) return
        void apiClient
            .post("/api/admin/product/metadata/sync/price-union-public-id-backfill", null, { timeout: 120_000 })
            .then((response) => {
                alert(response.data as string)
            })
            .catch((error) => {
                console.error(error)
                alert("백필 요청에 실패했습니다.")
            })
    }, [])

    const handleStockCharge = useCallback(() => {
        void apiClient.post("/api/admin/product/metadata/sync/stock-charge").then((response) => {
            console.log(response)
            alert(response.data)
        })
            .catch((error) => {
                console.error(error)
                const status = (error as AxiosError).response?.status
                if (status === 409) {
                    alert("이미 재고 충전이 진행중입니다.")
                } else {
                    console.error(error)
                    alert("재고 충전 시작 요청에 실패했습니다.")
                }
            })
    }, [])

    return {
        syncGameList,
        lastPriceSyncTime,
        lastMetadataSyncTime,
        lastOpenBinderSyncTime,
        lastImageDownloadSyncTime,
        lastPriceLinkOverwriteSyncTime,
        loadingSyncGames,
        loadingLastSync,
        handleMetadataSync,
        handleSinglePriceSync,
        handleTcgPImagesDownload,
        handleOpenBinderImagesDownload,
        handleScryfallImagesDownload,
        handleOpenBinderPricesSync,
        handleCheckCodeRebuild,
        handleCheckCodeRebuildSelective,
        handleLinkRebuild,
        handleFullRebuild,
        handlePriceLinkRescanNulls,
        handleSaveAllPricesToUnion,
        handleUnionPublicIdBackfill,
        handleStockCharge,
    }
}
