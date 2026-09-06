"use client"

import { useCallback, useEffect, useState, type FormEvent } from "react"
import { api, getApiErrorMessage } from "@/lib/api"
import type { Page } from "@/types/pagination"
import type { SupplyTypeDto } from "@/types/product"

const PAGE_SIZE = 10

export function useSupplyTypesTab() {
    const [supplyTypeListPage, setSupplyTypeListPage] = useState<Page<SupplyTypeDto> | null>(null)
    const [currentPage, setCurrentPage] = useState(1)
    const [totalPages, setTotalPages] = useState(1)
    const [supplyTypeAddModalOpen, setSupplyTypeAddModalOpen] = useState(false)
    const [supplyTypeEditModalOpen, setSupplyTypeEditModalOpen] = useState(false)
    const [editingSupplyType, setEditingSupplyType] = useState<SupplyTypeDto | null>(null)

    const loadSupplyTypePageList = useCallback(() => {
        api.get<Page<SupplyTypeDto>>("/api/admin/product/supply-products/supply-types-by-page", {
            params: {
                page: currentPage - 1,
                size: PAGE_SIZE,
                sort: "id,desc",
            },
        })
            .then((res) => {
                setSupplyTypeListPage(res)
                setTotalPages(Math.max(1, res.totalPages ?? 1))
            })
            .catch((err) => {
                alert(`서플라이 분류 목록 조회에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }, [currentPage])

    useEffect(() => {
        loadSupplyTypePageList()
    }, [loadSupplyTypePageList])

    const handleSupplyTypeAdd = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        const formData = new FormData(e.currentTarget)
        const nameEn = formData.get("nameEn") as string
        const nameKo = formData.get("nameKo") as string

        // 백엔드 AdminSupplyProductController: POST /supply-types
        api.post<void>("/api/admin/product/supply-products/supply-types", { nameEn, nameKo })
            .then(() => {
                setSupplyTypeAddModalOpen(false)
                loadSupplyTypePageList()
            })
            .catch((err) => {
                alert(`서플라이 분류 추가에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    const openSupplyTypeEditModal = (supplyType: SupplyTypeDto) => {
        setEditingSupplyType(supplyType)
        setSupplyTypeEditModalOpen(true)
    }

    const handleSupplyTypeEditModalOpenChange = (open: boolean) => {
        setSupplyTypeEditModalOpen(open)
        if (!open) {
            setEditingSupplyType(null)
        }
    }

    const handleSupplyTypeUpdate = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        if (!editingSupplyType) return

        const formData = new FormData(e.currentTarget)
        const nameEn = (formData.get("nameEn") as string).trim()
        const nameKo = (formData.get("nameKo") as string).trim()
        if (!nameEn || !nameKo) return
        if (nameEn === editingSupplyType.nameEn && nameKo === editingSupplyType.nameKo) return

        api.put<void>(`/api/admin/product/supply-products/supply-types/${editingSupplyType.id}`, { nameEn, nameKo })
            .then(() => {
                setSupplyTypeEditModalOpen(false)
                setEditingSupplyType(null)
                loadSupplyTypePageList()
            })
            .catch((err) => {
                alert(`서플라이 분류 수정에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    const handleSupplyTypeDelete = (supplyTypeId: number) => {
        if (!window.confirm("해당 서플라이 분류를 삭제하시겠습니까?")) return

        api.post<void>(`/api/admin/product/supply-products/supply-types/delete/${supplyTypeId}`)
            .then(() => {
                loadSupplyTypePageList()
            })
            .catch((err) => {
                alert(`서플라이 분류 삭제에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    return {
        supplyTypeListPage,
        currentPage,
        setCurrentPage,
        totalPages,
        supplyTypeAddModalOpen,
        setSupplyTypeAddModalOpen,
        supplyTypeEditModalOpen,
        editingSupplyType,
        openSupplyTypeEditModal,
        handleSupplyTypeEditModalOpenChange,
        handleSupplyTypeAdd,
        handleSupplyTypeUpdate,
        handleSupplyTypeDelete,
    }
}
