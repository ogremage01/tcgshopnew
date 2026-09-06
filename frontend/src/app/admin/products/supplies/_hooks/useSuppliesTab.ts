"use client"

import { useCallback, useEffect, useState, type FormEvent } from "react"
import { api, getApiErrorMessage } from "@/lib/api"
import type { Page } from "@/types/pagination"
import type { SupplyDto } from "@/types/product"

const PAGE_SIZE = 10

export function useSuppliesTab() {
    const [supplyListPage, setSupplyListPage] = useState<Page<SupplyDto> | null>(null)
    const [currentPage, setCurrentPage] = useState(1)
    const [totalPages, setTotalPages] = useState(1)
    const [searchKeyword, setSearchKeyword] = useState("")
    const [appliedKeyword, setAppliedKeyword] = useState("")

    const fetchSupplyList = useCallback((page: number, keyword: string) => {
        const params: Record<string, string | number> = {
            page: page - 1,
            size: PAGE_SIZE,
            sort: "id,desc",
        }
        if (keyword.trim()) {
            params.keyword = keyword.trim()
        }
        return api.get<Page<SupplyDto>>("/api/admin/product/supply-products", undefined, { params })
    }, [])

    const loadSupplyList = useCallback(() => {
        fetchSupplyList(currentPage, appliedKeyword)
            .then((res) => {
                setSupplyListPage(res)
                setTotalPages(Math.max(1, res.totalPages ?? 1))
            })
            .catch((err) => {
                alert(`서플라이 목록 조회에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }, [currentPage, appliedKeyword, fetchSupplyList])

    useEffect(() => {
        loadSupplyList()
    }, [loadSupplyList])

    const handleSearch = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        const keyword = searchKeyword.trim()
        setAppliedKeyword(keyword)
        setCurrentPage(1)
        fetchSupplyList(1, keyword)
            .then((res) => {
                setSupplyListPage(res)
                setTotalPages(Math.max(1, res.totalPages ?? 1))
            })
            .catch((err) => {
                alert(`서플라이 목록 조회에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    const handleSearchReset = () => {
        setSearchKeyword("")
        setAppliedKeyword("")
        setCurrentPage(1)
        fetchSupplyList(1, "")
            .then((res) => {
                setSupplyListPage(res)
                setTotalPages(Math.max(1, res.totalPages ?? 1))
            })
            .catch((err) => {
                alert(`서플라이 목록 조회에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    const handleDelete = (supplyId: number) => {
        if (!window.confirm("해당 서플라이를 삭제하시겠습니까?")) return

        api.post<void>(`/api/admin/product/supply-products/delete/${supplyId}`)
            .then(() => {
                loadSupplyList()
            })
            .catch((err) => {
                alert(`서플라이 삭제에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    return {
        supplyListPage,
        currentPage,
        setCurrentPage,
        totalPages,
        searchKeyword,
        setSearchKeyword,
        appliedKeyword,
        handleSearch,
        handleSearchReset,
        handleDelete,
        loadSupplyList,
    }
}
