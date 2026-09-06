"use client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Separator } from "@/components/ui/separator";
import { ScrollArea } from "@/components/ui/scroll-area";

import {
  Table,
  TableHeader,
  TableRow,
  TableHead,
  TableBody,
  TableCell,
} from "@/components/ui/table";
import { useEffect, useState } from "react";
import AdminSearchBar from "@/app/admin/_components/AdminSearchBar";
import { apiClient } from "@/lib/api.client";
import type {
  OfflineProductDto,
  OfflineProductReceivingRequest,
} from "@/types/product";
import type { Page } from "@/types/pagination";
import { toast } from "sonner";
import { useAuthStore } from "@/stores/auth-store";

type ReceivingItem = {
  product: OfflineProductDto;
  /** 입력 중 빈 칸·`-` 허용을 위해 문자열도 둠 */
  quantity: number | "";
};

export default function AdminProductsReceivingPage() {
  const userName = useAuthStore((s) => s.user?.name);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [currentPage, setCurrentPage] = useState(1);
  const [pageSize] = useState(10);
  const [searchResults, setSearchResults] = useState<OfflineProductDto[]>([]);
  const [receivingList, setReceivingList] = useState<ReceivingItem[]>([]);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!searchKeyword) {
      setSearchResults([]);
      return;
    }

    void apiClient
      .get("/api/admin/offline-data/product/search", {
        params: {
          page: currentPage - 1,
          size: pageSize,
          keyword: searchKeyword,
        },
      })
      .then((response) => {
        const pageData = response.data as Page<OfflineProductDto>;
        setSearchResults(pageData.content);
      })
      .catch((error) => {
        console.error(error);
        setSearchResults([]);
      });
  }, [searchKeyword, currentPage, pageSize]);

  const handleAddToReceiving = (product: OfflineProductDto) => {
    setReceivingList((prev) => {
      const existing = prev.find((item) => item.product.id === product.id);
      if (existing) {
        return prev.map((item) =>
          item.product.id === product.id
            ? {
                ...item,
                quantity:
                  (typeof item.quantity === "number" ? item.quantity : 0) + 1,
              }
            : item,
        );
      }
      return [...prev, { product, quantity: 1 }];
    });
  };

  const handleQuantityChange = (productId: number, raw: string) => {
    setReceivingList((prev) =>
      prev.map((item) => {
        if (item.product.id !== productId) return item;
        // 음수 입력 중간 상태("", "-")를 허용해야 0 이하로 내려감
        if (raw === "" || raw === "-") {
          return { ...item, quantity: "" };
        }
        const quantity = Number(raw);
        if (!Number.isFinite(quantity)) return item;
        return { ...item, quantity };
      }),
    );
  };

  const handleRemoveFromReceiving = (productId: number) => {
    setReceivingList((prev) =>
      prev.filter((item) => item.product.id !== productId),
    );
  };

  const handleRegisterReceiving = () => {
    if (receivingList.length === 0) {
      toast.error("입고할 품목이 없습니다.");
      return;
    }
    const invalid = receivingList.some(
      (item) =>
        item.quantity === "" ||
        !Number.isFinite(item.quantity) ||
        item.quantity === 0,
    );
    if (invalid) {
      toast.error("입고 수량은 0이 아닌 숫자여야 합니다. (음수는 차감)");
      return;
    }

    const body: OfflineProductReceivingRequest = {
      items: receivingList.map(({ product, quantity }) => ({
        productId: product.id,
        receivingQuantity: quantity as number,
      })),
    };

    setSubmitting(true);
    void apiClient
      .post("/api/admin/offline-data/product/receiving", body)
      .then(() => {
        toast.success("입고 등록 완료");
        setReceivingList([]);
      })
      .catch((error) => {
        toast.error("입고 등록 실패", {
          description:
            error?.response?.data?.message ?? "입고 등록에 실패했습니다.",
        });
      })
      .finally(() => {
        setSubmitting(false);
      });
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle>입고 관리</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4 gap-4">
        <div className="flex flex-row gap-2">
          <AdminSearchBar
            url="/api/admin/offline-data/product/search"
            queryParam="keyword"
            pageParam="page"
            sizeParam="size"
            placeholder="품목명으로 검색"
            onSearchRequest={({ keyword }) => {
              setSearchKeyword(keyword.trim());
              setCurrentPage(1);
            }}
            description="품목명으로 검색"
          />
        </div>
        <ScrollArea className="h-[200px] border border-gray-200 rounded-md">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>제품 ID</TableHead>
                <TableHead>제품명</TableHead>
                <TableHead>판매가</TableHead>
                <TableHead>입고</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {searchResults.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={4}
                    className="text-center text-muted-foreground"
                  >
                    {searchKeyword
                      ? "검색 결과가 없습니다."
                      : "품목명으로 검색해 주세요."}
                  </TableCell>
                </TableRow>
              ) : (
                searchResults.map((product) => (
                  <TableRow key={product.id}>
                    <TableCell>{product.productId}</TableCell>
                    <TableCell>{product.title}</TableCell>
                    <TableCell>
                      ₩{product.priceValue.toLocaleString()}
                    </TableCell>
                    <TableCell>
                      <Button onClick={() => handleAddToReceiving(product)}>
                        입고
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </ScrollArea>
        <Separator />
        <div className="flex flex-row justify-between items-center gap-2">
          <span className="text-sm text-muted-foreground">
            입고 담당자: {userName ?? "-"}
          </span>
          <div className="flex flex-row items-center gap-2">
            <Button
              disabled={receivingList.length === 0 || submitting}
              onClick={handleRegisterReceiving}
            >
              {submitting ? "등록 중..." : "입고 등록"}
            </Button>
          </div>
        </div>
        <ScrollArea className="h-[500px] border border-gray-200 rounded-md p-2">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>제품 ID</TableHead>
                <TableHead>제품명</TableHead>
                <TableHead>판매가</TableHead>
                <TableHead>개수</TableHead>
                <TableHead>삭제</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {receivingList.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={5}
                    className="text-center text-muted-foreground"
                  >
                    입고할 제품을 위에서 추가해 주세요.
                  </TableCell>
                </TableRow>
              ) : (
                receivingList.map(({ product, quantity }) => (
                  <TableRow key={product.id}>
                    <TableCell>{product.productId}</TableCell>
                    <TableCell>{product.title}</TableCell>
                    <TableCell>
                      ₩{product.priceValue.toLocaleString()}
                    </TableCell>
                    <TableCell>
                      <Input
                        type="number"
                        className="w-24"
                        value={quantity}
                        onChange={(e) =>
                          handleQuantityChange(product.id, e.target.value)
                        }
                      />
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="outline"
                        onClick={() => handleRemoveFromReceiving(product.id)}
                      >
                        삭제
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </ScrollArea>
      </CardContent>
    </Card>
  );
}
