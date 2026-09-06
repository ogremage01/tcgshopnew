import { useState, useEffect, useRef, useCallback } from "react"
import { apiClient } from "@/lib/api"
import { PointLogDto, UserManagementResponseDto } from "@/types/user"
import { Page } from "@/types/pagination"
import { toast } from "sonner"


/**
 * 회원 관리 훅 (회원 목록, 회원 메모, 회원 포인트 변경 로그)
 */
export function useAdminUser() {
    const [userList, setUserList] = useState<UserManagementResponseDto[]>([])
    const [total, setTotal] = useState(0)
    const [currentPage, setCurrentPage] = useState(1)
    const [userMemoMap, setUserMemoMap] = useState<Record<string, string>>({})
    const [originalUserMemoMap, setOriginalUserMemoMap] = useState<Record<string, string>>({})
    const [isMemoSavingMap, setIsMemoSavingMap] = useState<Record<string, boolean>>({})
    const [pointLogList, setPointLogList] = useState<PointLogDto[]>([])
    const [pointLogListWithSystem, setPointLogListWithSystem] = useState<PointLogDto[]>([])
    type UserDataChangeRequest = {
        id: string
        name: string
        role: string
        userStatus: string
        userMemo: string
        password?: string
        passwordConfirm?: string
    }
    type UserPointChangeRequest = {
        id: string
        point: number
        changeReason: string
    }
    type UserMemoChangeRequest = {
        id: string
        userMemo: string
    }

    /** 백엔드 PageResponseDto / Spring Page 공통 필드 */
    type UserListPageBody = Pick<Page<UserManagementResponseDto>, "content" | "totalPages" | "pageNumber">

    const skipNextListFetchRef = useRef(false)

    const applyUserListFromPageBody = useCallback(
        (body: UserListPageBody, options?: { skipNextListFetch?: boolean }) => {
            if (options?.skipNextListFetch) {
                skipNextListFetchRef.current = true
            }
            const content = body.content ?? []
            const nextMemoMap: Record<string, string> = {}
            const nextOriginalMemoMap: Record<string, string> = {}
            content.forEach((user) => {
                const memo = user.userMemo ?? ""
                nextMemoMap[user.id] = memo
                nextOriginalMemoMap[user.id] = memo
            })
            setUserList(content)
            setUserMemoMap(nextMemoMap)
            setOriginalUserMemoMap(nextOriginalMemoMap)
            setTotal(body.totalPages ?? 0)
            setCurrentPage((body.pageNumber ?? 0) + 1)
            window.scrollTo({ top: 0, behavior: "instant" })
        },
        []
    )

    /**
     * 회원 목록 조회
     */
    useEffect(() => {
        if (skipNextListFetchRef.current) {
            skipNextListFetchRef.current = false
            return
        }
        apiClient.get(`/api/admin/user/list`, {
            params: {
                page: currentPage - 1,
                size: 10,
                sort: "createdAt,desc",
            },
        }).then(response => {
            //console.log(response.data)
            applyUserListFromPageBody(response.data as Page<UserManagementResponseDto>)
        })
    }, [currentPage, applyUserListFromPageBody])

    useEffect(() => {
        apiClient.get<PointLogDto[]>(`/api/admin/user/point-log/recent`).then((response) => {
            setPointLogList(response.data ?? [])
        })
    }, [])

    useEffect(() => {
        apiClient.get<PointLogDto[]>(`/api/admin/user/point-log/recent/include-system`).then((response) => {
            setPointLogListWithSystem(response.data ?? [])
        })
    }, [])

    /**
     * 회원 포인트 변경
     */
    const handleUserPointChange = async (e: React.FormEvent<HTMLFormElement>, userId: string) => {
        e.preventDefault()
        const form = e.currentTarget
        const formData = new FormData(form)

        try {
            const response = await apiClient.put<UserPointChangeRequest>(`/api/admin/user/point`, {
                id: userId,
                point: Number(formData.get("point")),
                changeReason: formData.get("changeReason") as string
            })
            if (response.status === 200) {
                //console.log(response)
                toast.success("회원 포인트가 변경되었습니다." + response.data + "포인트")
                setUserList((prev) =>
                    prev.map((user) => (user.id === userId ? { ...user, point: response.data as unknown as number } : user))
                )
            } else {
                throw new Error("회원 포인트 변경 응답 실패")
            }
        } catch (error) {
            console.error(error)
            alert("회원 포인트 변경에 실패했습니다. " + error)
        } finally {
            form.reset()
        }
    }



    /**
     * 회원 상세 정보 변경
     */
    const handleUserDataChange = async (e: React.FormEvent<HTMLFormElement>, userId: string) => {
        e.preventDefault()
        const form = e.currentTarget
        const formData = new FormData(form)
        const name = formData.get("name") as string
        const role = formData.get("role") as string
        const userStatus = formData.get("userStatus") as string
        const userMemo = formData.get("userMemo") as string
        const password = (formData.get("password") as string)?.trim() ?? ""
        const passwordConfirm = (formData.get("passwordConfirm") as string)?.trim() ?? ""

        if (password || passwordConfirm) {
            if (password !== passwordConfirm) {
                toast.error("비밀번호와 비밀번호 확인이 일치하지 않습니다.")
                return
            }
        }

        const payload: UserDataChangeRequest = {
            id: userId,
            name,
            role,
            userStatus: userStatus.toUpperCase() as "ACTIVE" | "INACTIVE" | "DELETED" | "BANNED",
            userMemo,
        }
        if (password) {
            payload.password = password
            payload.passwordConfirm = passwordConfirm
        }

        try {
            const response = await apiClient.put<UserDataChangeRequest>(`/api/admin/user/detail`, payload)
            if (response.status === 200) {
                toast.success("회원 상세 정보가 변경되었습니다.")
                setUserList((prev) =>
                    prev.map((user) =>
                        user.id === userId
                            ? { ...user, name, role, userStatus: payload.userStatus, userMemo }
                            : user
                    )
                )
                form.reset()
            } else {
                throw new Error("회원 상세 정보 변경 응답 실패")
            }
        } catch (error) {
            console.error(error)
            toast.error("회원 상세 정보 변경에 실패했습니다.")
        }
    }

    /**
     * 회원 메모 변경
     */
    const handleUserMemoChange = async (e: React.FormEvent<HTMLFormElement>, userId: string) => {
        e.preventDefault()

        const originalUserMemo = originalUserMemoMap[userId] ?? ""
        const userMemo = userMemoMap[userId] ?? ""

        setIsMemoSavingMap((prev) => ({
            ...prev,
            [userId]: true
        }))

        try {
            const response = await apiClient.put<UserMemoChangeRequest>(`/api/admin/user/memo`, {
                id: userId,
                userMemo: userMemo
            })
            if (response.status === 200) {
                toast.success("회원 메모가 변경되었습니다.")
                setOriginalUserMemoMap((prev) => ({
                    ...prev,
                    [userId]: userMemo
                }))
                setUserList((prev) =>
                    prev.map((user) => (user.id === userId ? { ...user, userMemo } : user))
                )
            } else {
                throw new Error("회원 메모 변경 응답 실패")
            }
        } catch (error) {
            console.error(error)
            alert("회원 메모 변경에 실패했습니다.")
            setUserMemoMap((prev) => ({
                ...prev,
                [userId]: originalUserMemo
            }))
        } finally {
            setIsMemoSavingMap((prev) => ({
                ...prev,
                [userId]: false
            }))
        }
    }

    const handleUserMemoInputChange = (userId: string, userMemo: string) => {
        setUserMemoMap((prev) => ({
            ...prev,
            [userId]: userMemo
        }))
    }

    const handleUserDelete = async (userId: string) => {
        try {
            await apiClient.delete<void>(`/api/admin/user/${userId}`)
            toast.success("회원이 삭제되었습니다.")
            setUserList((prev) => {
                const next = prev.filter((user) => user.id !== userId)
                if (next.length === 0 && currentPage > 1) {
                    setCurrentPage(currentPage - 1)
                }
                return next
            })
        } catch (error) {
            console.error(error)
            toast.error("회원 삭제에 실패했습니다.")
        }
    }

    return {
        userList,
        setUserList,
        total,
        setTotal,
        currentPage,
        setCurrentPage,
        userMemoMap,
        isMemoSavingMap,
        pointLogList,
        pointLogListWithSystem,
        applyUserListFromPageBody,
        handleUserPointChange,
        handleUserMemoChange,
        handleUserMemoInputChange,
        handleUserDataChange,
        handleUserDelete,
    }
}

/**
 * 회원 포인트, 상태 변경 로그 관리
 */
export function useAdminUserLog() {
    // TODO: any[] → 백엔드 작업 후 PointLog 인터페이스로 교체
    const [pointLogList, setPointLogList] = useState<any[]>([])
    // TODO: any[] → 백엔드 작업 후 StatusLog 인터페이스로 교체
    const [statusLogList, setStatusLogList] = useState<any[]>([])
    const [pointTotal, setPointTotal] = useState(0)
    const [statusTotal, setStatusTotal] = useState(0)
    const [pointCurrentPage, setPointCurrentPage] = useState(1)
    const [statusCurrentPage, setStatusCurrentPage] = useState(1)

    useEffect(() => {
        apiClient.get(`/api/admin/user/log/point?page=${pointCurrentPage}`).then(response => {
            setPointLogList(response.data.data)
            setPointTotal(response.data.total)
            setPointCurrentPage(response.data.currentPage)
        })
    }, [pointCurrentPage])
    useEffect(() => {
        apiClient.get(`/api/admin/user/log/status?page=${statusCurrentPage}`).then(response => {
            setStatusLogList(response.data.data)
            setStatusTotal(response.data.total)
            setStatusCurrentPage(response.data.currentPage)

        })
    }, [statusCurrentPage])
    return {
        pointLogList,
        statusLogList,
        pointTotal,
        statusTotal,
        pointCurrentPage,
        statusCurrentPage,
    }
}