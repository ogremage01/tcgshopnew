"use client"

import { useCallback, useEffect, useState, type FormEvent } from "react"
import { api, getApiErrorMessage } from "@/lib/api"
import type { Page } from "@/types/pagination"
import type { ProductIpDto } from "@/types/product"

const PAGE_SIZE = 10

export function useProductIpsTab() {
    const [productIpListPage, setProductIpListPage] = useState<Page<ProductIpDto> | null>(null)
    const [currentPage, setCurrentPage] = useState(1)
    const [totalPages, setTotalPages] = useState(1)
    const [productIpAddModalOpen, setProductIpAddModalOpen] = useState(false)
    const [productIpEditModalOpen, setProductIpEditModalOpen] = useState(false)
    const [editingProductIp, setEditingProductIp] = useState<ProductIpDto | null>(null)

    const loadProductIpPageList = useCallback(() => {
        api.get<Page<ProductIpDto>>("/api/admin/product/product-ips-by-page", {
            params: {
                page: currentPage - 1,
                size: PAGE_SIZE,
                sort: "id,desc",
            },
        })
            .then((res) => {
                setProductIpListPage(res)
                setTotalPages(Math.max(1, res.totalPages ?? 1))
            })
            .catch((err) => {
                alert(`제품 IP 목록 조회에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }, [currentPage])

    useEffect(() => {
        loadProductIpPageList()
    }, [loadProductIpPageList])

    const handleProductIpAdd = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        const formData = new FormData(e.currentTarget)
        const nameEn = formData.get("nameEn") as string
        const nameKo = formData.get("nameKo") as string

        api.post<void>("/api/admin/product/product-ips", { nameEn, nameKo })
            .then(() => {
                setProductIpAddModalOpen(false)
                loadProductIpPageList()
            })
            .catch((err) => {
                alert(`제품 IP 추가에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    const openProductIpEditModal = (productIp: ProductIpDto) => {
        setEditingProductIp(productIp)
        setProductIpEditModalOpen(true)
    }

    const handleProductIpEditModalOpenChange = (open: boolean) => {
        setProductIpEditModalOpen(open)
        if (!open) {
            setEditingProductIp(null)
        }
    }

    const handleProductIpUpdate = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        if (!editingProductIp) return

        const formData = new FormData(e.currentTarget)
        const nameEn = (formData.get("nameEn") as string).trim()
        const nameKo = (formData.get("nameKo") as string).trim()
        if (!nameEn || !nameKo) return
        if (nameEn === editingProductIp.nameEn && nameKo === editingProductIp.nameKo) return

        api.put<void>(`/api/admin/product/product-ips/${editingProductIp.id}`, { nameEn, nameKo })
            .then(() => {
                setProductIpEditModalOpen(false)
                setEditingProductIp(null)
                loadProductIpPageList()
            })
            .catch((err) => {
                alert(`제품 IP 수정에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    const handleProductIpDelete = (productIpId: number) => {
        if (!window.confirm("해당 제품 IP를 삭제하시겠습니까?")) return

        api.post<void>(`/api/admin/product/product-ips/delete/${productIpId}`)
            .then(() => {
                loadProductIpPageList()
            })
            .catch((err) => {
                alert(`제품 IP 삭제에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    return {
        productIpListPage,
        currentPage,
        setCurrentPage,
        totalPages,
        productIpAddModalOpen,
        setProductIpAddModalOpen,
        productIpEditModalOpen,
        editingProductIp,
        openProductIpEditModal,
        handleProductIpEditModalOpenChange,
        handleProductIpAdd,
        handleProductIpUpdate,
        handleProductIpDelete,
    }
}
