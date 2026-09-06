"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import PaginationRangeAsync from "@/components/ui/pagination-range-async"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import type { SupplyTypeDto } from "@/types/product"
import { useSupplyTypesTab } from "../_hooks/useSupplyTypesTab"

export function SupplyTypesTabContent() {
    const {
        supplyTypeListPage,
        currentPage,
        setCurrentPage,
        totalPages,
        supplyTypeAddModalOpen,
        setSupplyTypeAddModalOpen,
        supplyTypeEditModalOpen,
        editingSupplyType,
        openSupplyTypeEditModal,
        handleSupplyTypeEditModalOpenChange,
        handleSupplyTypeAdd,
        handleSupplyTypeUpdate,
        handleSupplyTypeDelete,
    } = useSupplyTypesTab()

    return (
        <div className="flex flex-row gap-4">
            <Card className="w-1/2">
                <CardHeader>
                    <div className="flex flex-row items-center justify-between gap-2">
                        <CardTitle>서플라이 분류</CardTitle>
                        <Dialog open={supplyTypeAddModalOpen} onOpenChange={setSupplyTypeAddModalOpen}>
                            <DialogTrigger asChild>
                                <Button className="w-fit">추가</Button>
                            </DialogTrigger>
                            <DialogContent className="sm:max-w-sm">
                                <DialogHeader>
                                    <DialogTitle>서플라이 분류 추가</DialogTitle>
                                    <DialogDescription>서플라이 분류 명을 입력해주세요.</DialogDescription>
                                </DialogHeader>
                                <form className="flex flex-col gap-2" onSubmit={handleSupplyTypeAdd}>
                                    <Input name="nameEn" placeholder="서플라이 분류 이름(영문)" required />
                                    <Input name="nameKo" placeholder="서플라이 분류 이름(한글)" required />
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
                            {(supplyTypeListPage?.content ?? []).map((supplyType: SupplyTypeDto) => (
                                <TableRow key={supplyType.id}>
                                    <TableCell className="w-14 align-middle">{supplyType.id}</TableCell>
                                    <TableCell className="min-w-0 truncate align-middle">{supplyType.nameEn}</TableCell>
                                    <TableCell className="min-w-0 truncate align-middle">{supplyType.nameKo}</TableCell>
                                    <TableCell className="w-36 align-middle">
                                        <div className="flex flex-wrap items-center justify-center gap-2">
                                            <Button
                                                variant="outline"
                                                size="sm"
                                                onClick={() => openSupplyTypeEditModal(supplyType)}
                                            >
                                                수정
                                            </Button>
                                            <Button
                                                variant="destructive"
                                                size="sm"
                                                onClick={() => handleSupplyTypeDelete(supplyType.id)}
                                            >
                                                삭제
                                            </Button>
                                        </div>
                                    </TableCell>
                                </TableRow>
                            ))}
                        </TableBody>
                    </Table>
                    <PaginationRangeAsync
                        currentPage={currentPage}
                        totalPages={totalPages}
                        onPageChange={setCurrentPage}
                    />
                </CardContent>
            </Card>

            <Dialog open={supplyTypeEditModalOpen} onOpenChange={handleSupplyTypeEditModalOpenChange}>
                <DialogContent className="sm:max-w-sm">
                    <DialogHeader>
                        <DialogTitle>서플라이 분류 수정</DialogTitle>
                        <DialogDescription>수정할 서플라이 분류 이름을 입력해주세요.</DialogDescription>
                    </DialogHeader>
                    {editingSupplyType && (
                        <form key={editingSupplyType.id} className="flex flex-col gap-2" onSubmit={handleSupplyTypeUpdate}>
                            <Input name="nameEn" defaultValue={editingSupplyType.nameEn} placeholder="서플라이 분류 이름(영문)" required />
                            <Input name="nameKo" defaultValue={editingSupplyType.nameKo} placeholder="서플라이 분류 이름(한글)" required />
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
    )
}
