import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from "@/components/ui/table"
import { Skeleton } from "@/components/ui/skeleton"
import { formatDateTime } from "@/utils/date"
import PaginationRangeAsync from "@/components/ui/pagination-range-async"
import { SyncLogDto } from "@/types/syncLog"
import { useAdminProductSyncLog } from "@/app/admin/_hooks/product-config/useAdminProductSyncLog"
export default function TCGPMetadataSyncLogTab() {
    const { loadingSyncLog, currentPage, totalPages, setCurrentPage, syncLog } = useAdminProductSyncLog()
    return (<>
        <Card>
            <CardHeader>
                <CardTitle>시스템 로그</CardTitle>
                <Separator />
            </CardHeader>
            <CardContent>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>ID</TableHead>
                            <TableHead>대상</TableHead>
                            <TableHead>소스</TableHead>
                            <TableHead>시작 시간</TableHead>
                            <TableHead>종료 시간</TableHead>
                            <TableHead>실행 시간</TableHead>
                            <TableHead>성공 여부</TableHead>
                            <TableHead>메시지</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {loadingSyncLog ? (
                            [1, 2, 3, 4, 5].map((i) => (
                                <TableRow key={i}>
                                    {[1, 2, 3, 4, 5, 6, 7, 8].map((j) => (
                                        <TableCell key={j}><Skeleton className="h-5 w-full min-w-[3rem]" /></TableCell>
                                    ))}
                                </TableRow>
                            ))
                        ) : (
                            syncLog.map((log: SyncLogDto) => (
                                <TableRow key={log.id}>
                                    <TableCell>{log.id}</TableCell>
                                    <TableCell>{log.syncTarget}</TableCell>
                                    <TableCell>{log.syncSource}</TableCell>
                                    <TableCell>{formatDateTime(log.startTime)}</TableCell>
                                    <TableCell>{formatDateTime(log.endTime)}</TableCell>
                                    <TableCell>{Math.floor(log.proceedingTime / 3600)}시간 {Math.floor((log.proceedingTime % 3600) / 60)}분 {log.proceedingTime % 60}초</TableCell>
                                    <TableCell><span className={`font-bold ${log.result === "success" ? "text-green-500" : log.result === "partial_success" ? "text-yellow-500" : "text-red-500"}`}> {log.result === "success" ? "성공" : log.result === "partial_success" ? "부분 성공" : "실패"}</span></TableCell>
                                    <TableCell>{log.message}</TableCell>
                                </TableRow>
                            ))
                        )}
                    </TableBody>
                </Table>
                <PaginationRangeAsync currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} />
            </CardContent>
        </Card>
    </>
    )
}