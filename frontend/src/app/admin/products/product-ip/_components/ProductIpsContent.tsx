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
import type { ProductIpDto } from "@/types/product";
import { useProductIpsTab } from "../_hooks/useProductIpsTab";

export function ProductIpsContent() {
  const {
    productIpListPage,
    currentPage,
    setCurrentPage,
    totalPages,
    productIpAddModalOpen,
    setProductIpAddModalOpen,
    productIpEditModalOpen,
    editingProductIp,
    openProductIpEditModal,
    handleProductIpEditModalOpenChange,
    handleProductIpAdd,
    handleProductIpUpdate,
    handleProductIpDelete,
  } = useProductIpsTab();

  return (
    <div className="flex flex-row gap-4">
      <Card className="w-full">
        <CardHeader>
          <div className="flex flex-row items-center justify-between gap-2">
            <CardTitle>제품 IP</CardTitle>
            <Dialog
              open={productIpAddModalOpen}
              onOpenChange={setProductIpAddModalOpen}
            >
              <DialogTrigger asChild>
                <Button className="w-fit">추가</Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-sm">
                <DialogHeader>
                  <DialogTitle>제품 IP 추가</DialogTitle>
                  <DialogDescription>
                    제품 IP 명을 입력해주세요.
                  </DialogDescription>
                </DialogHeader>
                <form
                  className="flex flex-col gap-2"
                  onSubmit={handleProductIpAdd}
                >
                  <Input
                    name="nameEn"
                    placeholder="제품 IP 이름(영문)"
                    required
                  />
                  <Input
                    name="nameKo"
                    placeholder="제품 IP 이름(한글)"
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
              {(productIpListPage?.content ?? []).map(
                (productIp: ProductIpDto) => (
                  <TableRow key={productIp.id}>
                    <TableCell className="w-14 align-middle">
                      {productIp.id}
                    </TableCell>
                    <TableCell className="min-w-0 truncate align-middle">
                      {productIp.nameEn}
                    </TableCell>
                    <TableCell className="min-w-0 truncate align-middle">
                      {productIp.nameKo}
                    </TableCell>
                    <TableCell className="w-36 align-middle">
                      <div className="flex flex-wrap items-center justify-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => openProductIpEditModal(productIp)}
                        >
                          수정
                        </Button>
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => handleProductIpDelete(productIp.id)}
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
        open={productIpEditModalOpen}
        onOpenChange={handleProductIpEditModalOpenChange}
      >
        <DialogContent className="sm:max-w-sm">
          <DialogHeader>
            <DialogTitle>제품 IP 수정</DialogTitle>
            <DialogDescription>
              수정할 제품 IP 이름을 입력해주세요.
            </DialogDescription>
          </DialogHeader>
          {editingProductIp && (
            <form
              key={editingProductIp.id}
              className="flex flex-col gap-2"
              onSubmit={handleProductIpUpdate}
            >
              <Input
                name="nameEn"
                defaultValue={editingProductIp.nameEn}
                placeholder="제품 IP 이름(영문)"
                required
              />
              <Input
                name="nameKo"
                defaultValue={editingProductIp.nameKo}
                placeholder="제품 IP 이름(한글)"
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
