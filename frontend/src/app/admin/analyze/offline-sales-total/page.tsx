"use client";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  TableHead,
} from "@/components/ui/table";
import { apiClient, getApiErrorMessage } from "@/lib/api";
import { useState, useEffect } from "react";
import { Page } from "@/types/pagination";
import { AdminOfflineSalesTotalDto } from "@/types/order";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

const PAGE_SIZE = 20;

export default function OfflineSalesTotalPage() {
  const [data, setData] = useState<AdminOfflineSalesTotalDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [startDate, setStartDate] = useState<string>(() => {
    const now = new Date();
    return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-01`;
  });
  const [endDate, setEndDate] = useState<string>(
    new Date().toISOString().split("T")[0],
  );
  const [currentStartDate, setCurrentStartDate] = useState<string>(startDate);
  const [currentEndDate, setCurrentEndDate] = useState<string>(endDate);

  useEffect(() => {
    setLoading(true);
    setError(null);
    apiClient
      .get<Page<AdminOfflineSalesTotalDto>>(
        "/api/admin/analyze/offline-sales-total",
        {
          params: {
            page: currentPage - 1,
            size: PAGE_SIZE,
            startDate: currentStartDate,
            endDate: currentEndDate,
          },
        },
      )
      .then((response) => {
        setData(response.data.content ?? []);
        setTotalPages(response.data.totalPages ?? 0);
      })
      .catch((error) => {
        setError(getApiErrorMessage(error) ?? null);
      })
      .finally(() => {
        setLoading(false);
      });
  }, [currentPage, currentStartDate, currentEndDate]);

  const handleSearch = () => {
    setCurrentStartDate(startDate);
    setCurrentEndDate(endDate);
    setCurrentPage(1);
  };

  const handleDownload = () => {
    apiClient
      .post(`/api/admin/analyze/offline-sales-total/download`, null, {
        params: {
          startDate,
          endDate,
        },
        responseType: "blob",
      })
      .then((response) => {
        const blob = new Blob([response.data], {
          type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        });
        const contentDisposition = response.headers["content-disposition"] as
          | string
          | undefined;
        const fileNameMatch = contentDisposition?.match(/filename="?([^"]+)"?/);
        const fileName = fileNameMatch?.[1] ?? "offline-sales-total.xlsx";

        const downloadUrl = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = downloadUrl;
        anchor.download = fileName;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        URL.revokeObjectURL(downloadUrl);
      })
      .catch((error) => {
        setError(getApiErrorMessage(error) ?? null);
      });
  };

  return (
    <Card className="p-4">
      <CardHeader>
        <CardTitle>
          오프라인 매출 누적 현황 ({currentStartDate} ~ {currentEndDate})
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="flex flex-row items-center justify-start">
          <div className="flex flex-row items-center justify-start gap-2">
            <div className="flex flex-row text-nowrap items-center justify-start gap-2">
              <Label>시작일</Label>
              <Input
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div className="flex flex-row text-nowrap items-center justify-start gap-2">
              <Label>종료일</Label>
              <Input
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
          </div>
          <Button variant="outline" onClick={handleSearch}>
            검색
          </Button>

          <Button variant="outline" onClick={handleDownload}>
            다운로드
          </Button>
        </div>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>카테고리</TableHead>
              <TableHead>항목</TableHead>
              <TableHead>수량</TableHead>
              <TableHead>매출</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {loading ? (
              <TableRow>
                <TableCell colSpan={4} className="text-center">
                  불러오는 중...
                </TableCell>
              </TableRow>
            ) : error ? (
              <TableRow>
                <TableCell colSpan={4} className="text-center text-red-500">
                  {error}
                </TableCell>
              </TableRow>
            ) : data.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4} className="text-center">
                  데이터가 없습니다.
                </TableCell>
              </TableRow>
            ) : (
              data.map((row, index) => (
                <TableRow key={`${row.category}-${row.item}-${index}`}>
                  <TableCell>{row.category}</TableCell>
                  <TableCell>{row.item}</TableCell>
                  <TableCell className="text-right">
                    {row.quantity.toLocaleString()}
                  </TableCell>
                  <TableCell className="text-right">
                    ₩{row.amount.toLocaleString()}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
        <PaginationRangeAsync
          currentPage={currentPage}
          onPageChange={setCurrentPage}
          totalPages={totalPages}
          siblingCount={5}
        />
      </CardContent>
    </Card>
  );
}
