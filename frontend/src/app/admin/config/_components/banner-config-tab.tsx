"use client"

import { ArrowDown, ArrowUp } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { resolveAssetUrl } from "@/lib/public-asset-url"
import { useBannerConfig } from "../_hooks/use-banner-config"
import Image from "next/image"
type BannerConfigTabProps = {
  target: string
  title: string
}

export function BannerConfigTab({ target, title }: BannerConfigTabProps) {
  const {
    bannerList,
    addBannerModalOpen,
    setAddBannerModalOpen,
    handleAddBanner,
    handleDeleteBanner,
    handleChangeBannerOrder,
    moveBannerUp,
    moveBannerDown,
  } = useBannerConfig(target)

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        <Card className="w-full">
          <CardHeader className="flex flex-row items-center justify-between gap-2">
            <div className="flex flex-row items-center gap-2">
              <CardTitle>배너</CardTitle>
              <Dialog open={addBannerModalOpen} onOpenChange={setAddBannerModalOpen}>
                <DialogTrigger asChild>
                  <Button variant="outline">배너 추가</Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>배너 추가</DialogTitle>
                  </DialogHeader>
                  <form className="flex flex-col gap-2" onSubmit={handleAddBanner}>
                    <Input type="file" name="imageFile" placeholder="이미지 URL" accept=".webp, .png, .jpg" />
                    <Input type="text" name="link" placeholder="링크" />
                    <Input type="text" name="title" placeholder="제목" />
                    <Button type="submit">배너 추가</Button>
                  </form>
                </DialogContent>
              </Dialog>
            </div>
            <Button onClick={handleChangeBannerOrder}>배너 순서 변경</Button>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>ID</TableHead>
                  <TableHead>이미지</TableHead>
                  <TableHead>링크</TableHead>
                  <TableHead>제목</TableHead>
                  <TableHead>순서 변경</TableHead>
                  <TableHead>관리</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {bannerList.map((banner, index) => (
                  <TableRow key={banner.id}>
                    <TableCell>{banner.id}</TableCell>
                    <TableCell className="w-1/2">
                      <Image src={resolveAssetUrl(banner.imageUrl)} alt="Banner Image" width={100} height={100} />
                    </TableCell>
                    <TableCell>
                      <a href={banner.link} target="_blank" rel="noreferrer">
                        {banner.link}
                      </a>
                    </TableCell>
                    <TableCell>{banner.title}</TableCell>
                    <TableCell>
                      <div className="flex flex-row items-center gap-2">
                        <Button variant="outline" size="icon" onClick={() => moveBannerUp(index)}>
                          <ArrowUp />
                        </Button>
                        <Button variant="outline" size="icon" onClick={() => moveBannerDown(index)}>
                          <ArrowDown />
                        </Button>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Button variant="destructive" onClick={() => handleDeleteBanner(banner.id)}>
                        삭제
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </CardContent>
    </Card>
  )
}
