"use client"

import AppSearchBar from "@/app/_components/AppSearchBar"
import UserDetailDialog from "@/app/admin/user/_components/UserDetailDialog"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import PaginationRangeAsync from "@/components/ui/pagination-range-async"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { Page } from "@/types/pagination"
import type { UserManagementResponseDto } from "@/types/user"

type UserListCardProps = {
  userList: UserManagementResponseDto[]
  total: number
  currentPage: number
  userMemoMap: Record<string, string>
  setCurrentPage: (page: number) => void
  applyUserListFromPageBody: (
    // Pick: 필요한 필드만 선택하기 위한 기능
    body: Pick<Page<UserManagementResponseDto>, "content" | "totalPages" | "pageNumber">,
    options?: { skipNextListFetch?: boolean }
  ) => void
  handleUserDataChange: (e: React.FormEvent<HTMLFormElement>, userId: string) => Promise<void>
  handleUserPointChange: (e: React.FormEvent<HTMLFormElement>, userId: string) => Promise<void>
  handleUserMemoChange: (e: React.FormEvent<HTMLFormElement>, userId: string) => Promise<void>
  handleUserMemoInputChange: (userId: string, userMemo: string) => void
  handleUserDelete: (userId: string) => Promise<void>
}

export default function UserListCard({
  userList,
  total,
  currentPage,
  userMemoMap,
  setCurrentPage,
  applyUserListFromPageBody,
  handleUserDataChange,
  handleUserPointChange,
  handleUserMemoChange,
  handleUserMemoInputChange,
  handleUserDelete,
}: UserListCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>회원 목록</CardTitle>
      </CardHeader>
      <CardContent>
        <AppSearchBar
          placeholder="이름, 이메일, 메모 검색"
          onSearch={(value) =>
            applyUserListFromPageBody(value as Page<UserManagementResponseDto>, {
              skipNextListFetch: true,
            })
          }
          url="/api/admin/user/search"
          queryParam="keyword"
          description="회원 검색"
        />
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>이름</TableHead>
              <TableHead>이메일</TableHead>
              <TableHead>포인트</TableHead>
              <TableHead>변경 포인트/사유</TableHead>
              <TableHead>메모</TableHead>
              <TableHead>관리</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {userList?.map((user) => (
              <TableRow key={user.id}>
                <TableCell>{user.id}</TableCell>
                <TableCell>{user.name}</TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>
                  <span className="min-w-32 font-bold">{user.point}</span>
                </TableCell>
                <TableCell>
                  <div className="flex flex-row items-center gap-2">
                    <form className="flex flex-row gap-2" onSubmit={(e) => handleUserPointChange(e, user.id)}>
                      <Input className="w-24" type="number" name="point" defaultValue={0} />
                      <Input className="w-full" type="text" name="changeReason" placeholder="변경 사유" defaultValue="" />
                      <Button type="submit">변경</Button>
                    </form>
                  </div>
                </TableCell>
                <TableCell>
                  <form className="flex flex-row gap-2" onSubmit={(e) => handleUserMemoChange(e, user.id)}>
                    <Input
                      type="text"
                      value={userMemoMap[user.id] ?? ""}
                      onChange={(e) => handleUserMemoInputChange(user.id, e.target.value)}
                      name="userMemo"
                    />
                    <Button type="submit">변경</Button>
                  </form>
                </TableCell>
                <TableCell className="flex flex-row gap-2">
                  <UserDetailDialog user={user} onSubmit={handleUserDataChange} onDelete={handleUserDelete} />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <PaginationRangeAsync currentPage={currentPage} totalPages={total} onPageChange={(page) => setCurrentPage(page)} />
      </CardContent>
    </Card>
  )
}
