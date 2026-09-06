"use client"

import { useCallback, useEffect, useState, type FormEvent } from "react"
import { api, getApiErrorMessage } from "@/lib/api"
import type { Page } from "@/types/pagination"
import type { ProductCategoryDto } from "@/types/product"

const PAGE_SIZE = 10

export function useProductCategoriesTab() {
    const [categoryListPage, setCategoryListPage] = useState<Page<ProductCategoryDto> | null>(null)
    const [currentPage, setCurrentPage] = useState(1)
    const [totalPages, setTotalPages] = useState(1)
    const [categoryAddModalOpen, setCategoryAddModalOpen] = useState(false)
    const [categoryEditModalOpen, setCategoryEditModalOpen] = useState(false)
    const [editingCategory, setEditingCategory] = useState<ProductCategoryDto | null>(null)

    const loadCategoryPageList = useCallback(() => {
        api.get<Page<ProductCategoryDto>>("/api/admin/product/product-categories-by-page", {
            params: {
                page: currentPage - 1,
                size: PAGE_SIZE,
                sort: "id,desc",
            },
        })
            .then((res) => {
                setCategoryListPage(res)
                setTotalPages(Math.max(1, res.totalPages ?? 1))
            })
            .catch((err) => {
                alert(`카테고리 목록 조회에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }, [currentPage])

    useEffect(() => {
        loadCategoryPageList()
    }, [loadCategoryPageList])

    const handleCategoryAdd = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        const formData = new FormData(e.currentTarget)
        const nameEn = formData.get("nameEn") as string
        const nameKo = formData.get("nameKo") as string

        api.post<void>("/api/admin/product/product-categories", { nameEn, nameKo })
            .then(() => {
                setCategoryAddModalOpen(false)
                loadCategoryPageList()
            })
            .catch((err) => {
                alert(`카테고리 추가에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    const openCategoryEditModal = (category: ProductCategoryDto) => {
        setEditingCategory(category)
        setCategoryEditModalOpen(true)
    }

    const handleCategoryEditModalOpenChange = (open: boolean) => {
        setCategoryEditModalOpen(open)
        if (!open) {
            setEditingCategory(null)
        }
    }

    const handleCategoryUpdate = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        if (!editingCategory) return

        const formData = new FormData(e.currentTarget)
        const nameEn = (formData.get("nameEn") as string).trim()
        const nameKo = (formData.get("nameKo") as string).trim()
        if (!nameEn || !nameKo) return
        if (nameEn === editingCategory.nameEn && nameKo === editingCategory.nameKo) return

        api.put<void>(`/api/admin/product/product-categories/${editingCategory.id}`, { nameEn, nameKo })
            .then(() => {
                setCategoryEditModalOpen(false)
                setEditingCategory(null)
                loadCategoryPageList()
            })
            .catch((err) => {
                alert(`카테고리 수정에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    const handleCategoryDelete = (categoryId: number) => {
        if (!window.confirm("해당 카테고리를 삭제하시겠습니까?")) return

        api.post<void>(`/api/admin/product/product-categories/delete/${categoryId}`)
            .then(() => {
                loadCategoryPageList()
            })
            .catch((err) => {
                alert(`카테고리 삭제에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    return {
        categoryListPage,
        currentPage,
        setCurrentPage,
        totalPages,
        categoryAddModalOpen,
        setCategoryAddModalOpen,
        categoryEditModalOpen,
        editingCategory,
        openCategoryEditModal,
        handleCategoryEditModalOpenChange,
        handleCategoryAdd,
        handleCategoryUpdate,
        handleCategoryDelete,
    }
}
