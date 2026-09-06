import { Dialog, DialogTrigger, DialogContent, DialogHeader, DialogTitle, DialogDescription, DialogClose, DialogFooter } from "@/components/ui/dialog"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Checkbox } from "@/components/ui/checkbox"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { useState, useEffect } from "react"
import type { ReactNode } from "react"
import { Separator } from "@/components/ui/separator"
import ProductImage from "@/components/product/ProductImage"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { api } from "@/lib/api"
import { StorageDto } from "@/types/product"
import { ButtonGroup } from "@/components/ui/button-group"
import { CardProductRegister, GradePricingPolicyDto } from "@/types/product"
import { useCardProductLanguages } from "@/hooks/use-card-product-languages"

type CardProductAddModalProps = {
    selectedCard: any
    storageList: StorageDto[]
    gradePolicyList: GradePricingPolicyDto[]
    onOpenChange: (open: boolean) => void
    trigger?: ReactNode
    imageUrl: string
}

export default function CardProductAddModal({ selectedCard, storageList, gradePolicyList, onOpenChange, trigger, imageUrl }: CardProductAddModalProps) {
    const [priceLinkedUsing, setPriceLinkedUsing] = useState<boolean>(true)
    const [isVisible, setIsVisible] = useState<boolean>(true)
    const [isAutoUpdatedStock, setIsAutoUpdatedStock] = useState<boolean>(true)
    const [currentVisibleStock, setCurrentVisibleStock] = useState<number>(0)
    const [totalStock, setTotalStock] = useState<number>(0)
    const [maxVisibleStock, setMaxVisibleStock] = useState<number>(8)
    const { languages } = useCardProductLanguages()
    const handleCardProductAdd = (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        console.log("selectedCard", selectedCard)

        const formData = new FormData(e.target as HTMLFormElement)
        if (formData.get("language") === "ko" && selectedCard.cardNameK === "") {
            alert("한글 카드가 존재하지 않습니다.")
            return
        }
        if (formData.get("language") !== "ko" && selectedCard.cardName === "") {
            alert("영어 카드가 존재하지 않습니다.")
            return
        }
        formData.set("isPriceLinked", priceLinkedUsing ? "true" : "false")
        formData.set("isVisible", isVisible ? "true" : "false")
        formData.set("isAutoUpdatedStock", isAutoUpdatedStock ? "true" : "false")
        const cardProductRegisterDto: CardProductRegister = {
            cardName: selectedCard.cardName,
            productId: selectedCard.id,
            unionPriceId: selectedCard.id,
            productType: selectedCard.productType,
            condition: formData.get("condition") as string,
            printType: selectedCard.printType,
            language: formData.get("language") as string,
            isVisible: isVisible,
            currentVisibleStock: currentVisibleStock,
            totalStock: totalStock,
            maxVisibleStock: maxVisibleStock,
            isAutoUpdatedStock: isAutoUpdatedStock,
            storageId: Number(formData.get("storageId")),
            isPriceLinked: priceLinkedUsing,
            pricingRate: Number(formData.get("pricingRate")),
            price: Number(formData.get("price")),
            memo: formData.get("memo") as string,
        }
        console.log(cardProductRegisterDto)
        api.post("/api/admin/product/single-products/card", cardProductRegisterDto).then(() => {
            alert('카드 등록 완료')
            setCurrentVisibleStock(0)
            setTotalStock(0)
            setMaxVisibleStock(8)
            setIsAutoUpdatedStock(true)
            setIsVisible(true)
            setPriceLinkedUsing(true)
            onOpenChange(false)
        }).catch((error) => {
            alert('카드 등록 실패' + (error.response.data as { message: string }).message)
            console.error(error)
        })

    }
    return (
        <Dialog>
            <DialogTrigger asChild>
                {trigger ? (
                    <div>{trigger}</div>
                ) : (
                    <Button>선택 카드 등록</Button>
                )}
            </DialogTrigger>
            <DialogContent className="sm:max-w-3xl">

                <DialogHeader>
                    <DialogTitle>선택 카드 등록</DialogTitle>
                    <DialogDescription>
                        선택 카드 정보를 입력해주세요.
                    </DialogDescription>
                </DialogHeader>
                <div className="flex flex-row gap-2">
                    <div className="flex flex-col w-1/3 gap-2">
                        <h2 className="text-lg font-bold">{selectedCard.cardName}-{selectedCard.printType}</h2>
                        <ProductImage
                            className="w-full object-contain"
                            src={imageUrl}
                            alt={selectedCard.cardName ?? ""}
                            width={250}
                            height={250}
                        />
                    </div>
                    <Separator orientation="vertical" />
                    <form className="flex flex-col gap-2" onSubmit={handleCardProductAdd}>
                        <div className="flex flex-col gap-2">
                            <Label htmlFor="productName">카드 이름</Label>
                            <Input name="productName" placeholder="카드 이름" defaultValue={selectedCard.cardName} readOnly />
                        </div>
                        <div className="flex flex-col gap-2">
                            <div className="flex flex-row gap-2">

                                <div className="w-1/3 flex flex-col gap-2">
                                    <Label htmlFor="condition">카드 상태</Label>
                                    <Select name="condition" defaultValue={gradePolicyList?.length > 0 ? gradePolicyList[0].grade : undefined}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="카드 상태" />
                                        </SelectTrigger>
                                        <SelectContent className="bg-white">
                                            {gradePolicyList?.map((gradePolicy) => (
                                                <SelectItem key={gradePolicy.id} value={gradePolicy.grade}>{gradePolicy.grade}</SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="w-1/3 flex flex-col gap-2">
                                    <Label htmlFor="language">언어</Label>
                                    <Select name="language" defaultValue="en">
                                        <SelectTrigger>
                                            <SelectValue placeholder="언어" />
                                        </SelectTrigger>
                                        <SelectContent className="bg-white">
                                            {languages.map((language) => (
                                                <SelectItem key={language.code} value={language.code}>
                                                    {language.displayName}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                                <div className="w-1/3 flex flex-col gap-2">
                                    <Label htmlFor="storageId">저장소</Label>
                                    <Select name="storageId" defaultValue={storageList?.length > 0 ? storageList[0].id.toString() : undefined}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="저장소를 선택해주세요." />
                                        </SelectTrigger>
                                        <SelectContent className="bg-white">
                                            {storageList?.map((storage) => (
                                                <SelectItem key={storage.id} value={storage.id.toString()}>{storage.storageName}</SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select></div>
                            </div>
                        </div>
                        <Label htmlFor="currentVisibleStock">표시 재고</Label>
                        <div className="flex flex-row gap-2">
                            <Input type="number" name="currentVisibleStock" placeholder="표시 재고" value={currentVisibleStock} onChange={(e) => setCurrentVisibleStock(Number(e.target.value))} required />
                            <ButtonGroup
                                orientation="horizontal"
                            >
                                <Button type="button" variant="outline" onClick={() => setCurrentVisibleStock(currentVisibleStock + 1)}>+</Button>
                                <Button type="button" variant="outline" onClick={() => setCurrentVisibleStock(1)}>1</Button>
                                <Button type="button" variant="outline" onClick={() => setCurrentVisibleStock(5)}>5</Button>
                                <Button type="button" variant="outline" onClick={() => setCurrentVisibleStock(10)}>10</Button>
                                <Button type="button" variant="outline" onClick={() => setCurrentVisibleStock(currentVisibleStock > 0 ? currentVisibleStock - 1 : 0)}>-</Button>
                            </ButtonGroup>
                        </div>
                        <Label htmlFor="totalStock">총 수량</Label>
                        <div className="flex flex-row gap-2">
                            <Input type="number" name="totalStock" placeholder="총 수량" value={totalStock} onChange={(e) => setTotalStock(Number(e.target.value))} required />
                            <ButtonGroup
                                orientation="horizontal"
                            >
                                <Button type="button" variant="outline" onClick={() => setTotalStock(totalStock + 1)}>+</Button>
                                <Button type="button" variant="outline" onClick={() => setTotalStock(1)}>1</Button>
                                <Button type="button" variant="outline" onClick={() => setTotalStock(5)}>5</Button>
                                <Button type="button" variant="outline" onClick={() => setTotalStock(10)}>10</Button>
                                <Button type="button" variant="outline" onClick={() => setTotalStock(totalStock - 1 > 0 ? totalStock - 1 : 0)}>-</Button>
                            </ButtonGroup>
                        </div>
                        <Label htmlFor="maxVisibleStock">최대 표시 수량</Label>
                        <div className="flex flex-row gap-2">
                            <Input type="number" name="maxVisibleStock" placeholder="최대 표시 수량" value={maxVisibleStock} onChange={(e) => setMaxVisibleStock(Number(e.target.value))} required />
                            <ButtonGroup
                                orientation="horizontal"
                            >
                                <Button type="button" variant="outline" onClick={() => setMaxVisibleStock(maxVisibleStock + 1)}>+</Button>
                                <Button type="button" variant="outline" onClick={() => setMaxVisibleStock(1)}>1</Button>
                                <Button type="button" variant="outline" onClick={() => setMaxVisibleStock(5)}>5</Button>
                                <Button type="button" variant="outline" onClick={() => setMaxVisibleStock(10)}>10</Button>
                                <Button type="button" variant="outline" onClick={() => setMaxVisibleStock(maxVisibleStock > 0 ? maxVisibleStock - 1 : 0)}>-</Button>
                            </ButtonGroup>
                        </div>
                        <div className="flex flex-row gap-2 items-center">
                            <div className="flex flex-row gap-2">
                                <Label htmlFor="isAutoUpdatedStock">자동 수량 업데이트 여부</Label>
                                <Checkbox
                                    id="isAutoUpdatedStock"
                                    name="isAutoUpdatedStock"
                                    checked={isAutoUpdatedStock}
                                    onCheckedChange={(checked) => setIsAutoUpdatedStock(checked === true)}
                                />
                            </div>
                            <Separator orientation="vertical" />
                            <div className="flex flex-row gap-2">
                                <Label htmlFor="isVisible">표시 여부</Label>
                                <Checkbox
                                    id="isVisible"
                                    name="isVisible"
                                    checked={isVisible}
                                    onCheckedChange={(checked) => setIsVisible(checked === true)}
                                />
                            </div>
                        </div>

                        <div className="flex flex-row gap-2">
                            <Label htmlFor="isPriceLinked">가격 연동 사용여부</Label>
                            <Checkbox
                                id="isPriceLinked"
                                name="isPriceLinked"
                                checked={priceLinkedUsing}
                                onCheckedChange={(checked) => setPriceLinkedUsing(checked === true)}
                            />
                            <span className="text-sm text-muted-foreground">
                                현재 가격: {selectedCard.price ?? "없음"}
                            </span>
                        </div>
                        <div className={`grid grid-cols-2 gap-2 items-center ${!priceLinkedUsing ? "hidden" : ""} `}>
                            <Label htmlFor="pricingRate">가격 연동 시 배율 (0.00 ~ 2.00)</Label>
                            <Input name="pricingRate" placeholder="가격 연동 시 배율" defaultValue={1.00} />
                        </div>
                        <div className={`grid grid-cols-2 gap-2 items-center ${!priceLinkedUsing ? "" : "hidden"} `}>
                            <Label htmlFor="price">수동가격 책정(원)</Label>
                            <Input name="price" placeholder="가격 연동 하지 않을 시 설정 가격" defaultValue={0} />
                        </div>
                        <Textarea name="memo" placeholder="메모" defaultValue={selectedCard.memo} />
                        <DialogFooter>
                            <Button type="submit">카드 등록</Button>
                            <DialogClose asChild>
                                <Button variant="outline">취소</Button>
                            </DialogClose>
                        </DialogFooter>
                        <Label htmlFor="warning" className="text-red-600">등록 전 언어를 확인해 주세요.<br />
                            한글 데이터가 있는 경우,  해당 판의 한글판이 없어도 등록될 수 있습니다.</Label>
                    </form>
                </div>
            </DialogContent>

        </Dialog>
    )
}
