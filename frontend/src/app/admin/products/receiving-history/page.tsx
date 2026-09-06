"use client";

import { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableHeader,
  TableRow,
  TableHead,
  TableBody,
  TableCell,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import { apiClient } from "@/lib/api.client";
import type { OfflineProductReceivingHistoryDto } from "@/types/product";
import type { Page } from "@/types/pagination";
import { formatDateTime } from "@/utils/date";
import { formatDeductionCount } from "@/app/admin/analyze/_components/report/report-table-styles";

const PAGE_SIZE = 10;

export default function AdminProductsReceivingHistoryPage() {
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [histories, setHistories] = useState<
    OfflineProductReceivingHistoryDto[]
  >([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [selectedDetail, setSelectedDetail] =
    useState<OfflineProductReceivingHistoryDto | null>(null);

  useEffect(() => {
    void apiClient
      .get("/api/admin/offline-data/product/receiving", {
        params: {
          page: currentPage - 1,
          size: PAGE_SIZE,
        },
      })
      .then((response) => {
        const pageData =
          response.data as Page<OfflineProductReceivingHistoryDto>;
        setHistories(pageData.content);
        setTotalPages(pageData.totalPages || 1);
      })
      .catch((error) => {
        console.error(error);
        setHistories([]);
      });
  }, [currentPage]);

  useEffect(() => {
    if (selectedId == null) {
      setSelectedDetail(null);
      return;
    }

    void apiClient
      .get(`/api/admin/offline-data/product/receiving/${selectedId}`)
      .then((response) => {
        setSelectedDetail(response.data as OfflineProductReceivingHistoryDto);
      })
      .catch((error) => {
        console.error(error);
        setSelectedDetail(null);
      });
  }, [selectedId]);

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>입고 내역</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>일시</TableHead>
                <TableHead>담당자</TableHead>
                <TableHead>품목 수</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {histories.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={4}
                    className="text-center text-muted-foreground"
                  >
                    입고 내역이 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                histories.map((history) => (
                  <TableRow
                    key={history.id}
                    className="cursor-pointer"
                    onClick={() => setSelectedId(history.id)}
                  >
                    <TableCell>{history.id}</TableCell>
                    <TableCell>{formatDateTime(history.createdAt)}</TableCell>
                    <TableCell>{history.receivingManager}</TableCell>
                    <TableCell>{history.itemCount}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
          <PaginationRangeAsync
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={setCurrentPage}
          />
        </CardContent>
      </Card>

      <Dialog
        open={selectedId != null}
        onOpenChange={(open) => {
          if (!open) setSelectedId(null);
        }}
      >
        <DialogContent className="max-w-2xl max-h-[80vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {selectedDetail ? `입고 상세 #${selectedDetail.id}` : "입고 상세"}
            </DialogTitle>
            {selectedDetail && (
              <DialogDescription>
                {formatDateTime(selectedDetail.createdAt)} ·{" "}
                {selectedDetail.receivingManager}
              </DialogDescription>
            )}
          </DialogHeader>
          {!selectedDetail ? (
            <p className="text-sm text-muted-foreground">불러오는 중...</p>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>제품 ID</TableHead>
                  <TableHead>품목명</TableHead>
                  <TableHead>수량</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {selectedDetail.items.map((item) => (
                  <TableRow key={item.id}>
                    <TableCell>
                      {item.tossProductId ?? item.productId}
                    </TableCell>
                    <TableCell>{item.title ?? "-"}</TableCell>
                    <TableCell
                      className={
                        item.receivingQuantity < 0 ? "text-red-500" : undefined
                      }
                    >
                      {formatDeductionCount(item.receivingQuantity)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
