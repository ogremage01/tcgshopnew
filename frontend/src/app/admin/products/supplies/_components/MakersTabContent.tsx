"use client"

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import PaginationRangeAsync from "@/components/ui/pagination-range-async"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Button } from "@/components/ui/button"
import type { MakerDto } from "@/types/product"
import { useMakersTab } from "../_hooks/useMakersTab"

export function MakersTabContent() {
    const {
        makerListPage,
        currentPage,
        setCurrentPage,
        totalPages,
        makerAddModalOpen,
        setMakerAddModalOpen,
        makerEditModalOpen,
        editingMaker,
        openMakerEditModal,
        handleMakerEditModalOpenChange,
        handleMakerAdd,
        handleMakerUpdate,
        handleMakerDelete,
    } = useMakersTab()

    return (
        <div className="flex flex-row gap-4">
            <Card className="w-1/2">
                <CardHeader>
                    <div className="flex flex-row items-center justify-between gap-2">
                        <CardTitle>제조사 목록</CardTitle>
                        <Dialog open={makerAddModalOpen} onOpenChange={setMakerAddModalOpen}>
                            <DialogTrigger asChild>
                                <Button className="w-fit">추가</Button>
                            </DialogTrigger>
                            <DialogContent className="sm:max-w-sm">
                                <DialogHeader>
                                    <DialogTitle>제조사 추가</DialogTitle>
                                    <DialogDescription>제조사 명을 입력해주세요.</DialogDescription>
                                </DialogHeader>
                                <form className="flex flex-col gap-2" onSubmit={handleMakerAdd}>
                                    <Input name="name" placeholder="제조사 이름" required />
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
                    <Table className="table-fixed">
                        <TableHeader>
                            <TableRow>
                                <TableHead className="w-14">ID</TableHead>
                                <TableHead className="min-w-0">이름</TableHead>
                                <TableHead className="w-36 text-center">작업</TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            {(makerListPage?.content ?? []).map((maker: MakerDto) => (
                                <TableRow key={maker.id}>
                                    <TableCell className="w-14 align-middle">{maker.id}</TableCell>
                                    <TableCell className="min-w-0 truncate align-middle">{maker.name}</TableCell>
                                    <TableCell className="w-36 align-middle">
                                        <div className="flex flex-wrap items-center justify-center gap-2">
                                            <Button
                                                variant="outline"
                                                size="sm"
                                                onClick={() => openMakerEditModal(maker)}
                                            >
                                                수정
                                            </Button>
                                            <Button
                                                variant="destructive"
                                                size="sm"
                                                onClick={() => handleMakerDelete(maker.id)}
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

            <Dialog open={makerEditModalOpen} onOpenChange={handleMakerEditModalOpenChange}>
                <DialogContent className="sm:max-w-sm">
                    <DialogHeader>
                        <DialogTitle>제조사 수정</DialogTitle>
                        <DialogDescription>수정할 제조사 이름을 입력해주세요.</DialogDescription>
                    </DialogHeader>
                    {editingMaker && (
                        <form key={editingMaker.id} className="flex flex-col gap-2" onSubmit={handleMakerUpdate}>
                            <Input name="name" defaultValue={editingMaker.name} placeholder="제조사 이름" required />
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
