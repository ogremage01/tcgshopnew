"use client";

import { useEffect, useMemo, useState } from "react";
import Image from "next/image";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
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
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Separator } from "@/components/ui/separator";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { OfflineProductLinkSelect } from "@/app/admin/products/_components/OfflineProductLinkSelect";
import { fromGameName } from "@/config/gameEnum";
import { fetchSetListForGame, type GameSetInfoDto } from "@/lib/game-set-list";
import { offlineProductIdForSubmit } from "@/lib/offline-product-id";
import type { ProductIpDto } from "@/types/product";

type SetOption = {
  value: string;
  label: string;
};

type InputFile = {
  file: File;
  url: string;
};

const EMPTY_SET_CODE_PREFIX = "__EMPTY_SET_CODE__:";

const IMAGE_MAX_SIZE = 1 * 1024 * 1024;

export function SealedProductAddDialog({
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
  const [productIpList, setProductIpList] = useState<ProductIpDto[]>([]);
  const [selectedProductIp, setSelectedProductIp] = useState("");
  const [setList, setSetList] = useState<GameSetInfoDto[]>([]);
  const [selectedSet, setSelectedSet] = useState<string | undefined>(undefined);

  const [productNameEn, setProductNameEn] = useState(
    initialProductNameEn ?? "",
  );
  const [productNameKo, setProductNameKo] = useState(
    initialProductNameEn ?? "",
  );
  const [price, setPrice] = useState(
    initialProductPrice != null ? String(initialProductPrice) : "",
  );
  const [currentVisibleStock, setCurrentVisibleStock] = useState("");
  const [totalStock, setTotalStock] = useState("");
  const [maxVisibleStock, setMaxVisibleStock] = useState("");
  const [inputImageList, setInputImageList] = useState<InputFile[]>([]);
  const [language, setLanguage] = useState("en");
  const [isActive, setIsActive] = useState(true);
  const [selectedOfflineProductId, setSelectedOfflineProductId] = useState<
    string | undefined
  >(initialOfflineProductId);
  const selectedGame = useMemo(
    () => fromGameName(selectedProductIp) ?? null,
    [selectedProductIp],
  );

  useEffect(() => {
    if (!open) return;
    api
      .get<ProductIpDto[]>("/api/admin/product/product-ips")
      .then((res) => setProductIpList(res))
      .catch(() => setProductIpList([]));
  }, [open]);

  useEffect(() => {
    if (!selectedGame) {
      setSetList([]);
      setSelectedSet(undefined);
      return;
    }

    setSetList([]);
    setSelectedSet(undefined);

    void fetchSetListForGame(selectedGame).then((res) => {
      setSetList(res);
    });
  }, [selectedGame]);

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
    const files = Array.from(e.target.files || []);
    if (files.length === 0) return;

    if (inputImageList.length + files.length > 1) {
      alert("이미지는 1장만 등록할 수 있습니다.");
      e.target.value = "";
      return;
    }

    if (files.some((file) => file.size > IMAGE_MAX_SIZE)) {
      alert(
        `최대 이미지 크기를 초과했습니다. ${IMAGE_MAX_SIZE / 1024 / 1024}MB 이하로 선택해주세요.`,
      );
      e.target.value = "";
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

  const handleReset = () => {
    setSelectedProductIp("");
    setSelectedSet(undefined);
    setProductNameEn(initialProductNameEn ?? "");
    setProductNameKo(initialProductNameEn ?? "");
    setPrice(initialProductPrice != null ? String(initialProductPrice) : "");
    setCurrentVisibleStock("");
    setTotalStock("");
    setMaxVisibleStock("");
    setLanguage("en");
    setIsActive(true);
    setSelectedOfflineProductId(initialOfflineProductId);
    inputImageList.forEach((image) => URL.revokeObjectURL(image.url));
    setInputImageList([]);
  };

  useEffect(() => {
    if (open) {
      setProductNameEn(initialProductNameEn ?? "");
      setProductNameKo(initialProductNameEn ?? "");
      setPrice(initialProductPrice != null ? String(initialProductPrice) : "");
      setSelectedOfflineProductId(initialOfflineProductId);
    } else {
      handleReset();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [
    open,
    initialOfflineProductId,
    initialProductNameEn,
    initialProductPrice,
  ]);

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!selectedProductIp) {
      alert("제품 IP를 선택해주세요.");
      return;
    }
    if (!productNameEn.trim()) {
      alert("제품명(영문)을 입력해주세요.");
      return;
    }
    if (!productNameKo.trim()) {
      alert("제품명(한글)을 입력해주세요.");
      return;
    }
    if (!price || Number(price) < 0) {
      alert("올바른 가격을 입력해주세요.");
      return;
    }

    const imageFile = inputImageList[0]?.file;
    if (!imageFile) {
      alert("상품 이미지를 선택해주세요.");
      return;
    }

    const fd = new FormData();
    fd.append("productNameEn", productNameEn);
    fd.append("productNameKo", productNameKo);
    fd.append("game", selectedProductIp);
    fd.append("setName", selectedSetName);
    fd.append("setCode", resolvedSetCode);
    fd.append("price", price);
    fd.append("currentVisibleStock", currentVisibleStock || "0");
    fd.append("totalStock", totalStock || "0");
    fd.append("maxVisibleStock", maxVisibleStock || "0");
    fd.append("imageFile", imageFile);
    fd.append("language", language);
    fd.append("isActive", String(isActive));
    fd.append(
      "offlineProductId",
      offlineProductIdForSubmit(selectedOfflineProductId),
    );
    api
      .postFormData("/api/admin/product/sealed-products", fd)
      .then(() => {
        alert("밀봉 상품 등록 완료");
        onAdded?.();
        onOpenChange(false);
      })
      .catch((error) => {
        const msg =
          (error.response?.data as { message?: string })?.message ?? "";
        alert("밀봉 상품 등록 실패" + (msg ? `: ${msg}` : ""));
        console.error(error);
        inputImageList.forEach((image) => URL.revokeObjectURL(image.url));
        setInputImageList([]);
      });
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] overflow-y-auto sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>밀봉 상품 등록</DialogTitle>
          <DialogDescription>
            새 밀봉 상품 정보를 입력해주세요.
          </DialogDescription>
        </DialogHeader>
        <form className="flex w-full flex-col gap-4" onSubmit={handleSubmit}>
          <div className="flex flex-col gap-1">
            <Label>제품 IP</Label>
            <Select
              value={selectedProductIp || undefined}
              onValueChange={setSelectedProductIp}
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

          <Combobox
            items={setOptions}
            value={selectedSetOption}
            onValueChange={(option) => setSelectedSet(option?.value)}
            disabled={!selectedGame}
            isItemEqualToValue={(a, b) => a.value === b.value}
            itemToStringLabel={(item) => item.label}
            itemToStringValue={(item) => item.value}
          >
            <ComboboxInput
              placeholder="Set name"
              disabled={!selectedGame}
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

          <div className="flex flex-col gap-1">
            <Label>제품명 (영문)</Label>
            <Input
              placeholder="제품명 (영문)"
              value={productNameEn}
              onChange={(e) => setProductNameEn(e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-1">
            <Label>제품명 (한글)</Label>
            <Input
              placeholder="제품명 (한글)"
              value={productNameKo}
              onChange={(e) => setProductNameKo(e.target.value)}
              required
            />
          </div>

          <div className="flex flex-col gap-1">
            <Label>언어</Label>
            <Select value={language} onValueChange={setLanguage} required>
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
            <Label>가격</Label>
            <Input
              placeholder="가격"
              type="number"
              min={0}
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              required
            />
          </div>

          <div className="flex gap-2">
            <div className="flex flex-1 flex-col gap-1">
              <Label>표시 재고</Label>
              <Input
                placeholder="표시 재고"
                type="number"
                min={0}
                value={currentVisibleStock}
                onChange={(e) => setCurrentVisibleStock(e.target.value)}
              />
            </div>
            <div className="flex flex-1 flex-col gap-1">
              <Label>총 재고</Label>
              <Input
                placeholder="총 재고"
                type="number"
                min={0}
                value={totalStock}
                onChange={(e) => setTotalStock(e.target.value)}
              />
            </div>
            <div className="flex flex-1 flex-col gap-1">
              <Label>최대 공개 재고</Label>
              <Input
                placeholder="최대 공개 재고"
                type="number"
                min={0}
                value={maxVisibleStock}
                onChange={(e) => setMaxVisibleStock(e.target.value)}
              />
            </div>
          </div>
          <OfflineProductLinkSelect
            value={selectedOfflineProductId}
            onValueChange={setSelectedOfflineProductId}
          />

          <div className="flex items-center gap-2">
            <Checkbox
              id="add-sealed-isActive"
              checked={isActive}
              onCheckedChange={(checked) => setIsActive(checked === true)}
            />
            <Label htmlFor="add-sealed-isActive">활성</Label>
          </div>

          <div className="flex flex-col gap-1">
            <Label>
              상품 이미지 ※ webp, png 전용 / 최대 1장 / 최대{" "}
              {IMAGE_MAX_SIZE / 1024 / 1024}MB
            </Label>
            <Input
              type="file"
              accept=".webp,.png"
              onChange={handleInputFileChange}
            />
          </div>

          <div className="flex flex-row gap-2">
            <Button type="submit" className="w-fit">
              Save
            </Button>
            <Button
              variant="outline"
              className="w-fit"
              type="button"
              onClick={handleReset}
            >
              Reset
            </Button>
          </div>
        </form>

        <Separator />

        <div className="flex flex-col gap-2">
          <Label>상품 이미지 미리보기</Label>
          <div className="flex w-full flex-row gap-2 rounded-md bg-orange-100 p-4">
            {inputImageList.map((image) => (
              <div key={image.url} className="flex flex-col gap-2">
                <Image
                  src={image.url}
                  alt={image.file.name}
                  width={100}
                  height={100}
                  className="object-contain"
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
        </div>
      </DialogContent>
    </Dialog>
  );
}
