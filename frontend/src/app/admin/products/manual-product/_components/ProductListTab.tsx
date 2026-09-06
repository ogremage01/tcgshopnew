"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import { api } from "@/lib/api";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { ManualProductDto } from "@/types/product";
import { useManualProductsTab } from "../_hooks/useManualProductsTab";
import { useManualProductPatch } from "../_hooks/useManualProductPatch";
import { ProductAddDialog } from "./ProductAddTab";
import { ProductEditModal } from "./ProductEditModal";
import { ManualProductRow } from "./ManualProductRow";
import { Input } from "@/components/ui/input";

export function ProductListTab() {
  const {
    productListPage,
    currentPage,
    setCurrentPage,
    totalPages,
    applyKeyword,
    handleDelete,
    loadProductList,
  } = useManualProductsTab();
  const { schedulePatch, flushPatch } = useManualProductPatch();

  const [openAddDialog, setOpenAddDialog] = useState(false);
  const [openEditModal, setOpenEditModal] = useState(false);
  const [editingProduct, setEditingProduct] = useState<ManualProductDto | null>(null);

  // ID 검색
  const [idSearchResult, setIdSearchResult] = useState<ManualProductDto | null>(null);

  const displayedProducts = idSearchResult ? [idSearchResult] : (productListPage?.content ?? []);

  const openEdit = (product: ManualProductDto) => {
    setEditingProduct(product);
    setOpenEditModal(true);
  };

  const handleEditOpenChange = (open: boolean) => {
    setOpenEditModal(open);
    if (!open) setEditingProduct(null);
  };

  const handleKeywordSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const keyword = (new FormData(e.currentTarget).get("keyword") as string ?? "").trim();
    setIdSearchResult(null);
    applyKeyword(keyword);
  };

  const handleIdSearch = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const raw = (new FormData(e.currentTarget).get("id") as string ?? "").trim();
    const id = Number(raw);
    if (!raw || isNaN(id)) { alert("유효한 ID를 입력해주세요."); return; }
    api.get<ManualProductDto>(`/api/admin/product/manual-products/${id}`)
      .then((res) => { setIdSearchResult(res); applyKeyword(""); })
      .catch(() => { alert("해당 ID의 상품을 찾을 수 없습니다."); setIdSearchResult(null); });
  };

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <CardTitle>검색</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-row gap-2">
            <form className="flex gap-1" onSubmit={handleKeywordSearch}>
              <Input name="keyword" placeholder="한글명 / 영문명" className="w-44" />
              <Button type="submit">검색</Button>
            </form>
            <form className="flex gap-1" onSubmit={handleIdSearch}>
              <Input name="id" placeholder="ID" type="number" min={1} className="w-28" />
              <Button type="submit">ID 검색</Button>
            </form>
            {idSearchResult && (
              <Button variant="outline" onClick={() => setIdSearchResult(null)}>전체</Button>
            )}
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between gap-2">
            <span>등록 상품 목록</span>
            <Button variant="outline" onClick={() => setOpenAddDialog(true)}>
              추가
            </Button>
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <Table className="table-fixed">
            <TableHeader>
              <TableRow>
                <TableHead className="w-14">ID</TableHead>
                <TableHead className="w-28">이미지</TableHead>
                <TableHead className="min-w-0">한글명</TableHead>
                <TableHead className="w-28">카테고리</TableHead>
                <TableHead className="w-24">제품 IP</TableHead>
                <TableHead className="w-24">가격</TableHead>
                <TableHead className="w-20">재고</TableHead>
                <TableHead className="w-16 text-center">공개</TableHead>
                <TableHead className="w-28 text-center">작업</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {displayedProducts.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={9}
                    className="text-center text-muted-foreground"
                  >
                    등록된 수동 상품이 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                displayedProducts.map((product: ManualProductDto) => (
                  <ManualProductRow
                    key={product.id}
                    product={product}
                    schedulePatch={schedulePatch}
                    flushPatch={flushPatch}
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
        </CardContent>
      </Card>

      <ProductAddDialog
        open={openAddDialog}
        onOpenChange={setOpenAddDialog}
        onAdded={loadProductList}
      />

      {editingProduct ? (
        <ProductEditModal
          product={editingProduct}
          open={openEditModal}
          onOpenChange={handleEditOpenChange}
          onUpdated={loadProductList}
        />
      ) : null}
    </div>
  );
}
