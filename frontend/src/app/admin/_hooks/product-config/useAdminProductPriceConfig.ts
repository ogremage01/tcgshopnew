import { useState, useEffect, useCallback } from "react"
import { apiClient } from "@/lib/api"
import { PriceConfigDto } from "@/types/product"
import { GradePricingPolicyDto } from "@/types/product"
import { GAME_ENUM } from "@/config/gameEnum"

/**
 * 게임별 최소 가격, 등급별 가격 비율, 게임별 환율
 * @param options.initialData — 설정 시 마운트 시 GET 생략 (부트스트랩과 함께 사용)
 */
export function useAdminProductPriceConfig(options?: {
    initialData?: {
        minimumPrices: PriceConfigDto[]
        grade: GradePricingPolicyDto[]
        currencyRates?: PriceConfigDto[]
    }
}) {
    const init = options?.initialData
    const [minimumPrices, setMinimumPrices] = useState<PriceConfigDto[]>(init?.minimumPrices ?? [])
    const [gradePrice, setGradePrice] = useState<GradePricingPolicyDto[]>(init?.grade ?? [])
    const [currencyRates, setCurrencyRates] = useState<PriceConfigDto[]>(init?.currencyRates ?? [])
    const [loadingMinimumPrices, setLoadingMinimumPrices] = useState(!init)
    const [loadingGradePrice, setLoadingGradePrice] = useState(!init)
    const [loadingCurrencyRates, setLoadingCurrencyRates] = useState(!init?.currencyRates)
    const [submittingGame, setSubmittingGame] = useState<string | null>(null)

    const loadMinimumPrices = useCallback(() => {
        return Promise.all(
            GAME_ENUM.map((g) =>
                apiClient
                    .get<PriceConfigDto>(`/api/admin/product/metadata/minimum-price/${g.gameAbbr}`)
                    .then((r) => r.data)
                    .catch(() => null)
            )
        ).then((results) => {
            setMinimumPrices(results.filter((r): r is PriceConfigDto => r !== null))
        })
    }, [])

    const loadGradePrice = useCallback(() => {
        return apiClient.get(`/api/admin/product/metadata/grade-price`).then((response) => {
            const body = response.data as GradePricingPolicyDto[]
            setGradePrice(
                body.map((price) => ({ id: price.id, grade: price.grade, percentage: price.percentage }))
            )
        })
    }, [])

    const loadCurrencyRates = useCallback(() => {
        return apiClient
            .get<PriceConfigDto[]>(`/api/admin/product/metadata/us-currency-rate`)
            .then((r) => {
                setCurrencyRates(r.data ?? [])
            })
            .catch((error) => {
                console.error(error)
                alert("환율 목록 로드에 실패했습니다")
            })
    }, [])

    useEffect(() => {
        if (!init) {
            void loadMinimumPrices().finally(() => setLoadingMinimumPrices(false))
            void loadGradePrice().finally(() => setLoadingGradePrice(false))
        }

        if (!init?.currencyRates) {
            void loadCurrencyRates().finally(() => setLoadingCurrencyRates(false))
        } else {
            setLoadingCurrencyRates(false)
        }
    }, [init, loadMinimumPrices, loadGradePrice, loadCurrencyRates])

    const handleExchangeRateChange = useCallback(
        (gameAbbr: string, e: React.FormEvent<HTMLFormElement>) => {
            e.preventDefault()
            const formData = new FormData(e.target as HTMLFormElement)
            const exchangeRateValue = formData.get("exchangeRate")

            if (exchangeRateValue == null || exchangeRateValue === "") {
                alert("환율 값을 입력해주세요.")
                return Promise.resolve()
            }

            const confirmed = window.confirm(
                "이 작업은 현재 등록된 상품가에 영향을 미치며, 데이터 양에 따라 시간이 걸릴 수 있습니다."
            )
            if (!confirmed) {
                return Promise.resolve()
            }

            setSubmittingGame(gameAbbr)
            return apiClient
                .post(`/api/admin/product/metadata/us-currency-rate/${gameAbbr}`, {
                    configValue: Number(exchangeRateValue),
                })
                .then(() => {
                    alert("환율 변경이 완료되었습니다")
                    return loadCurrencyRates()
                })
                .catch((error) => {
                    console.error(error)
                    alert("환율 변경에 실패했습니다")
                })
                .finally(() => {
                    setSubmittingGame(null)
                })
        },
        [loadCurrencyRates]
    )

    const handleMinimumPriceChange = useCallback(
        (gameAbbr: string, e: React.FormEvent<HTMLFormElement>) => {
            e.preventDefault()
            const formData = new FormData(e.target as HTMLFormElement)
            const minimumPriceValue = formData.get("minimumPrice")
            if (minimumPriceValue == null || minimumPriceValue === "") {
                alert("최소 가격 값을 입력해주세요.")
                return Promise.resolve()
            }
            return apiClient
                .post(`/api/admin/product/metadata/minimum-price/${gameAbbr}`, {
                    configValue: Number(minimumPriceValue),
                })
                .then(() => {
                    alert("최소 가격 변경이 완료되었습니다")
                    return loadMinimumPrices()
                })
                .catch((error) => {
                    console.error(error)
                    alert("최소 가격 변경에 실패했습니다")
                })
        },
        [loadMinimumPrices]
    )

    const handleGradePriceChange = useCallback(
        (e: React.FormEvent<HTMLFormElement>) => {
            e.preventDefault()
            const formData = new FormData(e.target as HTMLFormElement)
            return apiClient
                .post(`/api/admin/product/metadata/grade-price`, {
                    id: Number(formData.get("gradeId")),
                    grade: formData.get("grade") as string,
                    percentage: Number(formData.get("percentage")),
                })
                .then(() => {
                    alert("등급 별 카드 가격 변경이 완료되었습니다")
                    return loadGradePrice()
                })
                .catch((error) => {
                    console.error(error)
                    alert("등급 별 카드 가격 변경에 실패했습니다")
                })
        },
        [loadGradePrice]
    )

    return {
        minimumPrices,
        gradePrice,
        currencyRates,
        loadingMinimumPrices,
        loadingGradePrice,
        loadingCurrencyRates,
        handleMinimumPriceChange,
        handleGradePriceChange,
        handleExchangeRateChange,
        submittingGame,
    }
}
