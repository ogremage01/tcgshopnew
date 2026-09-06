"use client";

import { useEffect, useState } from "react";
import Image from "next/image";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RichTextEditor } from "@/components/ui/rich-text-editor";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { api, getApiErrorMessage } from "@/lib/api";
import { offlineProductIdForSubmit } from "@/lib/offline-product-id";
import { resolveAssetUrl } from "@/lib/public-asset-url";
import { OfflineProductLinkSelect } from "@/app/admin/products/_components/OfflineProductLinkSelect";
import type { MakerDto, SupplyDto, SupplyTypeDto } from "@/types/product";

const IMAGE_MAX_SIZE = 1 * 1024 * 1024;

type InputFile = {
  file: File;
  url: string;
};

export function SupplyEditDialog({
  supply,
  open,
  onOpenChange,
  onUpdated,
}: {
  supply: SupplyDto;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUpdated?: () => void;
}) {
  const [makerList, setMakerList] = useState<MakerDto[]>([]);
  const [supplyTypeList, setSupplyTypeList] = useState<SupplyTypeDto[]>([]);
  const [selectedMakerId, setSelectedMakerId] = useState<string>("");
  const [selectedSupplyTypeId, setSelectedSupplyTypeId] = useState<string>("");
  const [description, setDescription] = useState("");
  const [isVisible, setIsVisible] = useState(true);
  const [imageFile, setImageFile] = useState<InputFile | null>(null);
  const [selectedOfflineProductId, setSelectedOfflineProductId] = useState<
    string | undefined
  >(undefined);
  const [productPrice, setProductPrice] = useState<number>(0);

  useEffect(() => {
    if (!open) return;
    api
      .get<MakerDto[]>("/api/admin/product/supply-products/makers")
      .then((res) => setMakerList(res))
      .catch(console.error);

    api
      .get<SupplyTypeDto[]>("/api/admin/product/supply-products/supply-types")
      .then((res) => setSupplyTypeList(res))
      .catch(console.error);
  }, [open]);

  useEffect(() => {
    if (!open) {
      if (imageFile) URL.revokeObjectURL(imageFile.url);
      setImageFile(null);
      return;
    }

    setSelectedMakerId(
      supply.makerId != null ? String(supply.makerId) : "",
    );
    setSelectedSupplyTypeId(
      supply.supplyTypeId != null ? String(supply.supplyTypeId) : "",
    );
    setDescription(supply.description ?? "");
    setIsVisible(supply.isVisible ?? true);
    setSelectedOfflineProductId(supply.offlineProductId ?? undefined);
    setProductPrice(supply.price ?? 0);
    setImageFile(null);
  }, [open, supply]);

  useEffect(() => {
    return () => {
      if (imageFile) URL.revokeObjectURL(imageFile.url);
    };
  }, [imageFile]);

  const handleInputFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > IMAGE_MAX_SIZE) {
      alert(
        `최대 이미지 크기를 초과했습니다. ${IMAGE_MAX_SIZE / 1024 / 1024}MB 이하로 선택해주세요.`,
      );
      e.target.value = "";
      return;
    }

    if (imageFile) URL.revokeObjectURL(imageFile.url);
    setImageFile({ file, url: URL.createObjectURL(file) });
  };

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!selectedMakerId || !selectedSupplyTypeId) {
      alert("제조사와 서플라이 타입을 선택해주세요.");
      return;
    }

    const formData = new FormData(e.currentTarget);
    const fd = new FormData();
    fd.append("nameKo", String(formData.get("nameKo") ?? ""));
    fd.append("nameEn", String(formData.get("nameEn") ?? ""));
    fd.append("makerId", selectedMakerId);
    fd.append("supplyTypeId", selectedSupplyTypeId);
    fd.append("price", String(productPrice));
    fd.append("stock", String(formData.get("stock") ?? ""));
    fd.append("description", description);
    fd.append("isVisible", String(isVisible));
    if (imageFile) {
      fd.append("imageFile", imageFile.file);
    }
    fd.append(
      "offlineProductId",
      offlineProductIdForSubmit(selectedOfflineProductId),
    );

    api
      .putFormData(`/api/admin/product/supply-products/${supply.id}`, fd)
      .then(() => {
        alert("서플라이 수정 완료");
        onUpdated?.();
        onOpenChange(false);
      })
      .catch((err) => {
        alert(`서플라이 수정 실패 ${getApiErrorMessage(err) ?? ""}`);
        console.error(err);
      });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>서플라이 수정</DialogTitle>
          <DialogDescription>서플라이 상품 정보를 수정해주세요.</DialogDescription>
        </DialogHeader>
        <form
          key={`edit-supply-${supply.id}`}
          className="flex flex-col gap-3"
          onSubmit={handleSubmit}
        >
          <div className="flex flex-col gap-1">
            <Label>한글명</Label>
            <Input
              name="nameKo"
              placeholder="한글명"
              required
              defaultValue={supply.nameKo}
            />
          </div>
          <div className="flex flex-col gap-1">
            <Label>영문명</Label>
            <Input
              name="nameEn"
              placeholder="영문명"
              required
              defaultValue={supply.nameEn}
            />
          </div>
          <div className="flex flex-col gap-1">
            <Label>제조사</Label>
            <Select
              value={selectedMakerId || undefined}
              onValueChange={setSelectedMakerId}
            >
              <SelectTrigger>
                <SelectValue placeholder="제조사 선택" />
              </SelectTrigger>
              <SelectContent>
                {makerList.map((maker) => (
                  <SelectItem key={maker.id} value={String(maker.id)}>
                    {maker.name}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="flex flex-col gap-1">
            <Label>서플라이 타입</Label>
            <Select
              value={selectedSupplyTypeId || undefined}
              onValueChange={setSelectedSupplyTypeId}
            >
              <SelectTrigger>
                <SelectValue placeholder="서플라이 타입 선택" />
              </SelectTrigger>
              <SelectContent>
                {supplyTypeList.map((type) => (
                  <SelectItem key={type.id} value={String(type.id)}>
                    {type.nameKo}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="flex gap-2">
            <div className="flex flex-1 flex-col gap-1">
              <Label>가격</Label>
              <Input
                name="price"
                type="number"
                min={0}
                placeholder="가격"
                required
                defaultValue={supply.price}
                onChange={(e) => setProductPrice(Number(e.target.value))}
              />
            </div>
            <div className="flex flex-1 flex-col gap-1">
              <Label>재고</Label>
              <Input
                name="stock"
                type="number"
                min={0}
                placeholder="재고"
                required
                defaultValue={supply.stock}
              />
            </div>
          </div>
          <div className="flex flex-col gap-1">
            <Label>설명</Label>
            <RichTextEditor
              value={description}
              onChange={setDescription}
              placeholder="상품 설명 입력"
            />
          </div>
          <div className="flex items-center gap-2">
            <Checkbox
              id="supply-edit-isVisible"
              checked={isVisible}
              onCheckedChange={(checked) => setIsVisible(checked === true)}
            />
            <Label htmlFor="supply-edit-isVisible">공개</Label>
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
            {supply.imgUrl && !imageFile ? (
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
                      URL.revokeObjectURL(imageFile.url);
                      setImageFile(null);
                    }}
                    className="w-full"
                  >
                    삭제
                  </Button>
                </div>
              </div>
            </div>
          ) : supply.imgUrl ? (
            <div className="flex flex-col gap-2">
              <Label>현재 이미지</Label>
              <Image
                src={resolveAssetUrl(supply.imgUrl)}
                alt={supply.nameKo ?? "상품 이미지"}
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
  );
}
