"use client";
import { Button } from "@/components/ui/button";
import {
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  DownloadIcon,
  FileDownIcon,
  RefreshCwIcon,
} from "lucide-react";
import { apiClient } from "@/lib/api.client";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import {
  Table,
  TableHeader,
  TableRow,
  TableHead,
  TableBody,
  TableCell,
} from "@/components/ui/table";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import { useState, useEffect } from "react";
import { OfflineProductDto } from "@/types/product";
import { Page } from "@/types/pagination";
import AdminSearchBar from "../../_components/AdminSearchBar";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";
import { SealedProductAddDialog } from "@/app/admin/products/sealed/_components/SealedProductAddPanel";
import { ProductAddDialog } from "@/app/admin/products/manual-product/_components/ProductAddTab";
import { SupplyAddDialog } from "@/app/admin/products/supplies/_components/SupplyAddDialog";
import { formatDeductionCount } from "@/app/admin/analyze/_components/report/report-table-styles";
import { toast } from "sonner";

const PAGE_SIZE = 10;

type SortField =
  | "id"
  | "productId"
  | "categoryTitle"
  | "title"
  | "priceValue"
  | "receivingQuantity"
  | "shippingQuantity"
  | "stockQuantity"
  | "createdAt"
  | "updatedAt"
  | "linkTableName";
type SortDirection = "asc" | "desc";

interface SortableTableHeadProps {
  field: SortField;
  label: string;
  sortBy: SortField;
  sortDirection: SortDirection;
  onSort: (field: SortField) => void;
}

function SortableTableHead({
  field,
  label,
  sortBy,
  sortDirection,
  onSort,
}: SortableTableHeadProps) {
  const active = sortBy === field;
  const SortIcon = !active
    ? ArrowUpDown
    : sortDirection === "asc"
      ? ArrowUp
      : ArrowDown;

  return (
    <TableHead
      aria-sort={
        active ? (sortDirection === "asc" ? "ascending" : "descending") : "none"
      }
    >
      <Button
        type="button"
        variant="ghost"
        className="-ml-3 h-8 px-3"
        onClick={() => onSort(field)}
      >
        {label}
        <SortIcon className="ml-1 h-4 w-4" />
      </Button>
    </TableHead>
  );
}

export default function OfflineProductPage() {
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [searchKeyword, setSearchKeyword] = useState("");
  const [sortBy, setSortBy] = useState<SortField>("createdAt");
  const [sortDirection, setSortDirection] = useState<SortDirection>("desc");
  const [offlineProductList, setOfflineProductList] = useState<
    OfflineProductDto[]
  >([]);
  const [rowLinkTableNames, setRowLinkTableNames] = useState<
    Record<number, string>
  >({});
  const [registerTarget, setRegisterTarget] = useState<{
    offlineProduct: OfflineProductDto;
    linkTableName: string;
  } | null>(null);
  const [refreshKey, setRefreshKey] = useState(0);
  const [stockSyncing, setStockSyncing] = useState(false);

  const handleAdded = () => {
    setRegisterTarget(null);
    setRefreshKey((prev) => prev + 1);
  };

  useEffect(() => {
    const params = {
      page: currentPage - 1,
      size: PAGE_SIZE,
      sortBy,
      sortDirection,
      ...(searchKeyword ? { keyword: searchKeyword } : {}),
    };

    void apiClient
      .get(
        searchKeyword
          ? "/api/admin/offline-data/product/search"
          : "/api/admin/offline-data/product",
        { params },
      )
      .then((response) => {
        const pageData = response.data as Page<OfflineProductDto>;
        setOfflineProductList(pageData.content);
        setTotalPages(pageData.totalPages);
      })
      .catch((error) => {
        console.error(error);
      });
  }, [currentPage, searchKeyword, sortBy, sortDirection, refreshKey]);

  const handlePageChange = (page: number) => {
    setCurrentPage(page);
  };

  const handleSort = (field: SortField) => {
    if (sortBy === field) {
      setSortDirection((current) => (current === "asc" ? "desc" : "asc"));
    } else {
      setSortBy(field);
      setSortDirection("asc");
    }
    setCurrentPage(1);
  };

  const handleDownloadOfflineProducts = () => {
    void apiClient
      .post("/api/admin/offline-data/product/download")
      .then((response) => {
        console.log(response);
        alert("오프라인 상품 동기화 완료");
        window.location.reload();
      })
      .catch((error) => {
        console.error(error);
      });
  };

  const handleRegisterOfflineProduct = (offlineProduct: OfflineProductDto) => {
    const selected = rowLinkTableNames[offlineProduct.id];
    if (!selected) {
      alert("제품 분류를 선택해주세요.");
      return;
    }
    setRegisterTarget({ offlineProduct, linkTableName: selected });
  };

  const handleSimpleRegisterOfflineProduct = (
    offlineProduct: OfflineProductDto,
  ) => {
    const selected = rowLinkTableNames[offlineProduct.id];
    if (!selected) {
      alert("제품 분류를 선택해주세요.");
      return;
    }
    void apiClient
      .post<void>("/api/admin/offline-data/product/simple-register", {
        offlineProductId: offlineProduct.id,
        linkTableName: selected,
      })
      .then(() => {
        toast.success("간편 등록 완료", {
          description: "간편 등록 완료",
        });
        setRefreshKey((prev) => prev + 1);
      })
      .catch((error) => {
        toast.error("간편 등록 실패", {
          description:
            error?.response?.data?.message ?? "간편 등록에 실패했습니다.",
        });
      });
  };

  const handleDownloadOfflineProductsExcel = () => {
    void apiClient
      .get("/api/admin/offline-data/product/download-excel", {
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
        const fileName = fileNameMatch?.[1] ?? "offline_products.xlsx";

        const downloadUrl = URL.createObjectURL(blob);
        const anchor = document.createElement("a");
        anchor.href = downloadUrl;
        anchor.download = fileName;
        document.body.appendChild(anchor);
        anchor.click();
        anchor.remove();
        URL.revokeObjectURL(downloadUrl);
        toast.success("엑셀 다운로드 완료");
      })
      .catch((error) => {
        console.error(error);
        toast.error("엑셀 다운로드 실패", {
          description:
            error?.response?.data?.message ?? "엑셀 다운로드에 실패했습니다.",
        });
      });
  };

  const handleSyncStockFromCurrentQuantities = () => {
    if (stockSyncing) {
      return;
    }
    const confirmed = confirm(
      "현재 입고·출고 기준으로 묶음 전환과 온라인 재고 반영을 실행할까요?\n(매출 다운로드·출고 절대값 재집계는 하지 않습니다.)",
    );
    if (!confirmed) {
      return;
    }
    setStockSyncing(true);
    void apiClient
      .post<{ processedProductCount: number }>(
        "/api/admin/offline-data/product/stock/sync",
      )
      .then((response) => {
        toast.success("재고 동기화 완료", {
          description: `처리 상품 ${response.data.processedProductCount}건`,
        });
        setRefreshKey((prev) => prev + 1);
      })
      .catch((error) => {
        console.error(error);
        toast.error("재고 동기화 실패", {
          description:
            error?.response?.data?.message ?? "재고 동기화에 실패했습니다.",
        });
      })
      .finally(() => {
        setStockSyncing(false);
      });
  };

  return (
    <div>
      <Button onClick={handleDownloadOfflineProducts} className="mb-4">
        <DownloadIcon className="w-4 h-4" />
        오프라인 상품 목록 동기화
      </Button>
      <Button onClick={handleDownloadOfflineProductsExcel} className="mb-4">
        <FileDownIcon className="w-4 h-4" />
        오프라인 상품 목록 엑셀 다운로드
      </Button>
      <Button
        onClick={handleSyncStockFromCurrentQuantities}
        disabled={stockSyncing}
        className="mb-4"
        variant="destructive"
      >
        <RefreshCwIcon className="w-4 h-4" />
        {stockSyncing ? "재고 동기화 중..." : "재고 동기화 (입·출고 → 온라인)"}
      </Button>
      <Card>
        <CardHeader>
          <CardTitle>오프라인 상품 검색</CardTitle>
        </CardHeader>
        <CardContent>
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
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>오프라인 상품 목록</CardTitle>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <SortableTableHead
                  field="id"
                  label="ID"
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                />
                <SortableTableHead
                  field="productId"
                  label="상품 ID"
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                />
                <SortableTableHead
                  field="categoryTitle"
                  label="카테고리"
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                />
                <SortableTableHead
                  field="title"
                  label="품목"
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                />
                <SortableTableHead
                  field="priceValue"
                  label="가격"
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                />
                <SortableTableHead
                  field="receivingQuantity"
                  label="입고"
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                />
                <SortableTableHead
                  field="shippingQuantity"
                  label="출고"
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                />
                <SortableTableHead
                  field="stockQuantity"
                  label="재고"
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                />
                <TableHead>바코드</TableHead>
                <SortableTableHead
                  field="createdAt"
                  label="생성일"
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                />
                {/* <SortableTableHead
                  field="updatedAt"
                  label="수정일"
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                /> */}
                <SortableTableHead
                  field="linkTableName"
                  label="제품 생성"
                  sortBy={sortBy}
                  sortDirection={sortDirection}
                  onSort={handleSort}
                />
              </TableRow>
            </TableHeader>
            <TableBody>
              {offlineProductList.map((offlineProduct) => (
                <TableRow key={offlineProduct.id}>
                  <TableCell>{offlineProduct.id}</TableCell>
                  <TableCell>{offlineProduct.productId}</TableCell>
                  <TableCell>{offlineProduct.categoryTitle}</TableCell>
                  <TableCell>{offlineProduct.title}</TableCell>
                  <TableCell className="text-right">
                    ₩{offlineProduct.priceValue.toLocaleString()}
                  </TableCell>
                  <TableCell
                    className={
                      (offlineProduct.receivingQuantity ?? 0) < 0
                        ? "text-right text-red-500"
                        : "text-right"
                    }
                  >
                    {formatDeductionCount(
                      offlineProduct.receivingQuantity ?? 0,
                    )}
                  </TableCell>
                  <TableCell
                    className={
                      (offlineProduct.shippingQuantity ?? 0) < 0
                        ? "text-right text-red-500"
                        : "text-right"
                    }
                  >
                    {formatDeductionCount(offlineProduct.shippingQuantity ?? 0)}
                  </TableCell>
                  <TableCell
                    className={
                      (offlineProduct.stockQuantity ?? 0) < 0
                        ? "text-right text-red-500"
                        : "text-right"
                    }
                  >
                    {formatDeductionCount(offlineProduct.stockQuantity ?? 0)}
                  </TableCell>
                  <TableCell>{offlineProduct.barcode}</TableCell>
                  <TableCell>{offlineProduct.createdAt}</TableCell>
                  {/* <TableCell>{offlineProduct.updatedAt}</TableCell> */}
                  <TableCell>
                    {offlineProduct.linkTableName !== null ? (
                      <>
                        {offlineProduct.linkTableName}
                        <br />
                        {offlineProduct.linkId}
                      </>
                    ) : (
                      <div className="flex flex-row gap-2">
                        <Select
                          value={rowLinkTableNames[offlineProduct.id] ?? ""}
                          onValueChange={(value) => {
                            setRowLinkTableNames((prev) => ({
                              ...prev,
                              [offlineProduct.id]: value,
                            }));
                          }}
                        >
                          <SelectTrigger className="w-32">
                            <SelectValue placeholder="카테고리 선택" />
                          </SelectTrigger>
                          <SelectContent className="w-fit">
                            <SelectItem value="Sealed">밀봉 제품</SelectItem>
                            <SelectItem value="Manual">수동 제품</SelectItem>
                            <SelectItem value="Supply">서플라이</SelectItem>
                          </SelectContent>
                        </Select>
                        <Button
                          variant="outline"
                          size="icon"
                          onClick={() =>
                            handleRegisterOfflineProduct(offlineProduct)
                          }
                        >
                          등록
                        </Button>
                        <Button
                          variant="default"
                          onClick={() =>
                            handleSimpleRegisterOfflineProduct(offlineProduct)
                          }
                        >
                          간편 등록
                        </Button>
                      </div>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <PaginationRangeAsync
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={handlePageChange}
          />
        </CardContent>
      </Card>
      <SealedProductAddDialog
        open={registerTarget?.linkTableName === "Sealed"}
        onOpenChange={(open) => {
          if (!open) setRegisterTarget(null);
        }}
        initialOfflineProductId={registerTarget?.offlineProduct.productId}
        initialProductNameEn={registerTarget?.offlineProduct.title}
        initialProductPrice={registerTarget?.offlineProduct.priceValue}
        onAdded={handleAdded}
      />
      <ProductAddDialog
        open={registerTarget?.linkTableName === "Manual"}
        onOpenChange={(open) => {
          if (!open) setRegisterTarget(null);
        }}
        initialOfflineProductId={registerTarget?.offlineProduct.productId}
        initialProductNameEn={registerTarget?.offlineProduct.title}
        initialProductPrice={registerTarget?.offlineProduct.priceValue}
        onAdded={handleAdded}
      />
      <SupplyAddDialog
        open={registerTarget?.linkTableName === "Supply"}
        onOpenChange={(open) => {
          if (!open) setRegisterTarget(null);
        }}
        initialOfflineProductId={registerTarget?.offlineProduct.productId}
        initialProductNameEn={registerTarget?.offlineProduct.title}
        initialProductPrice={registerTarget?.offlineProduct.priceValue}
        onAdded={handleAdded}
      />
    </div>
  );
}
