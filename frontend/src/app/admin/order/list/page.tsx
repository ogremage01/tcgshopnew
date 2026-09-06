"use client";

import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import {
  Table,
  TableHead,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
} from "@/components/ui/table";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import { useState, useEffect } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { SelectSeparator } from "@/components/ui/select";
import { api } from "@/lib/api";
import OrderSimpleInfoRow from "./_components/OrderSimpleInfoRow";
import {
  AdminOrderSearchRequest,
  AdminOrderSimpleDto,
  AdminSimpleOrderRequest,
  AdminTodayOrderSummaryDto,
  AdminTotalOrderSummaryDto,
} from "@/types/order";
import { Page } from "@/types/pagination";
import { toast } from "sonner";
import {
  ADMIN_ORDER_STATUS_FILTER_MAP,
  ADMIN_ORDER_STATUS_FILTER_OPTIONS,
  type AdminOrderStatusFilterKey,
} from "@/lib/order-status";

type ListMode = "filter" | "search";

type OrderListFetchOverrides = Partial<{
  page: number;
  startDate: string;
  endDate: string;
  orderStatus: AdminOrderStatusFilterKey;
}>;

type OrderSearchFetchOverrides = Partial<{
  page: number;
  keyword: string;
}>;

/**
 * input[type=date] 값(YYYY-MM-DD)을 브라우저 로컬 달력으로 해석.
 * `new Date("YYYY-MM-DD")`는 UTC 자정이라 한국 등에서 날짜가 하루 어긋날 수 있음.
 */
function parseLocalYmdParts(value: string): [number, number, number] | null {
  const v = value.trim();
  if (!v) return null;
  const parts = v.split("-").map((p) => parseInt(p, 10));
  if (parts.length !== 3 || parts.some((n) => Number.isNaN(n))) return null;
  return [parts[0], parts[1], parts[2]];
}

/** 시작일: 해당 일 00:00:00.000 (로컬) */
function startDateFromInput(value: string): Date | null {
  const ymd = parseLocalYmdParts(value);
  if (!ymd) return null;
  const [y, m, d] = ymd;
  const dt = new Date(y, m - 1, d, 0, 0, 0, 0);
  return Number.isNaN(dt.getTime()) ? null : dt;
}

/** 종료일: 해당 일 23:59:59.999 (로컬). 같은 날만 조회할 때 포함되도록. */
function endDateFromInput(value: string): Date | null {
  const ymd = parseLocalYmdParts(value);
  if (!ymd) return null;
  const [y, m, d] = ymd;
  const dt = new Date(y, m - 1, d, 23, 59, 59, 999);
  return Number.isNaN(dt.getTime()) ? null : dt;
}

/** JSON에 넣을 로컬 달력 시각 (타임존 접미사 없음) → Spring `LocalDateTime`과 숫자만 일치 */
function toLocalDateTimeIsoString(d: Date): string {
  const pad = (n: number, w = 2) => String(n).padStart(w, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}`;
}

/** input[type=date]용 로컬 YYYY-MM-DD. `toISOString()`은 UTC라 한국에서 하루 어긋날 수 있음. */
function toLocalYmd(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** 어제~오늘. 당일만 보면 전날 퇴근 이후 주문이 빠진다. */
function defaultOrderDateRange(): { startDate: string; endDate: string } {
  const today = new Date();
  const yesterday = new Date(
    today.getFullYear(),
    today.getMonth(),
    today.getDate() - 1,
  );
  return { startDate: toLocalYmd(yesterday), endDate: toLocalYmd(today) };
}

export default function OrderListPage() {
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(0);
  const [startDate, setStartDate] = useState(
    () => defaultOrderDateRange().startDate,
  );
  const [endDate, setEndDate] = useState(() => defaultOrderDateRange().endDate);
  const [orderList, setOrderList] = useState<AdminOrderSimpleDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [orderStatus, setOrderStatus] =
    useState<AdminOrderStatusFilterKey>("ACTIVE");
  const [listMode, setListMode] = useState<ListMode>("filter");
  const [keyword, setKeyword] = useState("");
  const [appliedKeyword, setAppliedKeyword] = useState("");
  const [todayOrderInfo, setTodayOrderInfo] =
    useState<AdminTodayOrderSummaryDto>({
      totalOrderCount: 0,
      totalOrderAmount: 0,
      orderPendingCount: 0,
      orderCompletedCount: 0,
      orderReceivedCount: 0,
    });
  const [totalOrderInfo, setTotalOrderInfo] =
    useState<AdminTotalOrderSummaryDto>({
      orderPendingCount: 0,
      orderCompletedCount: 0,
      orderReceivedCount: 0,
    });
  const handleOrderList = (overrides?: OrderListFetchOverrides) => {
    const page = overrides?.page ?? currentPage;
    const start = overrides?.startDate ?? startDate;
    const end = overrides?.endDate ?? endDate;
    const statusKey = overrides?.orderStatus ?? orderStatus;
    const orderStatusList = ADMIN_ORDER_STATUS_FILTER_MAP[statusKey];

    const startDt = startDateFromInput(start);
    const endDt = endDateFromInput(end);
    const request: AdminSimpleOrderRequest = {
      pageParam: {
        page: page - 1,
        size: 12,
        sort: ["id,desc"],
      },
      startDate: startDt ? toLocalDateTimeIsoString(startDt) : null,
      endDate: endDt ? toLocalDateTimeIsoString(endDt) : null,
      orderStatusList: [...orderStatusList],
    };
    api
      .post<Page<AdminOrderSimpleDto>>(`/api/admin/orders/list`, request)
      .then((res) => {
        console.log(res);
        setOrderList(res.content ?? []);
        setTotalPages(res.totalPages ?? 0);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
        toast.error("주문 목록 조회 실패");
      });
  };

  const handleOrderSearch = (overrides?: OrderSearchFetchOverrides) => {
    const page = overrides?.page ?? currentPage;
    const searchKeyword = (overrides?.keyword ?? appliedKeyword).trim();
    const request: AdminOrderSearchRequest = {
      pageParam: {
        page: page - 1,
        size: 12,
        sort: ["id,desc"],
      },
      keyword: searchKeyword,
    };
    api
      .post<Page<AdminOrderSimpleDto>>(`/api/admin/orders/search`, request)
      .then((res) => {
        setOrderList(res.content ?? []);
        setTotalPages(res.totalPages ?? 0);
        setLoading(false);
      })
      .catch((err) => {
        console.error(err);
        setLoading(false);
        toast.error("주문 검색 실패");
      });
  };

  const handlePageChange = (page: number) => {
    setLoading(true);
    setCurrentPage(page);
  };

  const handleSearch = () => {
    const trimmed = keyword.trim();
    if (!trimmed) {
      toast.error("주문 번호 또는 고객명을 입력하세요");
      return;
    }
    setListMode("search");
    setAppliedKeyword(trimmed);
    setLoading(true);
    if (currentPage !== 1) {
      setCurrentPage(1);
      return;
    }
    handleOrderSearch({ page: 1, keyword: trimmed });
  };

  const handleFilterApply = () => {
    setListMode("filter");
    setLoading(true);
    if (listMode === "search") {
      if (currentPage !== 1) {
        setCurrentPage(1);
        return;
      }
      handleOrderList({ page: 1 });
      return;
    }
    handleOrderList();
  };

  const handleOrderListFilterReset = () => {
    const { startDate: defaultStart, endDate: defaultEnd } =
      defaultOrderDateRange();
    setListMode("filter");
    setKeyword("");
    setAppliedKeyword("");
    setOrderStatus("ACTIVE");
    setStartDate(defaultStart);
    setEndDate(defaultEnd);
    setCurrentPage(1);
    setLoading(true);
    handleOrderList({
      page: 1,
      startDate: defaultStart,
      endDate: defaultEnd,
      orderStatus: "ACTIVE",
    });
  };
  useEffect(() => {
    // api.get<AdminTodayOrderSummaryDto>(`/api/admin/orders/today-summary`)
    //     .then((res) => {
    //         setTodayOrderInfo(res);
    //     })
    //     .catch((err) => {
    //         console.error(err);
    //         toast.error("오늘 주문 현황 조회 실패");
    //     });
    api
      .get<AdminTotalOrderSummaryDto>(`/api/admin/orders/total-summary`)
      .then((res) => {
        setTotalOrderInfo(res);
      })
      .catch((err) => {
        console.error(err);
        toast.error("총 주문 현황 조회 실패");
      });
  }, []);

  useEffect(() => {
    if (listMode === "search") {
      handleOrderSearch();
    } else {
      handleOrderList();
    }
    // 의도: 페이지 번호가 바뀔 때만 재조회(필터는「필터 적용」, 검색은「조회」로 반영)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentPage]);
  if (loading) {
    return <div>Loading...</div>;
  }
  return (
    <>
      <div className="flex flex-col gap-4 my-4">
        <h1 className="text-2xl font-bold">주문 목록</h1>
        <span className="text-sm text-gray-500">주문을 관리합니다.</span>
        <Card>
          <CardHeader>
            <CardTitle>주문 현황</CardTitle>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader></TableHeader>
              <TableBody className="border-t">
                <TableRow>
                  <TableHead>결제 확인중</TableHead>
                  <TableCell>{totalOrderInfo.orderPendingCount}</TableCell>
                  <TableHead>주문 완료</TableHead>
                  <TableCell>{totalOrderInfo.orderCompletedCount}</TableCell>
                  <TableHead>접수 완료</TableHead>
                  <TableCell>{totalOrderInfo.orderReceivedCount}</TableCell>
                </TableRow>
              </TableBody>
            </Table>
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>주문 필터</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="flex flex-row justify-between gap-4">
              <div className="flex flex-row items-center gap-4">
                <Label className="text-nowrap">주문 기간 범위</Label>
                <Input
                  type="date"
                  className="w-fit"
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                />
                <Input
                  type="date"
                  className="w-fit"
                  value={endDate}
                  onChange={(e) => setEndDate(e.target.value)}
                />
                <div className="flex flex-row gap-4">
                  <div className="flex flex-row items-center gap-2">
                    <Label className="text-nowrap">주문 상태</Label>
                    <Select
                      value={orderStatus}
                      onValueChange={(v) =>
                        setOrderStatus(v as AdminOrderStatusFilterKey)
                      }
                    >
                      <SelectTrigger className="w-60">
                        <SelectValue placeholder="주문 상태" />
                        <SelectContent>
                          {ADMIN_ORDER_STATUS_FILTER_OPTIONS.map(
                            ({ value, label }) => (
                              <SelectItem key={value} value={value}>
                                {label}
                              </SelectItem>
                            ),
                          )}
                          <SelectSeparator />
                          <SelectItem value="ALL">전체</SelectItem>
                        </SelectContent>
                      </SelectTrigger>
                    </Select>
                  </div>
                </div>
                <div className="flex flex-row gap-4">
                  <Button onClick={handleFilterApply}>필터 적용</Button>
                  <Button onClick={handleOrderListFilterReset}>
                    필터 초기화
                  </Button>
                </div>
              </div>
              <div className="flex flex-row gap-4 items-center">
                <Input
                  type="text"
                  placeholder="주문 번호 또는 고객명"
                  value={keyword}
                  onChange={(e) => setKeyword(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") {
                      e.preventDefault();
                      handleSearch();
                    }
                  }}
                />
                <Button onClick={handleSearch}>조회</Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>주문 목록</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow className="bg-gray-100">
                <TableHead>번호</TableHead>
                <TableHead>일시</TableHead>
                <TableHead>고객</TableHead>
                <TableHead>수량</TableHead>
                <TableHead>상태</TableHead>
                <TableHead>배송</TableHead>
                <TableHead>금액</TableHead>
                <TableHead>결제방식</TableHead>
                <TableHead>상세</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {orderList.map((order) => (
                <OrderSimpleInfoRow key={order.id} order={order} />
              ))}
            </TableBody>
          </Table>
          <PaginationRangeAsync
            currentPage={currentPage}
            totalPages={totalPages}
            siblingCount={2}
            onPageChange={handlePageChange}
          />
        </CardContent>
      </Card>
    </>
  );
}
