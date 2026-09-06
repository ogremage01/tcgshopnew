"use client"

import { useEffect, useRef, useState } from "react"
import Image from "next/image"
import { TableRow, TableCell } from "@/components/ui/table"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { resolveAssetUrl } from "@/lib/public-asset-url"
import type { ManualProductDto, ManualProductPatchRequest } from "@/types/product"

function numFromRaw(raw: string) {
    return raw === "" ? 0 : Number(raw)
}

type NumericDraft = {
    price: number
    stock: number
}

type ManualProductSaveArgs = {
    id: number
    base: ManualProductDto
    patch: ManualProductPatchRequest
}

type Props = {
    product: ManualProductDto
    schedulePatch: (args: ManualProductSaveArgs) => void
    flushPatch: (args: ManualProductSaveArgs) => void
    onEdit: (product: ManualProductDto) => void
    onDelete: (id: number) => void
}

export function ManualProductRow({
    product,
    schedulePatch,
    flushPatch,
    onEdit,
    onDelete,
}: Props) {
    const [draft, setDraft] = useState<NumericDraft>({
        price: product.price ?? 0,
        stock: product.stock ?? 0,
    })
    const draftRef = useRef(draft)
    const productRef = useRef(product)
    productRef.current = product

    useEffect(() => {
        const next: NumericDraft = {
            price: product.price ?? 0,
            stock: product.stock ?? 0,
        }
        const timer = setTimeout(() => {
            draftRef.current = next
            setDraft(next)
        }, 0)
        return () => clearTimeout(timer)
    }, [product.id, product.price, product.stock])

    const buildArgs = (patch: ManualProductPatchRequest): ManualProductSaveArgs => ({
        id: productRef.current.id,
        base: { ...productRef.current, ...draftRef.current },
        patch,
    })

    const patchNumChange = (field: keyof NumericDraft, raw: string) => {
        const num = numFromRaw(raw)
        const next = { ...draftRef.current, [field]: num }
        draftRef.current = next
        setDraft(next)
        schedulePatch(buildArgs({ [field]: num }))
    }

    const patchNumBlur = (field: keyof NumericDraft, raw: string) => {
        const num = numFromRaw(raw)
        const next = { ...draftRef.current, [field]: num }
        draftRef.current = next
        setDraft(next)
        flushPatch(buildArgs({ [field]: num }))
    }

    return (
        <TableRow>
            <TableCell>{product.id}</TableCell>
            <TableCell>
                {product.imgUrl ? (
                    <Image
                        src={resolveAssetUrl(product.imgUrl)}
                        alt={product.nameKo ?? "상품 이미지"}
                        width={100}
                        height={100}
                    />
                ) : (
                    <span className="text-muted-foreground text-xs">—</span>
                )}
            </TableCell>
            <TableCell className="truncate">{product.nameKo}</TableCell>
            <TableCell className="truncate">
                {product.categoryNameKo ?? product.categoryNameEn ?? "—"}
            </TableCell>
            <TableCell className="truncate">
                {product.productIpNameKo ?? product.productIpNameEn ?? product.productIp ?? "—"}
            </TableCell>
            <TableCell>
                <Input
                    className="w-24"
                    type="number"
                    value={draft.price}
                    min={0}
                    step={1}
                    onChange={(e) => patchNumChange("price", e.target.value)}
                    onBlur={(e) => patchNumBlur("price", e.target.value)}
                />
            </TableCell>
            <TableCell>
                <Input
                    className="w-20"
                    type="number"
                    value={draft.stock}
                    min={0}
                    step={1}
                    onChange={(e) => patchNumChange("stock", e.target.value)}
                    onBlur={(e) => patchNumBlur("stock", e.target.value)}
                />
            </TableCell>
            <TableCell className="text-center">
                {product.isVisible ? "Y" : "N"}
            </TableCell>
            <TableCell>
                <div className="flex flex-wrap items-center justify-center gap-2">
                    <Button variant="outline" size="sm" onClick={() => onEdit(product)}>
                        수정
                    </Button>
                    <Button variant="destructive" size="sm" onClick={() => onDelete(product.id)}>
                        삭제
                    </Button>
                </div>
            </TableCell>
        </TableRow>
    )
}
