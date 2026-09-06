import { useCallback, useEffect, useState } from "react"
import { apiClient } from "@/lib/api"
import { Page } from "@/types/pagination"
import { SyncLogDto } from "@/types/syncLog"

export function useAdminProductSyncLog() {
    const [syncLog, setSyncLog] = useState<SyncLogDto[]>([])
    const [totalPages, setTotalPages] = useState(0)
    const [currentPage, setCurrentPage] = useState(1)
    const [loadingSyncLog, setLoadingSyncLog] = useState(true)
    const loadSyncLogs = useCallback((page: number) => {
        return apiClient.get(`/api/admin/product/metadata/sync/log`, {
            params: {
                page: page - 1,
                size: 10,
                sort: "endTime,desc",
            },
        }).then((response) => {
            const body = response.data as Page<SyncLogDto>
            setSyncLog(body.content)
            setTotalPages(body.totalPages)
            setLoadingSyncLog(false)
        })
    }, [])

    useEffect(() => {
        void loadSyncLogs(currentPage)
    }, [currentPage, loadSyncLogs])
    return {
        syncLog,
        totalPages,
        currentPage,
        loadingSyncLog,
        loadSyncLogs,
        setCurrentPage,
    }
}