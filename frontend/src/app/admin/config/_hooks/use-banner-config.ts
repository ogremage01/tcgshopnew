"use client"

import { useCallback, useEffect, useState } from "react"
import { api } from "@/lib/api"
import type { BannerDto, ChangeBannerOrderDto } from "@/types/banner"

export function useBannerConfig(target: string) {
  const [bannerList, setBannerList] = useState<BannerDto[]>([])
  const [addBannerModalOpen, setAddBannerModalOpen] = useState(false)

  const loadBannerList = useCallback(() => {
    api.get<BannerDto[]>(`/api/admin/site-setting/banners/list/${target}`)
      .then((response) => {
        setBannerList(response)
      })
  }, [target])

  useEffect(() => {
    loadBannerList()
  }, [loadBannerList])

  const handleAddBanner = useCallback((e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault()
    const formData = new FormData(e.target as HTMLFormElement)
    api.postFormData(`/api/admin/site-setting/banners/add/${target}`, formData)
      .then(() => {
        loadBannerList()
        setAddBannerModalOpen(false)
      })
      .catch((error) => {
        console.error(error)
        alert("배너 추가에 실패했습니다")
      })
  }, [loadBannerList, target])

  const handleDeleteBanner = useCallback((id: number) => {
    if (!window.confirm("해당 배너를 삭제하시겠습니까?")) return
    api.delete<void>(`/api/admin/site-setting/banners/${id}`)
      .then(() => {
        alert("배너 삭제에 성공했습니다")
        loadBannerList()
      })
      .catch((error) => {
        console.error(error)
        alert("배너 삭제에 실패했습니다")
      })
  }, [loadBannerList])

  const handleChangeBannerOrder = useCallback(() => {
    const confirmed = window.confirm("배너 순서 변경을 하시겠습니까?")
    if (!confirmed) return
    const dto: ChangeBannerOrderDto = {
      orderedIds: bannerList.map((banner) => banner.id),
    }
    api.put<void>(`/api/admin/site-setting/banners/order/${target}`, dto)
      .then(() => {
        alert("배너 순서 변경에 성공했습니다")
        loadBannerList()
      })
      .catch((error) => {
        console.error(error)
        alert("배너 순서 변경에 실패했습니다")
      })
  }, [bannerList, loadBannerList, target])

  const moveBannerUp = useCallback((index: number) => {
    if (index === 0) return
    setBannerList((prev) => {
      const next = [...prev]
      const temp = next[index]
      next[index] = next[index - 1]
      next[index - 1] = temp
      return next
    })
  }, [])

  const moveBannerDown = useCallback((index: number) => {
    setBannerList((prev) => {
      if (index === prev.length - 1) return prev
      const next = [...prev]
      const temp = next[index]
      next[index] = next[index + 1]
      next[index + 1] = temp
      return next
    })
  }, [])

  return {
    bannerList,
    addBannerModalOpen,
    setAddBannerModalOpen,
    handleAddBanner,
    handleDeleteBanner,
    handleChangeBannerOrder,
    moveBannerUp,
    moveBannerDown,
  }
}
