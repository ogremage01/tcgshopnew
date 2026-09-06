import { useState, useEffect, useCallback } from "react"
import { apiClient } from "@/lib/api"
import { StorageDto } from "@/types/product"

/**
 * 상품 메타데이터 저장소 목록·추가·수정·삭제는 킬스위치가 되므로 지원하지 않습니다.
 * @param options.initialList — 설정 시 마운트 시 GET 생략 (빈 배열도 사전 로드로 간주)
 */
export function useAdminProductStorage(options?: { initialList?: StorageDto[] }) {
    const hasInitial = options?.initialList !== undefined
    const [storageList, setStorageList] = useState<StorageDto[]>(options?.initialList ?? [])
    const [storageAddModalOpen, setStorageAddModalOpen] = useState(false)
    const [storageEditModalOpen, setStorageEditModalOpen] = useState(false)
    const [loadingStorageList, setLoadingStorageList] = useState(!hasInitial)

    const loadStorageList = useCallback(() => {
        return apiClient.get(`/api/admin/product/metadata/storage/list`).then((response) => {
            setStorageList(response.data as StorageDto[])
        })
    }, [])

    useEffect(() => {
        if (hasInitial) {
            return
        }
        void loadStorageList().finally(() => setLoadingStorageList(false))
    }, [hasInitial, loadStorageList])

    const handleStorageAdd = useCallback(
        (e: React.FormEvent<HTMLFormElement>) => {
            e.preventDefault()
            const formData = new FormData(e.target as HTMLFormElement)
            return apiClient
                .post(`/api/admin/product/metadata/storage`, {
                    storageName: formData.get("storageName") as string,
                    description: formData.get("description") as string,
                    isDefault: formData.get("isDefault") === "on" ? true : false,
                })
                .then(() => {
                    alert("저장소 추가가 완료되었습니다")
                    setStorageAddModalOpen(false)
                    return loadStorageList()
                })
                .catch((error) => {
                    console.error(error)
                    alert("저장소 추가에 실패했습니다")
                })
        },
        [loadStorageList]
    )

    const handleStorageEdit = useCallback(
        (e: React.FormEvent<HTMLFormElement>) => {
            e.preventDefault()
            const formData = new FormData(e.target as HTMLFormElement)
            return apiClient
                .put(`/api/admin/product/metadata/storage`, {
                    id: Number(formData.get("id")),
                    storageName: formData.get("storageName") as string,
                    description: formData.get("description") as string,
                    isDefault: formData.get("isDefault") === "on" ? true : false,
                })
                .then(() => {
                    alert("저장소 변경이 완료되었습니다")
                    setStorageEditModalOpen(false)
                    return loadStorageList()
                })
                .catch((error) => {
                    console.error(error)
                    alert("저장소 변경에 실패했습니다")
                })
        },
        [loadStorageList]
    )

    return {
        storageList,
        loadingStorageList,
        storageAddModalOpen,
        setStorageAddModalOpen,
        storageEditModalOpen,
        setStorageEditModalOpen,
        handleStorageAdd,
        handleStorageEdit,
    }
}
