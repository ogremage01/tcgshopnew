"use client"

import { useCallback, useEffect, useState, type FormEvent } from "react"
import { api, getApiErrorMessage } from "@/lib/api"
import type { Page } from "@/types/pagination"
import type { MakerDto } from "@/types/product"

const PAGE_SIZE = 10

export function useMakersTab() {
    const [makerListPage, setMakerListPage] = useState<Page<MakerDto> | null>(null)
    const [currentPage, setCurrentPage] = useState(1)
    const [totalPages, setTotalPages] = useState(1)
    const [makerAddModalOpen, setMakerAddModalOpen] = useState(false)
    const [makerEditModalOpen, setMakerEditModalOpen] = useState(false)
    const [editingMaker, setEditingMaker] = useState<MakerDto | null>(null)

    const loadMakerPageList = useCallback(() => {
        api.get<Page<MakerDto>>("/api/admin/product/supply-products/makers-by-page", {
            params: {
                page: currentPage - 1,
                size: PAGE_SIZE,
                sort: "id,desc",
            },
        })
            .then((res) => {
                setMakerListPage(res)
                setTotalPages(Math.max(1, res.totalPages ?? 1))
            })
            .catch((err) => {
                alert(`제조사 목록 조회에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }, [currentPage])

    useEffect(() => {
        loadMakerPageList()
    }, [loadMakerPageList])

    const handleMakerAdd = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        const formData = new FormData(e.currentTarget)
        const name = formData.get("name") as string

        api.post<void>("/api/admin/product/supply-products/makers", { name })
            .then(() => {
                setMakerAddModalOpen(false)
                loadMakerPageList()
            })
            .catch((err) => {
                alert(`제조사 추가에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    const openMakerEditModal = (maker: MakerDto) => {
        setEditingMaker(maker)
        setMakerEditModalOpen(true)
    }

    const handleMakerEditModalOpenChange = (open: boolean) => {
        setMakerEditModalOpen(open)
        if (!open) {
            setEditingMaker(null)
        }
    }

    const handleMakerUpdate = (e: FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        if (!editingMaker) return

        const formData = new FormData(e.currentTarget)
        const name = (formData.get("name") as string).trim()
        if (!name || name === editingMaker.name) return

        api.put<void>(`/api/admin/product/supply-products/makers/${editingMaker.id}`, { name })
            .then(() => {
                setMakerEditModalOpen(false)
                setEditingMaker(null)
                loadMakerPageList()
            })
            .catch((err) => {
                alert(`제조사 수정에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    const handleMakerDelete = (makerId: number) => {
        if (!window.confirm("해당 제조사를 삭제하시겠습니까?")) return

        api.post<void>(`/api/admin/product/supply-products/makers/delete/${makerId}`)
            .then(() => {
                loadMakerPageList()
            })
            .catch((err) => {
                alert(`제조사 삭제에 실패했습니다. ${getApiErrorMessage(err) ?? ""}`)
            })
    }

    return {
        makerListPage,
        currentPage,
        setCurrentPage,
        totalPages,
        makerAddModalOpen,
        setMakerAddModalOpen,
        makerEditModalOpen,
        editingMaker,
        openMakerEditModal,
        handleMakerEditModalOpenChange,
        handleMakerAdd,
        handleMakerUpdate,
        handleMakerDelete,
    }
}
