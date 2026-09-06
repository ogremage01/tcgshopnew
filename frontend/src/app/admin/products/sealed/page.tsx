"use client";

import { useMemo, useState, useRef } from "react";
import { api } from "@/lib/api";
import { Search } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectGroup,
  SelectItem,
  SelectLabel,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox";
import {
  Table,
  TableBody,
  TableHead,
  TableHeader,
  TableRow,
  TableCell,
} from "@/components/ui/table";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import type { SealedProductAdminDto } from "@/types/product";
import { SealedProductAddDialog } from "./_components/SealedProductAddPanel";
import { SealedProductEditModal } from "./_components/SealedProductEditModal";
import { SealedProductRow } from "./_components/SealedProductRow";
import {
  useSealedProductsTab,
  type SealedProductFilters,
} from "./_hooks/useSealedProductsTab";

const ALL_VALUE = "all";

type SetOption = { value: string; label: string };

export default function AdminSealedProductsPage() {
  const {
    productListPage,
    currentPage,
    setCurrentPage,
    totalPages,
    facets,
    filters,
    applyFilters,
    handleDelete,
    loadProductList,
    loadFacets,
  } = useSealedProductsTab();

  const products = productListPage?.content ?? [];

  // 추가 다이얼로그
  const [openAddDialog, setOpenAddDialog] = useState(false);

  // 수정 모달
  const [openEditModal, setOpenEditModal] = useState(false);
  const [editingProduct, setEditingProduct] =
    useState<SealedProductAdminDto | null>(null);

  const openEdit = (product: SealedProductAdminDto) => {
    setEditingProduct(product);
    setOpenEditModal(true);
  };
  const handleEditOpenChange = (open: boolean) => {
    setOpenEditModal(open);
    if (!open) setEditingProduct(null);
  };

  // 필터 로컬 상태 (적용 버튼 전까지는 pending)
  const [pendingKeyword, setPendingKeyword] = useState("");
  const [pendingGame, setPendingGame] = useState(ALL_VALUE);
  const [pendingSetCode, setPendingSetCode] = useState<string | undefined>(
    undefined,
  );

  // ID 검색
  const [idSearchResult, setIdSearchResult] =
    useState<SealedProductAdminDto | null>(null);
  const idInputRef = useRef<HTMLInputElement>(null);

  const handleIdSearch = () => {
    const raw = idInputRef.current?.value.trim();
    if (!raw) return;
    const id = Number(raw);
    if (isNaN(id)) {
      alert("유효한 ID를 입력해주세요.");
      return;
    }
    api
      .get<SealedProductAdminDto>(`/api/admin/product/sealed-products/${id}`)
      .then((res) => setIdSearchResult(res))
      .catch(() => {
        alert("해당 ID의 상품을 찾을 수 없습니다.");
        setIdSearchResult(null);
      });
  };

  const clearIdSearch = () => {
    setIdSearchResult(null);
    if (idInputRef.current) idInputRef.current.value = "";
  };

  const displayedProducts = idSearchResult ? [idSearchResult] : products;

  const selectedGameFacet = useMemo(
    () => facets.find((f) => f.game === pendingGame) ?? null,
    [facets, pendingGame],
  );

  const setOptions = useMemo<SetOption[]>(() => {
    if (!selectedGameFacet) return [];
    return selectedGameFacet.sets
      .filter((s) => s.setCode)
      .map((s) => ({
        value: s.setCode,
        label: s.setName ? `${s.setCode} - ${s.setName}` : s.setCode,
      }));
  }, [selectedGameFacet]);

  const selectedSetOption = useMemo(
    () => setOptions.find((o) => o.value === pendingSetCode) ?? null,
    [setOptions, pendingSetCode],
  );

  const handleGameChange = (game: string) => {
    setPendingGame(game);
    setPendingSetCode(undefined);
  };

  const handleApplyFilters = () => {
    const next: SealedProductFilters = {};
    if (pendingKeyword.trim()) next.keyword = pendingKeyword.trim();
    if (pendingGame !== ALL_VALUE) next.game = pendingGame;
    if (pendingSetCode) next.setCode = pendingSetCode;
    applyFilters(next);
  };

  const handleResetFilters = () => {
    setPendingKeyword("");
    setPendingGame(ALL_VALUE);
    setPendingSetCode(undefined);
    applyFilters({});
  };

  const isFiltered = !!(filters.keyword || filters.game || filters.setCode);

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-bold">밀봉 상품 관리</h1>

      {/* 검색 · 필터 영역 */}
      <Card>
        <CardContent className="pt-4">
          <div className="flex flex-wrap items-end gap-2">
            {/* 키워드 검색 */}
            <div className="flex flex-col gap-1">
              <span className="text-sm font-medium">상품명 검색</span>
              <div className="flex gap-1">
                <Input
                  className="w-52"
                  placeholder="한글명 / 영문명"
                  value={pendingKeyword}
                  onChange={(e) => setPendingKeyword(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") handleApplyFilters();
                  }}
                />
              </div>
            </div>

            {/* 게임 선택 — 등록된 밀봉 상품 facets 기준 */}
            <div className="flex flex-col gap-1">
              <span className="text-sm font-medium">게임</span>
              <Select value={pendingGame} onValueChange={handleGameChange}>
                <SelectTrigger className="w-44">
                  <SelectValue placeholder="게임 선택" />
                </SelectTrigger>
                <SelectContent>
                  <SelectGroup>
                    <SelectLabel>게임</SelectLabel>
                    <SelectItem value={ALL_VALUE}>전체</SelectItem>
                    {facets.map((f) => (
                      <SelectItem key={f.game} value={f.game}>
                        {f.game}
                      </SelectItem>
                    ))}
                  </SelectGroup>
                </SelectContent>
              </Select>
            </div>

            {/* 세트 선택 */}
            <div className="flex flex-col gap-1">
              <span className="text-sm font-medium">세트</span>
              <Combobox
                items={setOptions}
                value={selectedSetOption}
                onValueChange={(option) =>
                  setPendingSetCode(option?.value ?? undefined)
                }
                disabled={pendingGame === ALL_VALUE}
                isItemEqualToValue={(a, b) => a.value === b.value}
                itemToStringLabel={(item) => item.label}
                itemToStringValue={(item) => item.value}
              >
                <ComboboxInput
                  className="w-64"
                  placeholder="세트 검색/선택"
                  disabled={pendingGame === ALL_VALUE}
                  showClear
                />
                <ComboboxContent className="bg-white p-0">
                  <ComboboxEmpty>검색 결과가 없습니다.</ComboboxEmpty>
                  <ComboboxList className="max-h-60 overflow-y-auto">
                    {(item) => (
                      <ComboboxItem key={item.value} value={item}>
                        {item.label}
                      </ComboboxItem>
                    )}
                  </ComboboxList>
                </ComboboxContent>
              </Combobox>
            </div>

            {/* 버튼 */}
            <Button onClick={handleApplyFilters} size="sm">
              <Search className="mr-1 h-4 w-4" />
              검색
            </Button>
            {isFiltered && (
              <Button variant="outline" size="sm" onClick={handleResetFilters}>
                초기화
              </Button>
            )}

            <div className="ml-auto flex items-end gap-1">
              <div className="flex flex-col gap-1">
                <span className="text-sm font-medium">ID 검색</span>
                <Input
                  ref={idInputRef}
                  className="w-28"
                  placeholder="ID"
                  type="number"
                  min={1}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") handleIdSearch();
                  }}
                />
              </div>
              <Button size="sm" onClick={handleIdSearch}>
                <Search className="mr-1 h-4 w-4" />
                검색
              </Button>
              {idSearchResult && (
                <Button size="sm" variant="outline" onClick={clearIdSearch}>
                  전체
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>

      {/* 목록 */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between gap-2">
            <span>
              밀봉 상품 목록
              {isFiltered && (
                <span className="ml-2 text-sm font-normal text-muted-foreground">
                  (필터 적용 중)
                </span>
              )}
            </span>
            <Button variant="outline" onClick={() => setOpenAddDialog(true)}>
              추가
            </Button>
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-col gap-4">
            <Table className="table-fixed">
              <TableHeader>
                <TableRow>
                  <TableHead className="w-14">ID</TableHead>
                  <TableHead className="w-20">이미지</TableHead>
                  <TableHead className="w-20">게임</TableHead>
                  <TableHead className="w-12">언어</TableHead>
                  <TableHead className="w-40">영문명</TableHead>
                  <TableHead className="w-40">한글명</TableHead>
                  <TableHead className="w-24">가격</TableHead>
                  <TableHead className="w-16">표시 재고</TableHead>
                  <TableHead className="w-16">총 재고</TableHead>
                  <TableHead className="w-16">최대 재고</TableHead>
                  <TableHead className="w-16 text-center">공개</TableHead>
                  <TableHead className="w-28 text-center">작업</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {displayedProducts.length === 0 ? (
                  <TableRow>
                    <TableCell
                      colSpan={12}
                      className="text-center text-muted-foreground"
                    >
                      {isFiltered
                        ? "검색 결과가 없습니다."
                        : "등록된 밀봉 상품이 없습니다."}
                    </TableCell>
                  </TableRow>
                ) : (
                  displayedProducts.map((product) => (
                    <SealedProductRow
                      key={product.id}
                      product={product}
                      onEdit={openEdit}
                      onDelete={handleDelete}
                    />
                  ))
                )}
              </TableBody>
            </Table>
            <PaginationRangeAsync
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
            />
          </div>
        </CardContent>
      </Card>

      <SealedProductAddDialog
        open={openAddDialog}
        onOpenChange={setOpenAddDialog}
        onAdded={() => {
          loadProductList();
          loadFacets();
        }}
      />

      {editingProduct && (
        <SealedProductEditModal
          product={editingProduct}
          open={openEditModal}
          onOpenChange={handleEditOpenChange}
          onUpdated={() => {
            loadProductList();
            loadFacets();
          }}
        />
      )}
    </div>
  );
}
