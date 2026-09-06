"use client"

import { useCallback, useEffect, useState } from "react"
import { api, getApiErrorMessage } from "@/lib/api"
import type { Page } from "@/types/pagination"
import type { SealedProductAdminDto } from "@/types/product"

const PAGE_SIZE = 10

export interface SealedProductSetItem {
    setCode: string
    setName: string
}

export interface SealedProductGameFacet {
    game: string
    sets: SealedProductSetItem[]
}

export interface SealedProductFilters {
    keyword?: string
    game?: string
    setCode?: string
}

export function useSealedProductsTab() {
    const [productListPage, setProductListPage] = useState<Page<SealedProductAdminDto> | null>(null)
    const [currentPage, setCurrentPage] = useState(1)
    const [totalPages, setTotalPages] = useState(1)
    const [facets, setFacets] = useState<SealedProductGameFacet[]>([])
    const [filters, setFilters] = useState<SealedProductFilters>({})

    const loadFacets = useCallback(() => {
        api.get<SealedProductGameFacet[]>("/api/admin/product/sealed-products/facets")
            .then(setFacets)
            .catch(() => {})
    }, [])

    useEffect(() => {
        loadFacets()
    }, [loadFacets])

    const loadProductList = useCallback(() => {
        const params: Record<string, unknown> = {
            page: currentPage - 1,
            size: PAGE_SIZE,
            sort: "id,desc",
        }
        if (filters.keyword) params.keyword = filters.keyword
        if (filters.game) params.game = filters.game
        if (filters.setCode) params.setCode = filters.setCode

        api.get<Page<SealedProductAdminDto>>(
            "/api/admin/product/sealed-products",
            undefined,
            { params },
        )
            .then((res) => {
                setProductListPage(res)
                setTotalPages(Math.max(1, res.totalPages ?? 1))
            })
            .catch((err) => {
                alert(`밀봉 상품 목록 조회에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }, [currentPage, filters])

    useEffect(() => {
        loadProductList()
    }, [loadProductList])

    const applyFilters = (next: SealedProductFilters) => {
        setCurrentPage(1)
        setFilters(next)
    }

    const handleDelete = (productId: number) => {
        if (!window.confirm("해당 밀봉 상품을 삭제하시겠습니까?")) return

        api.delete<void>(`/api/admin/product/sealed-products/${productId}`)
            .then(() => {
                loadProductList()
                loadFacets()
            })
            .catch((err) => {
                alert(`밀봉 상품 삭제에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    return {
        productListPage,
        currentPage,
        setCurrentPage,
        totalPages,
        facets,
        filters,
        applyFilters,
        handleDelete,
        loadProductList,
        loadFacets,
    }
}
