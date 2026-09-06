"use client"

import { useState, useEffect } from "react"
import { api, getApiErrorMessage } from "@/lib/api"
import type { Page } from "@/types/pagination"
import type { CardProductManagementResponseDto } from "@/types/product"

const PAGE_SIZE = 10

export function useCardProductManagementList(productId: number | null) {
    // page: 현재 페이지
    const [page, setPage] = useState(1)
    // cardList: 제품 목록
    const [cardList, setCardList] = useState<CardProductManagementResponseDto[]>([])
    // listEpoch: 목록 데이터 변경 횟수 (캐시 무효화 트리거)
    const [listEpoch, setListEpoch] = useState(0)
    // loadedProductId: 현재 표시 중인 제품 ID
    const [loadedProductId, setLoadedProductId] = useState<number | null>(null)
    // loadedPage: 현재 표시 중인 페이지
    const [loadedPage, setLoadedPage] = useState(0)
    // totalPages: 제품 전체 페이지 수
    const [totalPages, setTotalPages] = useState(1)
    // searchedProductList: 검색 결과 목록
    const [searchedProductList, setSearchedProductList] = useState<CardProductManagementResponseDto[]>([])
    // searchedTotalElements: 검색 결과 총 개수
    const [searchedTotalElements, setSearchedTotalElements] = useState(0)

    useEffect(() => {
        if (productId == null) {
            return
        }
        api
            .get<Page<CardProductManagementResponseDto>>(
                `/api/admin/product/single-products/${productId}?page=${page - 1}&size=${PAGE_SIZE}`
            )
            .then((p) => {
                setCardList(p.content ?? [])
                setTotalPages(Math.max(1, p.totalPages ?? 1))
                setListEpoch((e) => e + 1)
                setSearchedTotalElements(p.totalElements ?? 0)
                setSearchedProductList(p.content ?? [])
                setLoadedProductId(productId)
                setLoadedPage(page)
            })
            .catch((err) => console.error(getApiErrorMessage(err) ?? err))
    }, [productId, page])

    const hasProduct = productId != null
    const isLoading = hasProduct && (loadedProductId !== productId || loadedPage !== page)
    return {
        cardList: hasProduct ? cardList : [],
        listEpoch,
        isLoading,
        totalPages: hasProduct ? totalPages : 1,
        page,
        setPage,
        searchedProductList: hasProduct ? searchedProductList : [],
        setSearchedProductList,
        searchedTotalElements: hasProduct ? searchedTotalElements : 0,
    }
}
