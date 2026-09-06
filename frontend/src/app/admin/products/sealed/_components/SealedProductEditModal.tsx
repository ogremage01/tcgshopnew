"use client";

import { useEffect, useMemo, useState } from "react";
import Image from "next/image";
import { Button } from "@/components/ui/button";
import {
  Combobox,
  ComboboxContent,
  ComboboxEmpty,
  ComboboxInput,
  ComboboxItem,
  ComboboxList,
} from "@/components/ui/combobox";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
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
import type { ProductIpDto, SealedProductAdminDto } from "@/types/product";
import { OfflineProductLinkSelect } from "@/app/admin/products/_components/OfflineProductLinkSelect";
import { fromGameName } from "@/config/gameEnum";
import { fetchSetListForGame, type GameSetInfoDto } from "@/lib/game-set-list";

const IMAGE_MAX_SIZE = 1 * 1024 * 1024;
const EMPTY_SET_CODE_PREFIX = "__EMPTY_SET_CODE__:";

type InputFile = {
  file: File;
  url: string;
};

type SetOption = {
  value: string;
  label: string;
};

export function SealedProductEditModal({
  product,
  open,
  onOpenChange,
  onUpdated,
}: {
  product: SealedProductAdminDto;
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onUpdated?: () => void;
}) {
  const [productIpList, setProductIpList] = useState<ProductIpDto[]>([]);
  const [selectedGame, setSelectedGame] = useState("");
  const [setList, setSetList] = useState<GameSetInfoDto[]>([]);
  const [selectedSet, setSelectedSet] = useState<string | undefined>(undefined);
  const [isActive, setIsActive] = useState(true);
  const [language, setLanguage] = useState(product.language ?? "en");
  const [imageFile, setImageFile] = useState<InputFile | null>(null);
  const [selectedOfflineProductId, setSelectedOfflineProductId] = useState<
    string | undefined
  >(product.offlineProductId ?? undefined);

  const selectedGameOption = useMemo(
    () => fromGameName(selectedGame) ?? null,
    [selectedGame],
  );

  useEffect(() => {
    if (!open) return;
    api
      .get<ProductIpDto[]>("/api/admin/product/product-ips")
      .then((res) => setProductIpList(res))
      .catch(() => setProductIpList([]));
  }, [open]);

  useEffect(() => {
    if (!open) {
      if (imageFile) URL.revokeObjectURL(imageFile.url);
      setImageFile(null);
      return;
    }

    setSelectedGame(product.game ?? "");
    setSelectedSet(product.setCode || undefined);
    setIsActive(product.isActive ?? true);
    setLanguage(product.language ?? "en");
    setSelectedOfflineProductId(product.offlineProductId ?? undefined);
    setImageFile(null);
  }, [open, product]);

  useEffect(() => {
    if (!selectedGameOption) {
      setSetList([]);
      return;
    }

    setSetList([]);

    void fetchSetListForGame(selectedGameOption).then((res) => {
      setSetList(res);
    });
  }, [selectedGameOption]);

  useEffect(() => {
    return () => {
      if (imageFile) URL.revokeObjectURL(imageFile.url);
    };
  }, [imageFile]);

  const setOptions = useMemo<SetOption[]>(() => {
    return setList.map((item, index) => {
      const setCode = item.setCode || `${EMPTY_SET_CODE_PREFIX}${index}`;
      const label = "name" in item ? item.name : item.setName;
      return {
        value: setCode,
        label: label
          ? `${item.setCode || "없음"} - ${label}`
          : item.setCode || "없음",
      };
    });
  }, [setList]);

  const selectedSetOption = useMemo(() => {
    if (!selectedSet) return null;
    return setOptions.find((option) => option.value === selectedSet) ?? null;
  }, [selectedSet, setOptions]);

  const selectedSetName = useMemo(() => {
    if (!selectedSet || selectedSet.startsWith(EMPTY_SET_CODE_PREFIX))
      return "";
    const item = setList.find((i) => i.setCode === selectedSet);
    if (!item) return "";
    return "name" in item ? item.name : item.setName;
  }, [selectedSet, setList]);

  const resolvedSetCode = useMemo(() => {
    if (!selectedSet || selectedSet.startsWith(EMPTY_SET_CODE_PREFIX))
      return "";
    return selectedSet;
  }, [selectedSet]);

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

    if (!selectedGame) {
      alert("제품 IP를 선택해주세요.");
      return;
    }

    const formData = new FormData(e.currentTarget);
    const fd = new FormData();
    fd.append("productNameEn", String(formData.get("productNameEn") ?? ""));
    fd.append("productNameKo", String(formData.get("productNameKo") ?? ""));
    fd.append("game", selectedGame);
    fd.append("setName", selectedSetName);
    fd.append("setCode", resolvedSetCode);
    fd.append("price", String(formData.get("price") ?? ""));
    fd.append(
      "currentVisibleStock",
      String(formData.get("currentVisibleStock") ?? ""),
    );
    fd.append("totalStock", String(formData.get("totalStock") ?? ""));
    fd.append("maxVisibleStock", String(formData.get("maxVisibleStock") ?? ""));
    fd.append("language", language);
    fd.append("isActive", String(isActive));
    if (imageFile) {
      fd.append("imageFile", imageFile.file);
    }
    fd.append(
      "offlineProductId",
      offlineProductIdForSubmit(selectedOfflineProductId),
    );

    api
      .putFormData(`/api/admin/product/sealed-products/${product.id}`, fd)
      .then(() => {
        alert("밀봉 상품 수정 완료");
        onUpdated?.();
        onOpenChange(false);
      })
      .catch((err) => {
        alert(`밀봉 상품 수정 실패 ${getApiErrorMessage(err) ?? ""}`);
        console.error(err);
      });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>밀봉 상품 수정</DialogTitle>
          <DialogDescription>상품 정보를 수정해주세요.</DialogDescription>
        </DialogHeader>
        <form
          key={`edit-sealed-${product.id}`}
          className="flex flex-col gap-3"
          onSubmit={handleSubmit}
        >
          <div className="flex flex-col gap-1">
            <Label>제품 IP</Label>
            <Select
              value={selectedGame || undefined}
              onValueChange={(value) => {
                setSelectedGame(value);
                setSelectedSet(undefined);
              }}
              required
            >
              <SelectTrigger>
                <SelectValue placeholder="제품 IP 선택" />
              </SelectTrigger>
              <SelectContent>
                {productIpList.map((productIp) => (
                  <SelectItem key={productIp.id} value={productIp.nameEn}>
                    {productIp.nameEn}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex flex-col gap-1">
            <Label>세트</Label>
            <Combobox
              items={setOptions}
              value={selectedSetOption}
              onValueChange={(option) => setSelectedSet(option?.value)}
              disabled={!selectedGameOption}
              isItemEqualToValue={(a, b) => a.value === b.value}
              itemToStringLabel={(item) => item.label}
              itemToStringValue={(item) => item.value}
            >
              <ComboboxInput
                placeholder="Set name"
                disabled={!selectedGameOption}
                showClear
              />
              <ComboboxContent className="bg-white p-0">
                <ComboboxEmpty>No results found.</ComboboxEmpty>
                <ComboboxList className="max-h-60 overflow-y-auto">
                  {(item) => (
                    <ComboboxItem key={item.value} value={item}>
                      {item.label}
                    </ComboboxItem>
                  )}
                </ComboboxList>
              </ComboboxContent>
            </Combobox>
          </div>
          <div className="flex flex-col gap-1">
            <Label>언어</Label>
            <Select
              value={language || undefined}
              onValueChange={setLanguage}
              required
            >
              <SelectTrigger>
                <SelectValue placeholder="언어" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="en">English</SelectItem>
                <SelectItem value="ko">Korean</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div className="flex flex-col gap-1">
            <Label>제품명 (한글)</Label>
            <Input
              name="productNameKo"
              defaultValue={product.productNameKo}
              required
            />
          </div>

          <div className="flex flex-col gap-1">
            <Label>제품명 (영문)</Label>
            <Input
              name="productNameEn"
              defaultValue={product.productNameEn}
              required
            />
          </div>

          <div className="flex gap-2">
            <div className="flex flex-1 flex-col gap-1">
              <Label>가격</Label>
              <Input
                name="price"
                type="number"
                min={0}
                defaultValue={product.price}
                required
              />
            </div>
          </div>

          <div className="flex gap-2">
            <div className="flex flex-1 flex-col gap-1">
              <Label>표시 재고</Label>
              <Input
                name="currentVisibleStock"
                type="number"
                min={0}
                defaultValue={product.currentVisibleStock}
              />
            </div>
            <div className="flex flex-1 flex-col gap-1">
              <Label>총 재고</Label>
              <Input
                name="totalStock"
                type="number"
                min={0}
                defaultValue={product.totalStock}
              />
            </div>
            <div className="flex flex-1 flex-col gap-1">
              <Label>최대 공개 재고</Label>
              <Input
                name="maxVisibleStock"
                type="number"
                min={0}
                defaultValue={product.maxVisibleStock}
              />
            </div>
          </div>
          <OfflineProductLinkSelect
            value={selectedOfflineProductId}
            onValueChange={setSelectedOfflineProductId}
          />

          <div className="flex items-center gap-2">
            <Checkbox
              id="edit-sealed-isActive"
              checked={isActive}
              onCheckedChange={(checked) => setIsActive(checked === true)}
            />
            <Label htmlFor="edit-sealed-isActive">활성</Label>
          </div>

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
            {product.imageUrl && !imageFile ? (
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
          ) : product.imageUrl ? (
            <div className="flex flex-col gap-2">
              <Label>현재 이미지</Label>
              <Image
                src={resolveAssetUrl(product.imageUrl)}
                alt={product.productNameKo ?? "상품 이미지"}
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
