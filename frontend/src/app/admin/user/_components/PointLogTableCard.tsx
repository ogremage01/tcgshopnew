"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import type { PointLogDto } from "@/types/user"

type PointLogTableCardProps = {
  title: string
  logs: PointLogDto[]
}

export default function PointLogTableCard({ title, logs }: PointLogTableCardProps) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
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
      </CardContent>
    </Card>
  )
}
