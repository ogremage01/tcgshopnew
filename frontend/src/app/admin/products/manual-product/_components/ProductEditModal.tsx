"use client"

import { useEffect, useState } from "react"
import Image from "next/image"
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { RichTextEditor } from "@/components/ui/rich-text-editor"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select-bgwhite"
import { api, getApiErrorMessage } from "@/lib/api"
import { offlineProductIdForSubmit } from "@/lib/offline-product-id"
import { resolveAssetUrl } from "@/lib/public-asset-url"
import type { ManualProductDto, ProductCategoryDto, ProductIpDto } from "@/types/product"
import { OfflineProductLinkSelect } from "@/app/admin/products/_components/OfflineProductLinkSelect"
import { MANUAL_PRODUCT_DESCRIPTION_MAX_LENGTH } from "../_constants"

const IMAGE_MAX_SIZE = 1 * 1024 * 1024

type InputFile = {
    file: File
    url: string
}

export function ProductEditModal({
    product,
    open,
    onOpenChange,
    onUpdated,
}: {
    product: ManualProductDto
    open: boolean
    onOpenChange: (open: boolean) => void
    onUpdated?: () => void
}) {
    const [categoryList, setCategoryList] = useState<ProductCategoryDto[]>([])
    const [productIpList, setProductIpList] = useState<ProductIpDto[]>([])
    const [selectedProductType, setSelectedProductType] = useState("")
    const [selectedProductIp, setSelectedProductIp] = useState("")
    const [description, setDescription] = useState("")
    const [isVisible, setIsVisible] = useState(true)
    const [imageFile, setImageFile] = useState<InputFile | null>(null)
    const [selectedOfflineProductId, setSelectedOfflineProductId] = useState<
        string | undefined
    >(product.offlineProductId ?? undefined)

    useEffect(() => {
        if (!open) return
        api
            .get<ProductCategoryDto[]>("/api/admin/product/product-categories")
            .then((res) => setCategoryList(res))
            .catch(() => setCategoryList([]))
        api
            .get<ProductIpDto[]>("/api/admin/product/product-ips")
            .then((res) => setProductIpList(res))
            .catch(() => setProductIpList([]))
    }, [open])

    useEffect(() => {
        if (!open) {
            if (imageFile) URL.revokeObjectURL(imageFile.url)
            setImageFile(null)
            return
        }

        setSelectedProductType(product.productType ?? "")
        setSelectedProductIp(product.productIp ?? "")
        setDescription(product.description ?? "")
        setIsVisible(product.isVisible ?? true)
        setSelectedOfflineProductId(product.offlineProductId ?? undefined)
        setImageFile(null)
    }, [open, product])

    useEffect(() => {
        return () => {
            if (imageFile) URL.revokeObjectURL(imageFile.url)
        }
    }, [imageFile])

    const handleInputFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (!file) return

        if (file.size > IMAGE_MAX_SIZE) {
            alert(
                `최대 이미지 크기를 초과했습니다. ${IMAGE_MAX_SIZE / 1024 / 1024}MB 이하로 선택해주세요.`,
            )
            e.target.value = ""
            return
        }

        if (imageFile) URL.revokeObjectURL(imageFile.url)
        setImageFile({ file, url: URL.createObjectURL(file) })
    }

    const handleProductEdit = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault()

        if (!selectedProductType || !selectedProductIp) {
            alert("상품 카테고리와 제품 IP를 선택해주세요.")
            return
        }

        const formData = new FormData(e.currentTarget)
        const trimmedDescription = description.trim()
        if (!trimmedDescription) {
            alert("상품 설명을 입력해주세요.")
            return
        }
        if (trimmedDescription.length > MANUAL_PRODUCT_DESCRIPTION_MAX_LENGTH) {
            alert(`상품 설명은 ${MANUAL_PRODUCT_DESCRIPTION_MAX_LENGTH}자 이내로 입력해주세요.`)
            return
        }

        const fd = new FormData()
        fd.append("nameEn", String(formData.get("nameEn") ?? ""))
        fd.append("nameKo", String(formData.get("nameKo") ?? ""))
        fd.append("description", trimmedDescription)
        fd.append("price", String(formData.get("price") ?? ""))
        fd.append("stock", String(formData.get("stock") ?? ""))
        fd.append("productType", selectedProductType)
        fd.append("productIp", selectedProductIp)
        fd.append("isVisible", String(isVisible))
        if (imageFile) {
            fd.append("imageFile", imageFile.file)
        }
        fd.append(
            "offlineProductId",
            offlineProductIdForSubmit(selectedOfflineProductId),
        )

        api.putFormData(`/api/admin/product/manual-products/${product.id}`, fd)
            .then(() => {
                alert("상품 수정 완료")
                onUpdated?.()
                onOpenChange(false)
            })
            .catch((err) => {
                alert(`상품 수정 실패 ${getApiErrorMessage(err) ?? ""}`)
                console.error(err)
            })
    }

    return (
        <Dialog open={open} onOpenChange={onOpenChange}>
            <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
                <DialogHeader>
                    <DialogTitle>상품 수정</DialogTitle>
                    <DialogDescription>상품 정보를 수정해주세요.</DialogDescription>
                </DialogHeader>
                <form
                    key={`edit-${product.id}`}
                    className="flex flex-col gap-2"
                    onSubmit={handleProductEdit}
                >
                    <div className="flex flex-col gap-2">
                        <Label>상품 카테고리</Label>
                        <Select
                            value={selectedProductType || undefined}
                            onValueChange={setSelectedProductType}
                        >
                            <SelectTrigger>
                                <SelectValue placeholder="상품 카테고리" />
                            </SelectTrigger>
                            <SelectContent>
                                {categoryList.map((category) => (
                                    <SelectItem key={category.id} value={category.nameEn}>
                                        {category.nameKo}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                    <div className="flex flex-col gap-2">
                        <Label>제품 IP</Label>
                        <Select
                            value={selectedProductIp || undefined}
                            onValueChange={setSelectedProductIp}
                        >
                            <SelectTrigger>
                                <SelectValue placeholder="제품 IP" />
                            </SelectTrigger>
                            <SelectContent>
                                {productIpList.map((productIp) => (
                                    <SelectItem key={productIp.id} value={productIp.nameEn}>
                                        {productIp.nameKo}
                                    </SelectItem>
                                ))}
                            </SelectContent>
                        </Select>
                    </div>
                    <Label>상품 이름(한글)</Label>
                    <Input name="nameKo" defaultValue={product.nameKo} required />
                    <Label>상품 이름(영어)</Label>
                    <Input name="nameEn" defaultValue={product.nameEn} required />
                    <div className="flex flex-row gap-2">
                        <div className="flex flex-1 flex-col gap-2">
                            <Label>상품 가격</Label>
                            <Input name="price" type="number" defaultValue={product.price} required />
                        </div>
                        <div className="flex flex-1 flex-col gap-2">
                            <Label>상품 수량</Label>
                            <Input name="stock" type="number" defaultValue={product.stock} required />
                        </div>
                    </div>
                    <Label>상품 설명 (최대 {MANUAL_PRODUCT_DESCRIPTION_MAX_LENGTH}자-html 태그 포함)</Label>
                    <RichTextEditor
                        value={description}
                        onChange={setDescription}
                        placeholder="상품 설명"
                        maxLength={MANUAL_PRODUCT_DESCRIPTION_MAX_LENGTH}
                    />
                    <div className="flex items-center gap-2">
                        <Checkbox
                            id="edit-isVisible"
                            checked={isVisible}
                            onCheckedChange={(checked) => setIsVisible(checked === true)}
                        />
                        <Label htmlFor="edit-isVisible">상품 표시 여부</Label>
                    </div>
                    <OfflineProductLinkSelect
                        value={selectedOfflineProductId}
                        onValueChange={setSelectedOfflineProductId}
                    />
                    <div className="flex flex-col gap-1">
                        <Label>
                            상품 이미지 ※ webp, png 전용 / 최대 {IMAGE_MAX_SIZE / 1024 / 1024}
                            MB
                        </Label>
                        <Input
                            type="file"
                            accept=".webp,.png"
                            onChange={handleInputFileChange}
                        />
                        {product.imgUrl && !imageFile ? (
                            <p className="text-xs text-muted-foreground">
                                새 이미지를 선택하면 기존 이미지가 덮어쓰입니다.
                            </p>
                        ) : null}
                    </div>
                    {imageFile ? (
                        <div className="flex flex-col gap-2">
                            <Label>새 이미지 미리보기</Label>
                            <div className="flex flex-row gap-2 rounded-md bg-orange-100 p-4">
                                <div className="flex flex-col gap-2">
                                    <Image
                                        src={imageFile.url}
                                        alt={imageFile.file.name}
                                        width={100}
                                        height={100}
                                        className="object-contain"
                                    />
                                    <p className="text-xs text-gray-500">{imageFile.file.name}</p>
                                    <Button
                                        type="button"
                                        variant="outline"
                                        onClick={() => {
                                            URL.revokeObjectURL(imageFile.url)
                                            setImageFile(null)
                                        }}
                                        className="w-full"
                                    >
                                        삭제
                                    </Button>
                                </div>
                            </div>
                        </div>
                    ) : product.imgUrl ? (
                        <div className="flex flex-col gap-2">
                            <Label>현재 이미지</Label>
                            <Image
                                src={resolveAssetUrl(product.imgUrl)}
                                alt={product.nameKo ?? "상품 이미지"}
                                width={100}
                                height={100}
                                className="object-contain"
                            />
                        </div>
                    ) : null}
                    <Button type="submit">수정</Button>
                </form>
            </DialogContent>
        </Dialog>
    )
}
