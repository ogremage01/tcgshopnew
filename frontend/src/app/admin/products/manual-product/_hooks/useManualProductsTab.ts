"use client"

import { useCallback, useEffect, useState } from "react"
import { api, getApiErrorMessage } from "@/lib/api"
import type { Page } from "@/types/pagination"
import type { ManualProductDto } from "@/types/product"

const PAGE_SIZE = 10

export function useManualProductsTab() {
    const [productListPage, setProductListPage] = useState<Page<ManualProductDto> | null>(null)
    const [currentPage, setCurrentPage] = useState(1)
    const [totalPages, setTotalPages] = useState(1)
    const [appliedKeyword, setAppliedKeyword] = useState("")

    const applyKeyword = useCallback((keyword: string) => {
        setAppliedKeyword(keyword)
        setCurrentPage(1)
    }, [])

    const loadProductList = useCallback(() => {
        const params: Record<string, unknown> = {
            page: currentPage - 1,
            size: PAGE_SIZE,
            sort: "id,desc",
        }
        if (appliedKeyword) params.keyword = appliedKeyword

        api.get<Page<ManualProductDto>>(
            "/api/admin/product/manual-products",
            undefined,
            { params },
        )
            .then((res) => {
                setProductListPage(res)
                setTotalPages(Math.max(1, res.totalPages ?? 1))
            })
            .catch((err) => {
                alert(`수동 상품 목록 조회에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }, [currentPage, appliedKeyword])

    useEffect(() => {
        loadProductList()
    }, [loadProductList])

    const handleDelete = (productId: number) => {
        if (!window.confirm("해당 수동 상품을 삭제하시겠습니까?")) return

        api.delete<void>(`/api/admin/product/manual-products/${productId}`)
            .then(() => {
                loadProductList()
            })
            .catch((err) => {
                alert(`수동 상품 삭제에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    return {
        productListPage,
        currentPage,
        setCurrentPage,
        totalPages,
        appliedKeyword,
        applyKeyword,
        handleDelete,
        loadProductList,
    }
}
