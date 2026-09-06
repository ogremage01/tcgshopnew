"use client";

import { useState, useEffect, useCallback } from "react";
import { DownloadIcon } from "lucide-react";
import { toast } from "sonner";
import { apiClient } from "@/lib/api.client";
import {
  Card,
  CardHeader,
  CardTitle,
  CardContent,
  CardDescription,
} from "@/components/ui/card";
import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  TableHead,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import { OfflineSalesInfoDto, OfflineSalesItemDto, OfflineSalesPaymentDto } from "@/types/sales";
import { Page } from "@/types/pagination";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogTrigger,
  DialogClose,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { SelectTrigger } from "@/components/ui/select";
import { SelectValue } from "@/components/ui/select";
import { SelectContent } from "@/components/ui/select";
import { SelectItem } from "@/components/ui/select";
import {
  formatCurrency,
  formatPaymentSourceType,
} from "@/app/admin/offline/sales/_lib/payment-source-type";

const PAGE_SIZE = 10;

type LineItemDetailRow = {
  lineItem: OfflineSalesItemDto;
  discountTitle: string | null;
  discountAmount: number | null;
  rowSpan: number;
  showProductCells: boolean;
};

function buildLineItemDetailRows(
  lineItems: OfflineSalesItemDto[],
): LineItemDetailRow[] {
  return lineItems.flatMap((lineItem) => {
    const discounts = lineItem.appliedDiscounts?.length
      ? lineItem.appliedDiscounts
      : [{ title: null, amount: null }];

    return discounts.map((discount, index) => ({
      lineItem,
      discountTitle: discount.title,
      discountAmount: discount.amount,
      rowSpan: discounts.length,
      showProductCells: index === 0,
    }));
  });
}

function formatPaymentsSummary(payments: OfflineSalesPaymentDto[]): string {
  if (!payments?.length) {
    return "-";
  }
  return payments
    .map(
      (payment) =>
        `${formatPaymentSourceType(payment.sourceType)} ${formatCurrency(payment.amount)}`,
    )
    .join(" / ");
}

export default function AdminOfflineSalesListPage() {
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [startDate, setStartDate] = useState<string>("");
  const [endDate, setEndDate] = useState<string>("");
  const [offlineSalesList, setOfflineSalesList] = useState<
    OfflineSalesInfoDto[]
  >([]);
  const [refreshKey, setRefreshKey] = useState(0);
  const [sortBy, setSortBy] = useState<string>("createdAt,desc");
  const [appliedStartDate, setAppliedStartDate] = useState<string>("");
  const [appliedEndDate, setAppliedEndDate] = useState<string>("");
  const [downloading, setDownloading] = useState(false);

  const fetchOfflineSalesList = useCallback(() => {
    const baseParams = {
      page: currentPage - 1,
      size: PAGE_SIZE,
      sort: sortBy,
    };

    const request =
      appliedStartDate && appliedEndDate
        ? apiClient.get<Page<OfflineSalesInfoDto>>(
            "/api/admin/offline-data/sales/list",
            {
              params: {
                ...baseParams,
                startDate: `${appliedStartDate}T00:00:00`,
                endDate: `${appliedEndDate}T23:59:59`,
              },
            },
          )
        : apiClient.get<Page<OfflineSalesInfoDto>>(
            "/api/admin/offline-data/sales",
            { params: baseParams },
          );

    void request
      .then((response) => {
        const pageData = response.data;
        setOfflineSalesList(pageData.content);
        setTotalPages(pageData.totalPages);
      })
      .catch((error) => {
        console.error(error);
      });
  }, [currentPage, sortBy, appliedStartDate, appliedEndDate, refreshKey]);

  useEffect(() => {
    fetchOfflineSalesList();
  }, [fetchOfflineSalesList]);

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const handleDownloadOfflineSales = () => {
    if (downloading) {
      toast.warning("이미 다운로드가 진행 중입니다.");
      return;
    }
    if (!startDate || !endDate) {
      toast.error("시작일과 종료일을 선택해 주세요.");
      return;
    }
    if (startDate > endDate) {
      toast.error("시작일은 종료일보다 이후일 수 없습니다.");
      return;
    }
    setDownloading(true);
    void apiClient
      .post<string>("/api/admin/offline-data/sales/download", null, {
        params: {
          startDate: `${startDate}T00:00:00`,
          endDate: `${endDate}T23:59:59`,
        },
        validateStatus: (status) =>
          (status >= 200 && status < 300) || status === 409,
      })
      .then((response) => {
        if (response.status === 409) {
          toast.warning("이미 다운로드가 진행 중입니다.");
          return;
        }
        toast.success(
          "오프라인 매출 다운로드를 시작했습니다. 잠시 후 목록을 새로고침해 주세요.",
        );
        setCurrentPage(1);
        setRefreshKey((key) => key + 1);
      })
      .catch((error) => {
        console.error(error);
        toast.error("오프라인 매출 다운로드에 실패했습니다.");
      })
      .finally(() => {
        setDownloading(false);
      });
  };

  const handleSearchOfflineSalesList = () => {
    if (!startDate || !endDate) {
      alert("시작일과 종료일을 선택해 주세요.");
      return;
    }
    if (startDate > endDate) {
      alert("시작일은 종료일보다 이후일 수 없습니다.");
      return;
    }
    setAppliedStartDate(startDate);
    setAppliedEndDate(endDate);
    setCurrentPage(1);
  };

  const handleSortByChange = (nextSortBy: string) => {
    if (startDate && endDate && startDate <= endDate) {
      setAppliedStartDate(startDate);
      setAppliedEndDate(endDate);
    }
    setSortBy(nextSortBy);
    setCurrentPage(1);
  };

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>오프라인 매출 다운로드</CardTitle>
          <CardDescription>
            토스 POS에서 기간별 매출 데이터를 가져옵니다. 요청 후 백그라운드에서
            처리되며, 완료까지 다소 시간이 걸릴 수 있습니다.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-4 sm:flex-row sm:items-end">
            <div className="flex flex-row gap-4">
              <div className="space-y-2">
                <Label htmlFor="startDate">시작일</Label>
                <Input
                  id="startDate"
                  type="date"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-fit"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="endDate">종료일</Label>
                <Input
                  id="endDate"
                  type="date"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                  className="w-fit"
                />
              </div>
            </div>
            <Button
              onClick={handleDownloadOfflineSales}
              disabled={downloading}
              className="sm:w-auto"
            >
              <DownloadIcon className="h-4 w-4" />
              {downloading ? "다운로드 중..." : "다운로드"}
            </Button>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>오프라인 매출 목록</CardTitle>
          <div className="flex flex-row gap-4 items-end">
            <div className="space-y-2">
              <Label htmlFor="startDate">시작일</Label>
              <Input
                id="startDate"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="endDate">종료일</Label>
              <Input
                id="endDate"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
            <div className="flex flex-col gap-4">
              <Label htmlFor="sortBy">정렬</Label>
              <Select value={sortBy} onValueChange={handleSortByChange}>
                <SelectTrigger className="w-[180px]">
                  <SelectValue placeholder="정렬" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="createdAt,asc">주문일 오름차순</SelectItem>
                  <SelectItem value="createdAt,desc">
                    주문일 내림차순
                  </SelectItem>
                </SelectContent>
              </Select>
            </div>
            <Button onClick={handleSearchOfflineSalesList}>조회</Button>
          </div>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>주문일</TableHead>
                <TableHead>주문번호</TableHead>
                <TableHead>주문상태</TableHead>
                <TableHead className="text-right">주문금액</TableHead>
                <TableHead className="text-right">할인금액</TableHead>
                <TableHead className="text-right">세액</TableHead>
                <TableHead className="text-right">공급가액</TableHead>
                <TableHead className="text-right">면세금액</TableHead>
                <TableHead className="text-right">총액</TableHead>
                <TableHead>결제수단</TableHead>
                <TableHead>상세</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {offlineSalesList.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={11}
                    className="h-24 text-center text-muted-foreground"
                  >
                    조회된 매출 데이터가 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                offlineSalesList.map((offlineSales) => (
                  <TableRow key={offlineSales.id}>
                    <TableCell>{offlineSales.createdAt}</TableCell>
                    <TableCell>{offlineSales.orderNumber}</TableCell>
                    <TableCell>{offlineSales.orderState}</TableCell>
                    <TableCell className="text-right">
                      ₩{offlineSales.listPrice.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{offlineSales.discountAmount.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{offlineSales.taxAmount.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{offlineSales.supplyAmount.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{offlineSales.taxExemptAmount.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-right">
                      ₩{offlineSales.totalAmount.toLocaleString()}
                    </TableCell>
                    <TableCell className="max-w-[220px] text-sm">
                      {formatPaymentsSummary(offlineSales.payments ?? [])}
                    </TableCell>
                    <TableCell>
                      <Dialog>
                        <DialogTrigger asChild>
                          <Button variant="outline" size="sm">
                            상세
                          </Button>
                        </DialogTrigger>
                        <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-4xl">
                          <DialogHeader>
                            <DialogTitle>오프라인 매출 상세</DialogTitle>
                          </DialogHeader>
                          <div className="space-y-6">
                            <div>
                              <h3 className="mb-2 text-sm font-medium">
                                결제 수단
                              </h3>
                              {(offlineSales.payments ?? []).length === 0 ? (
                                <p className="text-sm text-muted-foreground">
                                  결제 수단 정보가 없습니다.
                                </p>
                              ) : (
                                <Table>
                                  <TableHeader>
                                    <TableRow>
                                      <TableHead>결제 수단</TableHead>
                                      <TableHead className="text-right">
                                        결제 금액
                                      </TableHead>
                                      <TableHead className="text-right">
                                        세액
                                      </TableHead>
                                      <TableHead className="text-right">
                                        공급가액
                                      </TableHead>
                                      <TableHead className="text-right">
                                        면세 금액
                                      </TableHead>
                                    </TableRow>
                                  </TableHeader>
                                  <TableBody>
                                    {(offlineSales.payments ?? []).map(
                                      (payment, paymentIndex) => (
                                        <TableRow
                                          key={`${payment.sourceType ?? "payment"}-${paymentIndex}`}
                                        >
                                          <TableCell>
                                            {formatPaymentSourceType(
                                              payment.sourceType,
                                            )}
                                          </TableCell>
                                          <TableCell className="text-right">
                                            {formatCurrency(payment.amount)}
                                          </TableCell>
                                          <TableCell className="text-right">
                                            {formatCurrency(payment.taxAmount)}
                                          </TableCell>
                                          <TableCell className="text-right">
                                            {formatCurrency(
                                              payment.supplyAmount,
                                            )}
                                          </TableCell>
                                          <TableCell className="text-right">
                                            {formatCurrency(
                                              payment.taxExemptAmount,
                                            )}
                                          </TableCell>
                                        </TableRow>
                                      ),
                                    )}
                                  </TableBody>
                                </Table>
                              )}
                            </div>
                            <div>
                              <h3 className="mb-2 text-sm font-medium">
                                상품 내역
                              </h3>
                              <Table>
                            <TableHeader>
                              <TableRow>
                                <TableHead>상품명</TableHead>
                                <TableHead>카테고리</TableHead>
                                <TableHead>할인적용</TableHead>
                                <TableHead>할인금액</TableHead>
                                <TableHead className="text-right">
                                  수량
                                </TableHead>
                                <TableHead className="text-right">
                                  가격
                                </TableHead>
                              </TableRow>
                            </TableHeader>
                            <TableBody>
                              {buildLineItemDetailRows(
                                offlineSales.lineItems,
                              ).map((row, index) => (
                                <TableRow key={`${row.lineItem.id}-${index}`}>
                                  {row.showProductCells ? (
                                    <>
                                      <TableCell rowSpan={row.rowSpan}>
                                        {row.lineItem.title}
                                      </TableCell>
                                      <TableCell rowSpan={row.rowSpan}>
                                        {row.lineItem.category ?? "-"}
                                      </TableCell>
                                    </>
                                  ) : null}
                                  <TableCell>
                                    {row.discountTitle ?? "-"}
                                  </TableCell>
                                  <TableCell className="text-right">
                                    {row.discountAmount != null
                                      ? `₩${row.discountAmount.toLocaleString()}`
                                      : "-"}
                                  </TableCell>
                                  {row.showProductCells ? (
                                    <>
                                      <TableCell
                                        rowSpan={row.rowSpan}
                                        className="text-right"
                                      >
                                        {row.lineItem.quantity.toLocaleString()}
                                      </TableCell>
                                      <TableCell
                                        rowSpan={row.rowSpan}
                                        className="text-right"
                                      >
                                        ₩
                                        {row.lineItem.priceValue.toLocaleString()}
                                      </TableCell>
                                    </>
                                  ) : null}
                                </TableRow>
                              ))}
                            </TableBody>
                          </Table>
                            </div>
                          </div>
                          <DialogFooter>
                            <DialogClose asChild>
                              <Button>닫기</Button>
                            </DialogClose>
                          </DialogFooter>
                        </DialogContent>
                      </Dialog>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
          <PaginationRangeAsync
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={handlePageChange}
          />
        </CardContent>
      </Card>
    </div>
  );
}
