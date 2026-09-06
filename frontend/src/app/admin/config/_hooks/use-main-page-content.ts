"use client"

import { useCallback, useEffect, useRef, useState } from "react"
import { api } from "@/lib/api"
import type { TiptapEditorHandle } from "../_components/TiptapEditor"
import type {
  ChangeMainPageContentOrderDto,
  MainPageContentDto,
  SaveMainPageContentDto,
} from "@/types/mainPageContent"

export function useMainPageContent() {
  const [contentList, setContentList] = useState<MainPageContentDto[]>([])
  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [editTarget, setEditTarget] = useState<MainPageContentDto | null>(null)
  const [addName, setAddName] = useState("")
  const [addLink, setAddLink] = useState("")
  const [editName, setEditName] = useState("")
  const [editLink, setEditLink] = useState("")
  const addEditorRef = useRef<TiptapEditorHandle | null>(null)
  const editEditorRef = useRef<TiptapEditorHandle | null>(null)

  const loadContentList = useCallback(() => {
    api.get<MainPageContentDto[]>("/api/admin/site-setting/main-page-content/list").then(setContentList)
  }, [])

  useEffect(() => {
    loadContentList()
  }, [loadContentList])

  const openEdit = useCallback((item: MainPageContentDto) => {
    setEditName(item.name)
    setEditLink(item.link ?? "")
    setEditTarget(item)
  }, [])

  const closeEdit = useCallback(() => {
    setEditTarget(null)
    setEditName("")
    setEditLink("")
  }, [])

  const handleAdd = useCallback(async () => {
    if (!addEditorRef.current) return
    try {
      const html = addEditorRef.current.getHTML()
      const dto: SaveMainPageContentDto = {
        name: addName,
        content: html,
        link: addLink,
      }
      await api.post("/api/admin/site-setting/main-page-content/add", dto)
      setAddDialogOpen(false)
      setAddName("")
      setAddLink("")
      loadContentList()
    } catch (e) {
      console.error(e)
      alert("콘텐츠 추가에 실패했습니다")
    }
  }, [addName, addLink, loadContentList])

  const handleUpdate = useCallback(async () => {
    if (!editTarget || !editEditorRef.current) return
    try {
      const html = editEditorRef.current.getHTML()
      const dto: SaveMainPageContentDto = {
        name: editName,
        content: html,
        link: editLink,
      }
      await api.put(`/api/admin/site-setting/main-page-content/${editTarget.id}`, dto)
      closeEdit()
      loadContentList()
    } catch (e) {
      console.error(e)
      alert("콘텐츠 수정에 실패했습니다")
    }
  }, [editTarget, editName, editLink, closeEdit, loadContentList])

  const handleDelete = useCallback(
    (id: number) => {
      if (!window.confirm("해당 콘텐츠를 삭제하시겠습니까?")) return
      api
        .delete(`/api/admin/site-setting/main-page-content/${id}`)
        .then(() => loadContentList())
        .catch((e) => {
          console.error(e)
          alert("콘텐츠 삭제에 실패했습니다")
        })
    },
    [loadContentList],
  )

  const handleChangeOrder = useCallback(() => {
    if (!window.confirm("현재 순서로 저장하시겠습니까?")) return
    const dto: ChangeMainPageContentOrderDto = {
      orderedIds: contentList.map((c) => c.id),
    }
    api
      .put("/api/admin/site-setting/main-page-content/order", dto)
      .then(() => {
        alert("순서 변경에 성공했습니다")
        loadContentList()
      })
      .catch((e) => {
        console.error(e)
        alert("순서 변경에 실패했습니다")
      })
  }, [contentList, loadContentList])

  const moveUp = useCallback((index: number) => {
    if (index === 0) return
    setContentList((prev) => {
      const next = [...prev]
      ;[next[index - 1], next[index]] = [next[index], next[index - 1]]
      return next
    })
  }, [])

  const moveDown = useCallback((index: number) => {
    setContentList((prev) => {
      if (index === prev.length - 1) return prev
      const next = [...prev]
      ;[next[index], next[index + 1]] = [next[index + 1], next[index]]
      return next
    })
  }, [])

  return {
    contentList,
    addDialogOpen,
    setAddDialogOpen,
    editTarget,
    addName,
    setAddName,
    addLink,
    setAddLink,
    editName,
    setEditName,
    editLink,
    setEditLink,
    addEditorRef,
    editEditorRef,
    openEdit,
    closeEdit,
    handleAdd,
    handleUpdate,
    handleDelete,
    handleChangeOrder,
    moveUp,
    moveDown,
  }
}
