"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import type { SupplyDto } from "@/types/product";
import { useSuppliesTab } from "../_hooks/useSuppliesTab";
import { api } from "@/lib/api";
import { SupplyAddDialog } from "./SupplyAddDialog";
import { SupplyEditDialog } from "./SupplyEditDialog";
import { SupplyRow } from "./SupplyRow";

export function SuppliesTabContent() {
  const {
    supplyListPage,
    currentPage,
    setCurrentPage,
    totalPages,
    searchKeyword,
    setSearchKeyword,
    handleSearch,
    handleSearchReset,
    handleDelete,
    loadSupplyList,
  } = useSuppliesTab();

  const [idKeyword, setIdKeyword] = useState("");
  const [idSearchResult, setIdSearchResult] = useState<SupplyDto | null>(null);
  const [addDialogOpen, setAddDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [editingSupply, setEditingSupply] = useState<SupplyDto | null>(null);

  const handleIdSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const id = Number(idKeyword.trim());
    if (!idKeyword.trim() || isNaN(id)) {
      alert("유효한 ID를 입력해주세요.");
      return;
    }
    api
      .get<SupplyDto>(`/api/admin/product/supply-products/${id}`)
      .then((res) => setIdSearchResult(res))
      .catch(() => {
        alert("해당 ID의 서플라이를 찾을 수 없습니다.");
        setIdSearchResult(null);
      });
  };

  const clearIdSearch = () => {
    setIdSearchResult(null);
    setIdKeyword("");
  };

  const onKeywordSearch = (e: React.FormEvent<HTMLFormElement>) => {
    clearIdSearch();
    handleSearch(e);
  };

  const onKeywordSearchReset = () => {
    clearIdSearch();
    handleSearchReset();
  };

  const openEdit = (supply: SupplyDto) => {
    setEditingSupply(supply);
    setEditDialogOpen(true);
  };

  const handleEditOpenChange = (open: boolean) => {
    setEditDialogOpen(open);
    if (!open) setEditingSupply(null);
  };

  const handleDeleteSupply = (supplyId: number) => {
    if (editingSupply?.id === supplyId) {
      setEditDialogOpen(false);
      setEditingSupply(null);
    }
    handleDelete(supplyId);
  };

  const supplies = idSearchResult
    ? [idSearchResult]
    : (supplyListPage?.content ?? []);

  return (
    <div className="flex flex-col gap-4">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center justify-between gap-2">
            <span>서플라이 목록</span>

            <Button variant="outline" onClick={() => setAddDialogOpen(true)}>
              추가
            </Button>
            <SupplyAddDialog
              open={addDialogOpen}
              onOpenChange={setAddDialogOpen}
              onAdded={loadSupplyList}
            />
          </CardTitle>
        </CardHeader>
        <CardContent className="flex flex-col gap-4">
          <div className="flex flex-row gap-2">
            <form
              className="flex flex-row flex-wrap items-end gap-2"
              onSubmit={onKeywordSearch}
            >
              <div className="min-w-[200px] flex-1">
                <Label htmlFor="supply-search">키워드 검색</Label>
                <Input
                  id="supply-search"
                  value={searchKeyword}
                  onChange={(e) => setSearchKeyword(e.target.value)}
                  placeholder="이름·제조사·분류"
                />
              </div>
              <Button type="submit">검색</Button>
              <Button
                type="button"
                variant="outline"
                onClick={onKeywordSearchReset}
              >
                초기화
              </Button>
            </form>

            <form
              className="flex flex-row flex-wrap items-end gap-2"
              onSubmit={handleIdSearch}
            >
              <div className="min-w-[120px]">
                <Label htmlFor="supply-id-search">ID 검색</Label>
                <Input
                  id="supply-id-search"
                  value={idKeyword}
                  onChange={(e) => setIdKeyword(e.target.value)}
                  placeholder="ID"
                  type="number"
                  min={1}
                />
              </div>
              <Button type="submit">검색</Button>
              {idSearchResult && (
                <Button
                  type="button"
                  variant="outline"
                  onClick={clearIdSearch}
                >
                  전체
                </Button>
              )}
            </form>
          </div>

          <Table className="table-fixed">
            <TableHeader>
              <TableRow>
                <TableHead className="w-14">ID</TableHead>
                <TableHead className="w-28">이미지</TableHead>
                <TableHead className="min-w-0">한글명</TableHead>
                <TableHead className="min-w-0">영문명</TableHead>
                <TableHead className="w-24">제조사</TableHead>
                <TableHead className="w-24">분류</TableHead>
                <TableHead className="w-20 text-right">가격</TableHead>
                <TableHead className="w-16 text-right">재고</TableHead>
                <TableHead className="w-16 text-center">공개</TableHead>
                <TableHead className="w-36 text-center">작업</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {supplies.length === 0 ? (
                <TableRow>
                  <TableCell
                    colSpan={9}
                    className="text-center text-muted-foreground"
                  >
                    등록된 서플라이가 없습니다.
                  </TableCell>
                </TableRow>
              ) : (
                supplies.map((supply: SupplyDto) => (
                  <SupplyRow
                    key={supply.id}
                    supply={supply}
                    onEdit={openEdit}
                    onDelete={handleDeleteSupply}
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

      {editingSupply && (
        <SupplyEditDialog
          supply={editingSupply}
          open={editDialogOpen}
          onOpenChange={handleEditOpenChange}
          onUpdated={loadSupplyList}
        />
      )}
    </div>
  );
}
