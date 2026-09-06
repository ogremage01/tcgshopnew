"use client"

import { useCallback, useEffect, useState } from "react"
import { api } from "@/lib/api"
import type { SetBannerDto } from "@/types/banner"

export function useSetBannerConfig() {
    const [setBannerList, setSetBannerList] = useState<SetBannerDto[]>([])
    const [editingBanner, setEditingBanner] = useState<SetBannerDto | null>(null)

    const loadSetBannerList = useCallback(() => {
        api.get<SetBannerDto[]>("/api/admin/site-setting/set-banners/list").then((response) => {
            setSetBannerList(response)
        })
    }, [])

    useEffect(() => {
        loadSetBannerList()
    }, [loadSetBannerList])

    const handleUpdateSetBanner = useCallback(
        (e: React.FormEvent<HTMLFormElement>) => {
            e.preventDefault()
            if (!editingBanner) return

            const form = e.target as HTMLFormElement
            const formData = new FormData(form)
            const activeCheckbox = form.elements.namedItem("active") as HTMLInputElement | null
            formData.set("active", activeCheckbox?.checked ? "true" : "false")

            api.putFormData(
                `/api/admin/site-setting/set-banners/${editingBanner.game}/${editingBanner.bannerId}`,
                formData,
            )
                .then(() => {
                    alert("세트 배너 수정에 성공했습니다")
                    loadSetBannerList()
                    setEditingBanner(null)
                })
                .catch((error) => {
                    console.error(error)
                    alert("세트 배너 수정에 실패했습니다")
                })
        },
        [editingBanner, loadSetBannerList],
    )

    const toggleSetBannerActive = useCallback(
        async (banner: SetBannerDto, active: boolean) => {
            const formData = new FormData()
            formData.set("active", active ? "true" : "false")

            try {
                await api.putFormData(
                    `/api/admin/site-setting/set-banners/${banner.game}/${banner.bannerId}`,
                    formData,
                )
                loadSetBannerList()
            } catch (error) {
                console.error(error)
                alert("세트 배너 수정에 실패했습니다")
            }
        },
        [loadSetBannerList],
    )

    return {
        setBannerList,
        editingBanner,
        setEditingBanner,
        handleUpdateSetBanner,
        toggleSetBannerActive,
    }
}
