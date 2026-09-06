"use client"

import { useCallback, useEffect, useState } from "react"
import { apiClient } from "@/lib/api"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import PaginationRangeAsync from "@/components/ui/pagination-range-async"
import type { Page } from "@/types/pagination"
import type { PointLogDto } from "@/types/user"
import { Search } from "lucide-react"

type PointLogSearchTableCardProps = {
  title: string
  includeSystem?: boolean
}

const PAGE_SIZE = 10

export default function PointLogSearchTableCard({ title, includeSystem = false }: PointLogSearchTableCardProps) {
  const [keywordInput, setKeywordInput] = useState("")
  const [keyword, setKeyword] = useState("")
  const [logs, setLogs] = useState<PointLogDto[]>([])
  const [currentPage, setCurrentPage] = useState(1)
  const [totalPages, setTotalPages] = useState(1)

  const fetchLogs = useCallback(() => {
    apiClient
      .get<Page<PointLogDto>>("/api/admin/user/point-log", {
        params: {
          keyword: keyword.trim() || undefined,
          includeSystem,
          page: currentPage - 1,
          size: PAGE_SIZE,
          sort: "actionDate,desc",
        },
      })
      .then((response) => {
        const body = response.data
        setLogs(body.content ?? [])
        setTotalPages(Math.max(1, body.totalPages ?? 1))
      })
  }, [currentPage, includeSystem, keyword])

  useEffect(() => {
    fetchLogs()
  }, [fetchLogs])

  const handleSearch = () => {
    setCurrentPage(1)
    setKeyword(keywordInput)
  }

  const handleSearchKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.nativeEvent.isComposing || e.key !== "Enter") return
    e.preventDefault()
    handleSearch()
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <div className="mb-4 flex flex-row gap-2">
          <Input
            className="w-full"
            type="text"
            placeholder="회원 등록명 또는 이메일 또는 메모"
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            onKeyDown={handleSearchKeyDown}
          />
          <Button className="w-fit" onClick={handleSearch}>
            <Search />
          </Button>
        </div>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ID</TableHead>
              <TableHead>회원 ID</TableHead>
              <TableHead>회원 등록명</TableHead>
              <TableHead>변경 포인트</TableHead>
              <TableHead>변경 전/후</TableHead>
              <TableHead>변경 사유</TableHead>
              <TableHead>변경 일시</TableHead>
              <TableHead>변경 사용자</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {logs.map((log) => (
              <TableRow key={log.id}>
                <TableCell>{log.id}</TableCell>
                <TableCell>{log.userId}</TableCell>
                <TableCell>{log.userName}</TableCell>
                <TableCell>{log.changedPoint}</TableCell>
                <TableCell>
                  {log.beforePoint} → {log.afterPoint}
                </TableCell>
                <TableCell>{log.changeReason}</TableCell>
                <TableCell>{new Date(log.actionDate).toLocaleString("ko-KR")}</TableCell>
                <TableCell>{log.executor}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <PaginationRangeAsync currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} />
      </CardContent>
    </Card>
  )
}

