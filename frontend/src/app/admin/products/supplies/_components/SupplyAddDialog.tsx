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
import { OfflineProductLinkSelect } from "@/app/admin/products/_components/OfflineProductLinkSelect";
import type { MakerDto, SupplyTypeDto } from "@/types/product";

const IMAGE_MAX_SIZE = 1 * 1024 * 1024;

type InputFile = {
  file: File;
  url: string;
};

export function SupplyAddDialog({
  open,
  onOpenChange,
  onAdded,
  initialOfflineProductId,
  initialProductNameEn,
  initialProductPrice,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onAdded?: () => void;
  initialOfflineProductId?: string;
  initialProductNameEn?: string;
  initialProductPrice?: number;
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
  >(initialOfflineProductId);
  const [formKey, setFormKey] = useState(0);
  useEffect(() => {
    api
      .get<MakerDto[]>("/api/admin/product/supply-products/makers")
      .then((res) => setMakerList(res))
      .catch(console.error);

    api
      .get<SupplyTypeDto[]>("/api/admin/product/supply-products/supply-types")
      .then((res) => setSupplyTypeList(res))
      .catch(console.error);
  }, []);

  const handleReset = () => {
    if (imageFile) URL.revokeObjectURL(imageFile.url);
    setImageFile(null);
    setSelectedMakerId("");
    setSelectedSupplyTypeId("");
    setDescription("");
    setIsVisible(true);
    setSelectedOfflineProductId(initialOfflineProductId);
    setFormKey((prev) => prev + 1);
  };

  useEffect(() => {
    if (open) {
      setSelectedOfflineProductId(initialOfflineProductId);
    } else {
      handleReset();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, initialOfflineProductId]);

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
    if (!imageFile) {
      alert("이미지를 선택해주세요.");
      return;
    }

    const formData = new FormData(e.currentTarget);
    const fd = new FormData();
    fd.append("nameKo", String(formData.get("nameKo") ?? ""));
    fd.append("nameEn", String(formData.get("nameEn") ?? ""));
    fd.append("makerId", selectedMakerId);
    fd.append("supplyTypeId", selectedSupplyTypeId);
    fd.append("price", String(formData.get("price") ?? ""));
    fd.append("stock", String(formData.get("stock") ?? ""));
    fd.append("description", description);
    fd.append("isVisible", String(isVisible));
    fd.append("imageFile", imageFile.file);
    fd.append(
      "offlineProductId",
      offlineProductIdForSubmit(selectedOfflineProductId),
    );

    api
      .postFormData("/api/admin/product/supply-products", fd)
      .then(() => {
        alert("서플라이 등록 완료");
        onAdded?.();
        onOpenChange(false);
      })
      .catch((err) => {
        alert(`서플라이 등록 실패 ${getApiErrorMessage(err) ?? ""}`);
        console.error(err);
      });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>서플라이 등록</DialogTitle>
          <DialogDescription>
            새 서플라이 상품 정보를 입력해주세요.
          </DialogDescription>
        </DialogHeader>
        <form
          key={formKey}
          className="flex flex-col gap-3"
          onSubmit={handleSubmit}
        >
          <div className="flex flex-col gap-1">
            <Label>한글명</Label>
            <Input
              name="nameKo"
              placeholder="한글명"
              required
              defaultValue={initialProductNameEn}
            />
          </div>
          <div className="flex flex-col gap-1">
            <Label>영문명</Label>
            <Input
              name="nameEn"
              placeholder="영문명"
              required
              defaultValue={initialProductNameEn}
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
                defaultValue={initialProductPrice?.toString()}
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
              id="supply-isVisible"
              checked={isVisible}
              onCheckedChange={(checked) => setIsVisible(checked === true)}
            />
            <Label htmlFor="supply-isVisible">공개</Label>
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
          </div>
          {imageFile && (
            <div className="flex flex-col gap-2">
              <Label>이미지 미리보기</Label>
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
          )}
          <div className="flex gap-2">
            <Button type="submit">등록</Button>
            <Button type="button" variant="outline" onClick={handleReset}>
              초기화
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  );
}
