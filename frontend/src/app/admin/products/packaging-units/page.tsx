"use client";

import { useCallback, useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
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
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import AdminSearchBar from "@/app/admin/_components/AdminSearchBar";
import { apiClient } from "@/lib/api.client";
import type {
  OfflineProductDto,
  PackagingUnitDto,
  PackagingUnitRequest,
} from "@/types/product";
import type { Page } from "@/types/pagination";
import { toast } from "sonner";

const PAGE_SIZE = 10;

type ProductSlot = "piece" | "packaging";

export default function AdminPackagingUnitsPage() {
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [units, setUnits] = useState<PackagingUnitDto[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [piece, setPiece] = useState<OfflineProductDto | null>(null);
  const [packaging, setPackaging] = useState<OfflineProductDto | null>(null);
  const [unitCount, setUnitCount] = useState<number | "">(10);
  const [submitting, setSubmitting] = useState(false);

  const [searchSlot, setSearchSlot] = useState<ProductSlot>("piece");
  const [searchKeyword, setSearchKeyword] = useState("");
  const [searchPage, setSearchPage] = useState(1);
  const [searchResults, setSearchResults] = useState<OfflineProductDto[]>([]);

  const loadUnits = useCallback(() => {
    void apiClient
      .get("/api/admin/offline-data/packaging-units", {
        params: { page: currentPage - 1, size: PAGE_SIZE },
      })
      .then((response) => {
        const pageData = response.data as Page<PackagingUnitDto>;
        setUnits(pageData.content);
        setTotalPages(pageData.totalPages || 1);
      })
      .catch((error) => {
        console.error(error);
        setUnits([]);
        toast.error("포장 단위 목록을 불러오지 못했습니다.");
      });
  }, [currentPage]);

  useEffect(() => {
    loadUnits();
  }, [loadUnits]);

  useEffect(() => {
    if (!dialogOpen || !searchKeyword) {
      setSearchResults([]);
      return;
    }
    void apiClient
      .get("/api/admin/offline-data/product/search", {
        params: {
          page: searchPage - 1,
          size: 8,
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
  }, [dialogOpen, searchKeyword, searchPage]);

  const openCreate = () => {
    setEditingId(null);
    setPiece(null);
    setPackaging(null);
    setUnitCount(10);
    setSearchKeyword("");
    setSearchSlot("piece");
    setDialogOpen(true);
  };

  const openEdit = (unit: PackagingUnitDto) => {
    setEditingId(unit.id);
    setPiece({
      id: unit.pieceId,
      productId: unit.pieceProductId ?? "",
      title: unit.pieceTitle ?? "",
      categoryId: "",
      categoryTitle: "",
      priceUnit: 0,
      priceValue: 0,
      barcode: "",
      createdAt: "",
      updatedAt: "",
    });
    setPackaging({
      id: unit.packagingId,
      productId: unit.packagingProductId ?? "",
      title: unit.packagingTitle ?? "",
      categoryId: "",
      categoryTitle: "",
      priceUnit: 0,
      priceValue: 0,
      barcode: "",
      createdAt: "",
      updatedAt: "",
    });
    setUnitCount(unit.unitCount);
    setSearchKeyword("");
    setSearchSlot("piece");
    setDialogOpen(true);
  };

  const selectProduct = (product: OfflineProductDto) => {
    if (searchSlot === "piece") {
      setPiece(product);
    } else {
      setPackaging(product);
    }
  };

  const handleSubmit = () => {
    if (!piece || !packaging) {
      toast.error("낱개/포장 상품을 모두 선택하세요.");
      return;
    }
    if (piece.id === packaging.id) {
      toast.error("낱개와 포장 상품은 달라야 합니다.");
      return;
    }
    const count = typeof unitCount === "number" ? unitCount : Number(unitCount);
    if (!Number.isFinite(count) || count < 1) {
      toast.error("포장 단위 개수는 1 이상이어야 합니다.");
      return;
    }

    const body: PackagingUnitRequest = {
      pieceId: piece.id,
      packagingId: packaging.id,
      unitCount: count,
    };

    setSubmitting(true);
    const request =
      editingId == null
        ? apiClient.post("/api/admin/offline-data/packaging-units", body)
        : apiClient.put(
            `/api/admin/offline-data/packaging-units/${editingId}`,
            body,
          );

    void request
      .then(() => {
        toast.success(editingId == null ? "등록되었습니다." : "수정되었습니다.");
        setDialogOpen(false);
        loadUnits();
      })
      .catch((error) => {
        console.error(error);
        const message =
          error?.response?.data?.message ?? "저장에 실패했습니다.";
        toast.error(message);
      })
      .finally(() => setSubmitting(false));
  };

  const handleDelete = (id: number) => {
    if (!window.confirm("이 포장 단위를 삭제할까요?")) return;
    void apiClient
      .delete(`/api/admin/offline-data/packaging-units/${id}`)
      .then(() => {
        toast.success("삭제되었습니다.");
        loadUnits();
      })
      .catch((error) => {
        console.error(error);
        toast.error("삭제에 실패했습니다.");
      });
  };

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="flex flex-row items-center justify-between space-y-0">
          <CardTitle>포장 단위 관리</CardTitle>
          <Button type="button" onClick={openCreate}>
            등록
          </Button>
        </CardHeader>
        <CardContent className="space-y-4">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ID</TableHead>
                <TableHead>낱개 상품</TableHead>
                <TableHead>포장 상품</TableHead>
                <TableHead>단위 개수</TableHead>
                <TableHead className="w-40">관리</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {units.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={5}
                    className="text-center text-muted-foreground"
                  >
                    등록된 포장 단위가 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                units.map((unit) => (
                  <TableRow key={unit.id}>
                    <TableCell>{unit.id}</TableCell>
                    <TableCell>
                      <div className="font-medium">{unit.pieceTitle}</div>
                      <div className="text-xs text-muted-foreground">
                        id={unit.pieceId} / {unit.pieceProductId}
                      </div>
                    </TableCell>
                    <TableCell>
                      <div className="font-medium">{unit.packagingTitle}</div>
                      <div className="text-xs text-muted-foreground">
                        id={unit.packagingId} / {unit.packagingProductId}
                      </div>
                    </TableCell>
                    <TableCell>{unit.unitCount}</TableCell>
                    <TableCell className="space-x-2">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        onClick={() => openEdit(unit)}
                      >
                        수정
                      </Button>
                      <Button
                        type="button"
                        variant="destructive"
                        size="sm"
                        onClick={() => handleDelete(unit.id)}
                      >
                        삭제
                      </Button>
                    </TableCell>
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

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {editingId == null ? "포장 단위 등록" : "포장 단위 수정"}
            </DialogTitle>
            <DialogDescription>
              낱개 입고 시 박스 단위로 변환할 상품 관계를 설정합니다.
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="grid gap-3 sm:grid-cols-2">
              <div className="rounded-md border p-3 space-y-1">
                <div className="text-sm font-medium">낱개 상품</div>
                {piece ? (
                  <div className="text-sm">
                    {piece.title}
                    <div className="text-xs text-muted-foreground">
                      id={piece.id} / {piece.productId}
                    </div>
                  </div>
                ) : (
                  <div className="text-sm text-muted-foreground">미선택</div>
                )}
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={() => setSearchSlot("piece")}
                >
                  검색 대상: 낱개
                </Button>
              </div>
              <div className="rounded-md border p-3 space-y-1">
                <div className="text-sm font-medium">포장 상품</div>
                {packaging ? (
                  <div className="text-sm">
                    {packaging.title}
                    <div className="text-xs text-muted-foreground">
                      id={packaging.id} / {packaging.productId}
                    </div>
                  </div>
                ) : (
                  <div className="text-sm text-muted-foreground">미선택</div>
                )}
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={() => setSearchSlot("packaging")}
                >
                  검색 대상: 포장
                </Button>
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium">포장 1개당 낱개 수</label>
              <Input
                type="number"
                min={1}
                value={unitCount}
                onChange={(e) => {
                  const raw = e.target.value;
                  if (raw === "") {
                    setUnitCount("");
                    return;
                  }
                  const n = Number(raw);
                  if (Number.isFinite(n)) setUnitCount(n);
                }}
              />
            </div>

            <div className="space-y-2">
              <div className="text-sm font-medium">
                상품 검색 ({searchSlot === "piece" ? "낱개" : "포장"})
              </div>
              <AdminSearchBar
                url="/api/admin/offline-data/product/search"
                queryParam="keyword"
                pageParam="page"
                sizeParam="size"
                placeholder="오프라인 상품 검색"
                onSearchRequest={({ keyword }) => {
                  setSearchKeyword(keyword.trim());
                  setSearchPage(1);
                }}
                description="오프라인 상품 검색"
              />
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>상품명</TableHead>
                    <TableHead>ID</TableHead>
                    <TableHead></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {searchResults.length === 0 ? (
                    <TableRow>
                      <TableCell
                        colSpan={3}
                        className="text-center text-muted-foreground"
                      >
                        검색 결과가 없습니다.
                      </TableCell>
                    </TableRow>
                  ) : (
                    searchResults.map((product) => (
                      <TableRow key={product.id}>
                        <TableCell>{product.title}</TableCell>
                        <TableCell className="text-xs text-muted-foreground">
                          {product.id} / {product.productId}
                        </TableCell>
                        <TableCell>
                          <Button
                            type="button"
                            size="sm"
                            variant="outline"
                            onClick={() => selectProduct(product)}
                          >
                            선택
                          </Button>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => setDialogOpen(false)}
            >
              취소
            </Button>
            <Button type="button" disabled={submitting} onClick={handleSubmit}>
              저장
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
