"use client"

import { useCallback, useState } from "react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Field, FieldGroup } from "@/components/ui/field"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Textarea } from "@/components/ui/textarea"
import type { UserManagementResponseDto } from "@/types/user"

type UserDetailDialogProps = {
  user: UserManagementResponseDto
  onSubmit: (e: React.FormEvent<HTMLFormElement>, userId: string) => Promise<void>
  onDelete?: (userId: string) => Promise<void>
}

export default function UserDetailDialog({ user, onSubmit, onDelete }: UserDetailDialogProps) {
  const [open, setOpen] = useState(false)

  const handleDelete = useCallback(async () => {
    if (!onDelete) return

    const confirmed = window.confirm(
      "이 작업은 돌이킬 수 없으며, 유저의 정보와 포인트가 손실됩니다."
    )
    if (!confirmed) return

    await onDelete(user.id)
    setOpen(false)
  }, [onDelete, user.id])

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline">상세</Button>
      </DialogTrigger>
      <DialogContent>
        <form className="flex flex-col gap-2" onSubmit={(e) => onSubmit(e, user.id)}>
          <DialogHeader>
            <DialogTitle>회원 상세 정보</DialogTitle>
            <DialogDescription>회원 상세 정보를 수정해주세요.</DialogDescription>
          </DialogHeader>
          <FieldGroup>
            <Field>
              <Label>ID(읽기 전용)</Label>
              <Input name="id" type="text" value={user.id} readOnly />
            </Field>
            <Field>
              <Label>이름(닉네임)</Label>
              <Input name="name" placeholder="이름" defaultValue={user.name} />
            </Field>
            <Field>
              <Label>비밀번호</Label>
              <Input name="password" type="password" placeholder="변경 시에만 입력" autoComplete="new-password" />
            </Field>
            <Field>
              <Label>비밀번호 확인</Label>
              <Input name="passwordConfirm" type="password" placeholder="변경 시에만 입력" autoComplete="new-password" />
            </Field>
            <Field>
              <Label>권한</Label>
              <Select name="role" defaultValue={user.role}>
                <SelectTrigger>
                  <SelectValue placeholder="권한" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ROLE_ADMIN">ADMIN</SelectItem>
                  <SelectItem value="ROLE_USER">USER</SelectItem>
                </SelectContent>
              </Select>
            </Field>
            <Field>
              <Label>상태</Label>
              <Select name="userStatus" defaultValue={user.userStatus}>
                <SelectTrigger>
                  <SelectValue placeholder="상태" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="ACTIVE">활성화</SelectItem>
                  <SelectItem value="INACTIVE">비활성화</SelectItem>
                  <SelectItem value="DELETED">탈퇴</SelectItem>
                </SelectContent>
              </Select>
            </Field>
            <Field>
              <Label>메모</Label>
              <Textarea name="userMemo" placeholder="메모" defaultValue={user.userMemo} />
            </Field>
          </FieldGroup>
          <DialogFooter>
            <Button type="submit">수정</Button>
            {onDelete && (
              <Button type="button" variant="destructive" onClick={handleDelete}>
                탈퇴
              </Button>
            )}
            <DialogClose asChild>
              <Button variant="outline">닫기</Button>
            </DialogClose>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
