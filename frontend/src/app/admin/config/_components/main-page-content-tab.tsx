"use client"

import Image from "next/image"
import { ArrowDown, ArrowUp } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { resolveAssetUrl } from "@/lib/public-asset-url"
import { useMainPageContent } from "../_hooks/use-main-page-content"
import { TiptapEditor } from "./TiptapEditor"

export function MainPageContentTab() {
  const {
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
  } = useMainPageContent()

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-2">
        <div className="flex flex-row items-center gap-2">
          <CardTitle>메인 페이지 콘텐츠</CardTitle>

          {/* 추가 모달 */}
          <Dialog open={addDialogOpen} onOpenChange={setAddDialogOpen}>
            <DialogTrigger asChild>
              <Button variant="outline">콘텐츠 추가</Button>
            </DialogTrigger>
            <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
              <DialogHeader>
                <DialogTitle>콘텐츠 추가</DialogTitle>
              </DialogHeader>
              <div className="flex flex-col gap-4">
                <div className="flex flex-col gap-1">
                  <Label htmlFor="add-name">이름</Label>
                  <Input
                    id="add-name"
                    value={addName}
                    onChange={(e) => setAddName(e.target.value)}
                    placeholder="콘텐츠 이름"
                  />
                </div>
                <div className="flex flex-col gap-1">
                  <Label htmlFor="add-link">링크 URL (선택)</Label>
                  <Input
                    id="add-link"
                    value={addLink}
                    onChange={(e) => setAddLink(e.target.value)}
                    placeholder="https://example.com"
                  />
                </div>
                <div className="flex flex-col gap-1">
                  <Label>내용</Label>
                  <TiptapEditor ref={addEditorRef} />
                </div>
                <Button onClick={handleAdd} disabled={!addName.trim()}>
                  등록
                </Button>
              </div>
            </DialogContent>
          </Dialog>
        </div>

        <Button onClick={handleChangeOrder}>순서 변경</Button>
      </CardHeader>

      <CardContent>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>이름</TableHead>
              <TableHead>이미지</TableHead>
              <TableHead>순서 변경</TableHead>
              <TableHead>관리</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {contentList.map((item, index) => (
              <TableRow key={item.id}>
                <TableCell>{item.id}</TableCell>
                <TableCell>{item.name}</TableCell>
                <TableCell>
                  {item.imageUrl ? (
                    <Image
                      src={resolveAssetUrl(item.imageUrl)}
                      alt="thumbnail"
                      width={80}
                      height={60}
                      className="object-cover rounded"
                    />
                  ) : (
                    <span className="text-muted-foreground text-xs">없음</span>
                  )}
                </TableCell>
                <TableCell>
                  <div className="flex flex-row items-center gap-2">
                    <Button variant="outline" size="icon" onClick={() => moveUp(index)}>
                      <ArrowUp />
                    </Button>
                    <Button variant="outline" size="icon" onClick={() => moveDown(index)}>
                      <ArrowDown />
                    </Button>
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex flex-row gap-2">
                    {/* 수정 모달 */}
                    <Dialog
                      open={editTarget?.id === item.id}
                      onOpenChange={(open) => {
                        if (!open) closeEdit()
                      }}
                    >
                      <DialogTrigger asChild>
                        <Button variant="outline" onClick={() => openEdit(item)}>
                          수정
                        </Button>
                      </DialogTrigger>
                      <DialogContent className="max-w-3xl max-h-[90vh] overflow-y-auto">
                        <DialogHeader>
                          <DialogTitle>콘텐츠 수정</DialogTitle>
                        </DialogHeader>
                        <div className="flex flex-col gap-4">
                          <div className="flex flex-col gap-1">
                            <Label htmlFor="edit-name">이름</Label>
                            <Input
                              id="edit-name"
                              value={editName}
                              onChange={(e) => setEditName(e.target.value)}
                              placeholder="콘텐츠 이름"
                            />
                          </div>
                          <div className="flex flex-col gap-1">
                            <Label htmlFor="edit-link">링크 URL (선택)</Label>
                            <Input
                              id="edit-link"
                              value={editLink}
                              onChange={(e) => setEditLink(e.target.value)}
                              placeholder="https://example.com"
                            />
                          </div>
                          <div className="flex flex-col gap-1">
                            <Label>내용</Label>
                            {editTarget?.id === item.id && (
                              <TiptapEditor
                                ref={editEditorRef}
                                initialContent={item.content ?? ""}
                              />
                            )}
                          </div>
                          <Button onClick={handleUpdate} disabled={!editName.trim()}>
                            수정
                          </Button>
                        </div>
                      </DialogContent>
                    </Dialog>

                    <Button variant="destructive" onClick={() => handleDelete(item.id)}>
                      삭제
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </CardContent>
    </Card>
  )
}
