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
import { Separator } from "@/components/ui/separator";
import { RichTextEditor } from "@/components/ui/rich-text-editor";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select-bgwhite";
import { api } from "@/lib/api";
import { offlineProductIdForSubmit } from "@/lib/offline-product-id";
import { OfflineProductLinkSelect } from "@/app/admin/products/_components/OfflineProductLinkSelect";
import type { ProductCategoryDto, ProductIpDto } from "@/types/product";
import { MANUAL_PRODUCT_DESCRIPTION_MAX_LENGTH } from "../_constants";

type InputFile = {
  file: File;
  url: string;
};

const IMAGE_MAX_SIZE = 1 * 1024 * 1024;
const IMAGE_MAX_COUNT = 1;

export function ProductAddDialog({
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
  const [isVisible, setIsVisible] = useState<boolean>(true);
  const [categoryList, setCategoryList] = useState<ProductCategoryDto[]>([]);
  const [productIpList, setProductIpList] = useState<ProductIpDto[]>([]);
  const [selectedProductType, setSelectedProductType] = useState<string>("");
  const [selectedProductIp, setSelectedProductIp] = useState<string>("");
  const [inputImageList, setInputImageList] = useState<InputFile[]>([]);
  const [formKey, setFormKey] = useState(0);
  const [selectedOfflineProductId, setSelectedOfflineProductId] = useState<
    string | undefined
  >(initialOfflineProductId);

  useEffect(() => {
    if (!open) return;
    api
      .get<ProductCategoryDto[]>("/api/admin/product/product-categories")
      .then((res) => setCategoryList(res))
      .catch(() => setCategoryList([]));
    api
      .get<ProductIpDto[]>("/api/admin/product/product-ips")
      .then((res) => setProductIpList(res))
      .catch(() => setProductIpList([]));
  }, [open]);

  const handleReset = () => {
    inputImageList.forEach((image) => URL.revokeObjectURL(image.url));
    setInputImageList([]);
    setSelectedProductType("");
    setSelectedProductIp("");
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

  const handleInputFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;

    if (inputImageList.length + files.length > IMAGE_MAX_COUNT) {
      alert(
        `최대 이미지 개수를 초과했습니다. ${IMAGE_MAX_COUNT}장 이하로 선택해주세요.`,
      );
      return;
    }

    if (files.some((file) => file.size > IMAGE_MAX_SIZE)) {
      alert(
        `최대 이미지 크기를 초과했습니다. ${IMAGE_MAX_SIZE / 1024 / 1024}MB 이하로 선택해주세요.`,
      );
      return;
    }

    const newFiles = files.map((file) => ({
      file,
      url: URL.createObjectURL(file),
    }));
    setInputImageList((prev) => [...prev, ...newFiles]);
  };

  const handleRemove = (url: string) => {
    URL.revokeObjectURL(url);
    setInputImageList((prev) => prev.filter((image) => image.url !== url));
  };

  useEffect(() => {
    return () => {
      inputImageList.forEach((image) => URL.revokeObjectURL(image.url));
    };
  }, [inputImageList]);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = e.target as HTMLFormElement;
    const formData = new FormData(form);

    if (!selectedProductType || !selectedProductIp) {
      alert("상품 카테고리와 제품 IP를 선택해주세요.");
      return;
    }

    const description = String(formData.get("description") ?? "").trim();
    if (!description) {
      alert("상품 설명을 입력해주세요.");
      return;
    }
    if (description.length > MANUAL_PRODUCT_DESCRIPTION_MAX_LENGTH) {
      alert(
        `상품 설명은 ${MANUAL_PRODUCT_DESCRIPTION_MAX_LENGTH}자 이내로 입력해주세요.`,
      );
      return;
    }

    const imageFile = inputImageList[0]?.file;
    if (!imageFile) {
      alert("상품 이미지를 선택해주세요.");
      return;
    }

    const fd = new FormData();
    fd.append("nameEn", String(formData.get("nameEn") ?? ""));
    fd.append("nameKo", String(formData.get("nameKo") ?? ""));
    fd.append("description", description);
    fd.append("price", String(formData.get("price") ?? ""));
    fd.append("stock", String(formData.get("stock") ?? ""));
    fd.append("productType", selectedProductType);
    fd.append("productIp", selectedProductIp);
    fd.append("isVisible", String(isVisible));
    fd.append("imageFile", imageFile);
    fd.append(
      "offlineProductId",
      offlineProductIdForSubmit(selectedOfflineProductId),
    );

    api
      .postFormData("/api/admin/product/manual-products", fd)
      .then(() => {
        alert("상품 등록 완료");
        onAdded?.();
        onOpenChange(false);
      })
      .catch((error) => {
        alert(
          "상품 등록 실패" +
            (error.response?.data as { message: string })?.message,
        );
        console.error(error);
      });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>상품 등록</DialogTitle>
          <DialogDescription>상품 정보를 입력해주세요.</DialogDescription>
        </DialogHeader>
        <form
          key={formKey}
          className="flex flex-col gap-2"
          onSubmit={handleSubmit}
        >
          <div className="flex flex-col gap-2">
            <Label>상품 카테고리</Label>
            <Select
              value={selectedProductType || undefined}
              onValueChange={setSelectedProductType}
              required
            >
              <SelectTrigger>
                <SelectValue placeholder="상품 카테고리" />
              </SelectTrigger>
              <SelectContent>
                {categoryList.map((category) => (
                  <SelectItem key={category.id} value={category.nameEn}>
                    {category.nameEn}
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
              required
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
          <Input
            type="text"
            name="nameKo"
            placeholder="상품 이름(한글)"
            defaultValue={initialProductNameEn}
            required
          />
          <Label>상품 이름(영어)</Label>
          <Input
            type="text"
            name="nameEn"
            defaultValue={initialProductNameEn}
            placeholder="상품 이름(영어)"
            required
          />
          <div className="flex flex-row gap-2">
            <div className="flex flex-1 flex-col gap-2">
              <Label>상품 가격</Label>
              <Input
                type="number"
                defaultValue={initialProductPrice?.toString()}
                name="price"
                placeholder="상품 가격"
                required
              />
            </div>
            <div className="flex flex-1 flex-col gap-2">
              <Label>상품 수량</Label>
              <Input
                type="number"
                name="stock"
                placeholder="상품 수량"
                required
              />
            </div>
          </div>
          <Label>
            상품 설명 (최대 {MANUAL_PRODUCT_DESCRIPTION_MAX_LENGTH}자)
          </Label>
          <RichTextEditor
            name="description"
            placeholder="상품 설명"
            maxLength={MANUAL_PRODUCT_DESCRIPTION_MAX_LENGTH}
          />
          <Label>
            상품 이미지 ※ webp, png 파일 전용 / 최대 이미지 개수:{" "}
            {IMAGE_MAX_COUNT}장 / 최대 이미지 크기:{" "}
            {IMAGE_MAX_SIZE / 1024 / 1024}MB
          </Label>
          <Input
            type="file"
            name="image"
            placeholder="상품 이미지"
            accept=".webp, .png"
            multiple
            onChange={handleInputFileChange}
          />
          <div className="flex items-center gap-2">
            <Checkbox
              id="add-isVisible"
              checked={isVisible}
              onCheckedChange={(checked) => setIsVisible(checked === true)}
            />
            <Label htmlFor="add-isVisible">상품 표시 여부</Label>
          </div>
          <OfflineProductLinkSelect
            value={selectedOfflineProductId}
            onValueChange={setSelectedOfflineProductId}
          />
          <div className="flex gap-2">
            <Button type="submit">등록</Button>
            <Button type="button" variant="outline" onClick={handleReset}>
              초기화
            </Button>
          </div>
        </form>
        <Separator />
        <Label>상품 이미지 미리보기</Label>
        <div className="flex flex-row gap-2 rounded-md bg-orange-100 p-4">
          {inputImageList.map((image) => (
            <div key={image.url} className="flex flex-col gap-2">
              <Image
                src={image.url}
                alt={image.file.name}
                width={100}
                height={100}
              />
              <p className="text-sm text-gray-500">{image.file.name}</p>
              <Button
                type="button"
                variant="outline"
                onClick={() => handleRemove(image.url)}
                className="w-full"
              >
                삭제
              </Button>
            </div>
          ))}
          {inputImageList.length === 0 && (
            <p className="text-sm text-gray-400">이미지를 선택해주세요.</p>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
