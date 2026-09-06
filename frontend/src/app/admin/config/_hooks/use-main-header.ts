"use client"

import { useCallback, useEffect, useState } from "react"
import { api } from "@/lib/api"
import type {
  ChangeMainHeaderOrderDto,
  MainHeaderDto,
  SaveMainHeaderDto,
} from "@/types/mainHeader"

export function useMainHeader() {
  const [headerList, setHeaderList] = useState<MainHeaderDto[]>([])
  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<MainHeaderDto | null>(null)
  const [addTitle, setAddTitle] = useState("")
  const [addUrlString, setAddUrlString] = useState("")
  const [editTitle, setEditTitle] = useState("")
  const [editUrlString, setEditUrlString] = useState("")
  const [editIsActive, setEditIsActive] = useState(true)

  const loadHeaderList = useCallback(() => {
    api
      .get<MainHeaderDto[]>("/api/admin/site-setting/main-header/list")
      .then(setHeaderList)
      .catch((e) => {
        console.error(e)
        alert("헤더 메뉴 목록을 불러오지 못했습니다")
      })
  }, [])

  useEffect(() => {
    loadHeaderList()
  }, [loadHeaderList])

  const openEdit = useCallback((item: MainHeaderDto) => {
    setEditTitle(item.title)
    setEditUrlString(item.urlString)
    setEditIsActive(item.isActive)
    setEditTarget(item)
  }, [])

  const closeEdit = useCallback(() => {
    setEditTarget(null)
    setEditTitle("")
    setEditUrlString("")
    setEditIsActive(true)
  }, [])

  const handleAdd = useCallback(async () => {
    if (!addTitle.trim() || !addUrlString.trim()) return
    try {
      const dto: SaveMainHeaderDto = {
        title: addTitle.trim(),
        urlString: addUrlString.trim(),
        isActive: true,
      }
      await api.post("/api/admin/site-setting/main-header/add", dto)
      setAddDialogOpen(false)
      setAddTitle("")
      setAddUrlString("")
      loadHeaderList()
    } catch (e) {
      console.error(e)
      alert("헤더 메뉴 추가에 실패했습니다")
    }
  }, [addTitle, addUrlString, loadHeaderList])

  const handleUpdate = useCallback(async () => {
    if (!editTarget || !editTitle.trim() || !editUrlString.trim()) return
    try {
      const dto: SaveMainHeaderDto = {
        title: editTitle.trim(),
        urlString: editUrlString.trim(),
        isActive: editIsActive,
      }
      await api.put(`/api/admin/site-setting/main-header/${editTarget.id}`, dto)
      closeEdit()
      loadHeaderList()
    } catch (e) {
      console.error(e)
      alert("헤더 메뉴 수정에 실패했습니다")
    }
  }, [editTarget, editTitle, editUrlString, editIsActive, closeEdit, loadHeaderList])

  const handleDelete = useCallback(
    (id: number) => {
      if (!window.confirm("해당 헤더 메뉴를 삭제하시겠습니까?")) return
      api
        .delete(`/api/admin/site-setting/main-header/${id}`)
        .then(() => loadHeaderList())
        .catch((e) => {
          console.error(e)
          alert("헤더 메뉴 삭제에 실패했습니다")
        })
    },
    [loadHeaderList],
  )

  const handleToggleActive = useCallback(
    async (item: MainHeaderDto) => {
      try {
        const dto: SaveMainHeaderDto = {
          title: item.title,
          urlString: item.urlString,
          isActive: !item.isActive,
        }
        await api.put(`/api/admin/site-setting/main-header/${item.id}`, dto)
        loadHeaderList()
      } catch (e) {
        console.error(e)
        alert("활성 상태 변경에 실패했습니다")
      }
    },
    [loadHeaderList],
  )

  const handleChangeOrder = useCallback(() => {
    if (!window.confirm("현재 순서로 저장하시겠습니까?")) return
    const dto: ChangeMainHeaderOrderDto = {
      orderedIds: headerList.map((h) => h.id),
    }
    api
      .put("/api/admin/site-setting/main-header/order", dto)
      .then(() => {
        alert("순서 변경에 성공했습니다")
        loadHeaderList()
      })
      .catch((e) => {
        console.error(e)
        alert("순서 변경에 실패했습니다")
      })
  }, [headerList, loadHeaderList])

  const moveUp = useCallback((index: number) => {
    if (index === 0) return
    setHeaderList((prev) => {
      const next = [...prev]
      ;[next[index - 1], next[index]] = [next[index], next[index - 1]]
      return next
    })
  }, [])

  const moveDown = useCallback((index: number) => {
    setHeaderList((prev) => {
      if (index === prev.length - 1) return prev
      const next = [...prev]
      ;[next[index], next[index + 1]] = [next[index + 1], next[index]]
      return next
    })
  }, [])

  return {
    headerList,
    addDialogOpen,
    setAddDialogOpen,
    editTarget,
    addTitle,
    setAddTitle,
    addUrlString,
    setAddUrlString,
    editTitle,
    setEditTitle,
    editUrlString,
    setEditUrlString,
    editIsActive,
    setEditIsActive,
    openEdit,
    closeEdit,
    handleAdd,
    handleUpdate,
    handleDelete,
    handleToggleActive,
    handleChangeOrder,
    moveUp,
    moveDown,
  }
}
