"use client"

import Image from "next/image"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { resolveAssetUrl } from "@/lib/public-asset-url"
import { useSetBannerConfig } from "../_hooks/use-set-banner-config"
import { Checkbox } from "@/components/ui/checkbox"
export function SetBannerConfigTab() {
    const { setBannerList, editingBanner, setEditingBanner, handleUpdateSetBanner, toggleSetBannerActive } =
        useSetBannerConfig()

    return (
        <Card>
            <CardHeader>
                <CardTitle>게임별배너 설정</CardTitle>
            </CardHeader>
            <CardContent>
                <Table>
                    <TableHeader>
                        <TableRow>
                            <TableHead>게임</TableHead>
                            <TableHead>배너 ID</TableHead>
                            <TableHead>이미지</TableHead>
                            <TableHead>링크</TableHead>
                            <TableHead>제목</TableHead>
                            <TableHead>활성</TableHead>
                            <TableHead>관리</TableHead>
                        </TableRow>
                    </TableHeader>
                    <TableBody>
                        {setBannerList.map((banner) => (
                            <TableRow key={`${banner.game}-${banner.bannerId}`}>
                                <TableCell>{banner.game}</TableCell>
                                <TableCell>{banner.bannerId}</TableCell>
                                <TableCell>
                                    {banner.imageUrl ? (
                                        <Image
                                            src={resolveAssetUrl(banner.imageUrl)}
                                            alt={banner.title ?? "set banner"}
                                            width={120}
                                            height={48}
                                            className="h-12 w-auto object-cover"
                                        />
                                    ) : (
                                        "-"
                                    )}
                                </TableCell>
                                <TableCell className="max-w-48 truncate">{banner.link || "-"}</TableCell>
                                <TableCell>{banner.title || "-"}</TableCell>
                                <TableCell>
                                    <Checkbox
                                        checked={banner.active ?? false}
                                        onCheckedChange={(checked) => {
                                            void toggleSetBannerActive(banner, checked === true)
                                        }}
                                    />
                                </TableCell>
                                <TableCell>
                                    <Button variant="outline" onClick={() => setEditingBanner(banner)}>
                                        수정
                                    </Button>
                                </TableCell>
                            </TableRow>
                        ))}
                    </TableBody>
                </Table>

                <Dialog open={editingBanner !== null} onOpenChange={(open) => !open && setEditingBanner(null)}>
                    <DialogContent>
                        <DialogHeader>
                            <DialogTitle>
                                세트 배너 수정 ({editingBanner?.game} / {editingBanner?.bannerId})
                            </DialogTitle>
                        </DialogHeader>
                        {editingBanner && (
                            <form className="flex flex-col gap-4" onSubmit={handleUpdateSetBanner}>
                                <div className="flex flex-col gap-2">
                                    <Label htmlFor="imageFile">이미지 (변경 시에만 선택)</Label>
                                    <Input
                                        id="imageFile"
                                        type="file"
                                        name="imageFile"
                                        accept=".webp,.png,.jpg,.jpeg"
                                    />
                                </div>
                                <div className="flex flex-col gap-2">
                                    <Label htmlFor="link">링크</Label>
                                    <Input
                                        id="link"
                                        type="text"
                                        name="link"
                                        defaultValue={editingBanner.link ?? ""}
                                    />
                                </div>
                                <div className="flex flex-col gap-2">
                                    <Label htmlFor="title">제목</Label>
                                    <Input
                                        id="title"
                                        type="text"
                                        name="title"
                                        defaultValue={editingBanner.title ?? ""}
                                    />
                                </div>
                                <div className="flex items-center gap-2">
                                    <input
                                        id="active"
                                        name="active"
                                        type="checkbox"
                                        defaultChecked={editingBanner.active ?? false}
                                        className="size-4"
                                    />
                                    <Label htmlFor="active">활성</Label>
                                </div>
                                <Button type="submit">저장</Button>
                            </form>
                        )}
                    </DialogContent>
                </Dialog>
            </CardContent>
        </Card>
    )
}
