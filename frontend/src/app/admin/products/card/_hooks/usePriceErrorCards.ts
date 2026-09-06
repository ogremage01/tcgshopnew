"use client"

import { useCallback, useEffect, useState } from "react"
import { isAxiosError } from "axios"
import { apiClient, getApiErrorMessage } from "@/lib/api"
import type { Page } from "@/types/pagination"
import type { CardProductManagementResponseDto } from "@/types/product"

const PRICE_ERROR_CARDS_URL = "/api/admin/product/single-products/price-error-cards"

const PAGE_SIZE = 20

export function usePriceErrorCards() {
    const [currentPage, setCurrentPage] = useState(1)
    const [data, setData] = useState<CardProductManagementResponseDto[]>([])
    const [totalPages, setTotalPages] = useState(0)
    const [totalElements, setTotalElements] = useState(0)
    const [isLoading, setIsLoading] = useState(false)

    useEffect(() => {
        const ac = new AbortController()
        setIsLoading(true)

        apiClient
            .get(PRICE_ERROR_CARDS_URL, {
                params: { page: currentPage - 1, size: PAGE_SIZE },
                signal: ac.signal,
            })
            .then((res) => {
                const pageData = res.data as Page<CardProductManagementResponseDto>
                setData(pageData?.content ?? [])
                setTotalPages(pageData?.totalPages ?? 0)
                setTotalElements(pageData?.totalElements ?? 0)
            })
            .catch((err) => {
                if (isAxiosError(err) && err.code === "ERR_CANCELED") return
                console.error(getApiErrorMessage(err) ?? err)
            })
            .finally(() => setIsLoading(false))

        return () => ac.abort()
    }, [currentPage])

    const handlePageChange = useCallback((page: number) => {
        setCurrentPage(page)
    }, [])

    const removeFromList = useCallback((id: number) => {
        setData((prev) => prev.filter((c) => c.id !== id))
        setTotalElements((n) => Math.max(0, n - 1))
    }, [])

    return {
        data,
        currentPage,
        totalPages,
        totalElements,
        isLoading,
        handlePageChange,
        removeFromList,
    }
}
