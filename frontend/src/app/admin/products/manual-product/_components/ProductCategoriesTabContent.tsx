"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import PaginationRangeAsync from "@/components/ui/pagination-range-async";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import type { ProductCategoryDto } from "@/types/product";
import { useProductCategoriesTab } from "../_hooks/useProductCategoriesTab";

export function ProductCategoriesTabContent() {
  const {
    categoryListPage,
    currentPage,
    setCurrentPage,
    totalPages,
    categoryAddModalOpen,
    setCategoryAddModalOpen,
    categoryEditModalOpen,
    editingCategory,
    openCategoryEditModal,
    handleCategoryEditModalOpenChange,
    handleCategoryAdd,
    handleCategoryUpdate,
    handleCategoryDelete,
  } = useProductCategoriesTab();

  return (
    <div className="flex flex-row gap-4">
      <Card className="w-full">
        <CardHeader>
          <div className="flex flex-row items-center justify-between gap-2">
            <CardTitle>상품 카테고리</CardTitle>
            <Dialog
              open={categoryAddModalOpen}
              onOpenChange={setCategoryAddModalOpen}
            >
              <DialogTrigger asChild>
                <Button className="w-fit">추가</Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-sm">
                <DialogHeader>
                  <DialogTitle>카테고리 추가</DialogTitle>
                  <DialogDescription>
                    카테고리 명을 입력해주세요.
                  </DialogDescription>
                </DialogHeader>
                <form
                  className="flex flex-col gap-2"
                  onSubmit={handleCategoryAdd}
                >
                  <Input
                    name="nameEn"
                    placeholder="카테고리 이름(영문)"
                    required
                  />
                  <Input
                    name="nameKo"
                    placeholder="카테고리 이름(한글)"
                    required
                  />
                  <DialogFooter>
                    <Button type="submit">추가</Button>
                    <DialogClose asChild>
                      <Button variant="outline">취소</Button>
                    </DialogClose>
                  </DialogFooter>
                </form>
              </DialogContent>
            </Dialog>
          </div>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-14 align-middle">ID</TableHead>
                <TableHead className="min-w-0">이름(영문)</TableHead>
                <TableHead className="min-w-0">이름(한글)</TableHead>
                <TableHead className="w-36 text-center">작업</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {(categoryListPage?.content ?? []).map(
                (category: ProductCategoryDto) => (
                  <TableRow key={category.id}>
                    <TableCell className="w-14 align-middle">
                      {category.id}
                    </TableCell>
                    <TableCell className="min-w-0 truncate align-middle">
                      {category.nameEn}
                    </TableCell>
                    <TableCell className="min-w-0 truncate align-middle">
                      {category.nameKo}
                    </TableCell>
                    <TableCell className="w-36 align-middle">
                      <div className="flex flex-wrap items-center justify-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openCategoryEditModal(category)}
                        >
                          수정
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => handleCategoryDelete(category.id)}
                        >
                          삭제
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ),
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
        open={categoryEditModalOpen}
        onOpenChange={handleCategoryEditModalOpenChange}
      >
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>카테고리 수정</DialogTitle>
            <DialogDescription>
              수정할 카테고리 이름을 입력해주세요.
            </DialogDescription>
          </DialogHeader>
          {editingCategory && (
            <form
              key={editingCategory.id}
              className="flex flex-col gap-2"
              onSubmit={handleCategoryUpdate}
            >
              <Input
                name="nameEn"
                defaultValue={editingCategory.nameEn}
                placeholder="카테고리 이름(영문)"
                required
              />
              <Input
                name="nameKo"
                defaultValue={editingCategory.nameKo}
                placeholder="카테고리 이름(한글)"
                required
              />
              <DialogFooter>
                <Button type="submit">수정</Button>
                <DialogClose asChild>
                  <Button variant="outline">취소</Button>
                </DialogClose>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>
    </div>
  );
}
