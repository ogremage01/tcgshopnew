"use client";

import { ArrowDown, ArrowUp } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { useMainHeader } from "../_hooks/use-main-header";

export function MainHeaderTab() {
  const {
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
  } = useMainHeader();

  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between gap-2">
        <div className="flex flex-row items-center gap-2">
          <CardTitle>헤더 메뉴</CardTitle>
          <p className="text-sm text-muted-foreground">
            Pre-order / Event Ticket 등 단일 항목 (게임 메뉴는 코드 고정)
          </p>

          <Dialog open={addDialogOpen} onOpenChange={setAddDialogOpen}>
            <DialogTrigger asChild>
              <Button variant="outline">메뉴 추가</Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>헤더 메뉴 추가</DialogTitle>
              </DialogHeader>
              <div className="flex flex-col gap-4">
                <div className="flex flex-col gap-1">
                  <Label htmlFor="add-header-title">제목</Label>
                  <Input
                    id="add-header-title"
                    value={addTitle}
                    onChange={(e) => setAddTitle(e.target.value)}
                    placeholder="메뉴명"
                  />
                </div>
                <div className="flex flex-col gap-1">
                  <Label htmlFor="add-header-url">
                    URL(host/en omitted)
                  </Label>
                  <Input
                    id="add-header-url"
                    value={addUrlString}
                    onChange={(e) => setAddUrlString(e.target.value)}
                    placeholder="/special/products?manualCategories=..."
                  />
                </div>
                <Button
                  onClick={handleAdd}
                  disabled={!addTitle.trim() || !addUrlString.trim()}
                >
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
              <TableHead>제목</TableHead>
              <TableHead>URL</TableHead>
              <TableHead className="w-24">활성</TableHead>
              <TableHead className="w-20">순서 변경</TableHead>
              <TableHead className="w-48">관리</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {headerList.map((item, index) => (
              <TableRow key={item.id}>
                <TableCell>{item.title}</TableCell>
                <TableCell className="max-w-md truncate font-mono text-xs">
                  {item.urlString}
                </TableCell>
                <TableCell>
                  <Button
                    variant={item.isActive ? "default" : "outline"}
                    size="sm"
                    onClick={() => handleToggleActive(item)}
                  >
                    {item.isActive ? "ON" : "OFF"}
                  </Button>
                </TableCell>
                <TableCell>
                  <div className="flex gap-1">
                    <Button
                      variant="outline"
                      size="icon"
                      onClick={() => moveUp(index)}
                      disabled={index === 0}
                    >
                      <ArrowUp className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="outline"
                      size="icon"
                      onClick={() => moveDown(index)}
                      disabled={index === headerList.length - 1}
                    >
                      <ArrowDown className="h-4 w-4" />
                    </Button>
                  </div>
                </TableCell>
                <TableCell>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => openEdit(item)}
                    >
                      수정
                    </Button>
                    <Button
                      variant="destructive"
                      size="sm"
                      onClick={() => handleDelete(item.id)}
                    >
                      삭제
                    </Button>
                  </div>
                </TableCell>
              </TableRow>
            ))}
            {headerList.length === 0 && (
              <TableRow>
                <TableCell
                  colSpan={5}
                  className="text-center text-muted-foreground"
                >
                  등록된 헤더 메뉴가 없습니다.
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </CardContent>

      <Dialog
        open={!!editTarget}
        onOpenChange={(open) => {
          if (!open) closeEdit();
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>헤더 메뉴 수정</DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-1">
              <Label htmlFor="edit-header-title">제목</Label>
              <Input
                id="edit-header-title"
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-1">
              <Label htmlFor="edit-header-url">URL</Label>
              <Input
                id="edit-header-url"
                value={editUrlString}
                onChange={(e) => setEditUrlString(e.target.value)}
              />
            </div>
            <div className="flex items-center gap-2">
              <Label htmlFor="edit-header-active">활성</Label>
              <Button
                id="edit-header-active"
                type="button"
                variant={editIsActive ? "default" : "outline"}
                size="sm"
                onClick={() => setEditIsActive((v) => !v)}
              >
                {editIsActive ? "ON" : "OFF"}
              </Button>
            </div>
            <Button
              onClick={handleUpdate}
              disabled={!editTitle.trim() || !editUrlString.trim()}
            >
              저장
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </Card>
  );
}
