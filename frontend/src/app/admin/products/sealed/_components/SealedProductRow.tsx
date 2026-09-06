"use client";

import { useEffect, useRef, useState } from "react";
import Image from "next/image";
import { TableRow, TableCell } from "@/components/ui/table";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { api, getApiErrorMessage } from "@/lib/api";
import { offlineProductIdForSubmit } from "@/lib/offline-product-id";
import { resolveAssetUrl } from "@/lib/public-asset-url";
import type { SealedProductAdminDto } from "@/types/product";
import { fromGameName } from "@/config/gameEnum";
import { languageShortLabel } from "@/lib/card-product-language";
import { toast } from "sonner";

function sealedGameShortLabel(game: string | undefined): string {
  return fromGameName(game)?.displayAbbr ?? game ?? "";
}

type NumericDraft = {
  price: number;
  currentVisibleStock: number;
  totalStock: number;
  maxVisibleStock: number;
};

type Props = {
  product: SealedProductAdminDto;
  onEdit: (product: SealedProductAdminDto) => void;
  onDelete: (id: number) => void;
};

export function SealedProductRow({ product, onEdit, onDelete }: Props) {
  const [draft, setDraft] = useState<NumericDraft>({
    price: product.price ?? 0,
    currentVisibleStock: product.currentVisibleStock ?? 0,
    totalStock: product.totalStock ?? 0,
    maxVisibleStock: product.maxVisibleStock ?? 0,
  });
  const draftRef = useRef(draft);

  useEffect(() => {
    const next: NumericDraft = {
      price: product.price ?? 0,
      currentVisibleStock: product.currentVisibleStock ?? 0,
      totalStock: product.totalStock ?? 0,
      maxVisibleStock: product.maxVisibleStock ?? 0,
    };
    const timer = setTimeout(() => {
      draftRef.current = next;
      setDraft(next);
    }, 0);
    return () => clearTimeout(timer);
  }, [
    product.id,
    product.price,
    product.currentVisibleStock,
    product.totalStock,
    product.maxVisibleStock,
  ]);

  const updateField = (field: keyof NumericDraft, value: number) => {
    const next = { ...draftRef.current, [field]: value };
    draftRef.current = next;
    setDraft(next);
  };

  const save = () => {
    const d = draftRef.current;
    const fd = new FormData();
    fd.append("productNameEn", product.productNameEn ?? "");
    fd.append("productNameKo", product.productNameKo ?? "");
    fd.append("game", product.game ?? "");
    fd.append("setName", product.setName ?? "");
    fd.append("setCode", product.setCode ?? "");
    fd.append("price", String(d.price));
    fd.append("currentVisibleStock", String(d.currentVisibleStock));
    fd.append("totalStock", String(d.totalStock));
    fd.append("maxVisibleStock", String(d.maxVisibleStock));
    fd.append("language", product.language ?? "");
    fd.append("isActive", String(product.isActive ?? false));
    fd.append(
      "offlineProductId",
      offlineProductIdForSubmit(product.offlineProductId ?? undefined),
    );

    return api
      .putFormData<void>(`/api/admin/product/sealed-products/${product.id}`, fd)
      .then(() => {
        toast("수정되었습니다.", {
          description: "수정된 내용이 저장되었습니다.",
        });
      })
      .catch((err) => {
        alert(`저장 실패: ${getApiErrorMessage(err) ?? ""}`);
      });
  };

  return (
    <TableRow>
      <TableCell>{product.id}</TableCell>
      <TableCell>
        {product.imageUrl ? (
          <Image
            src={resolveAssetUrl(product.imageUrl)}
            alt={product.productNameKo ?? "상품 이미지"}
            width={60}
            height={60}
            className="object-contain"
          />
        ) : (
          <span className="text-muted-foreground text-xs">—</span>
        )}
      </TableCell>
      <TableCell className="truncate">
        {sealedGameShortLabel(product.game)}
      </TableCell>
      <TableCell className="w-12">
        {languageShortLabel(product.language)}
      </TableCell>
      <TableCell>{product.productNameEn}</TableCell>
      <TableCell>{product.productNameKo}</TableCell>
      <TableCell>
        <Input
          className="w-24"
          type="number"
          value={draft.price}
          min={0}
          step={1}
          onChange={(e) => updateField("price", Number(e.target.value))}
          onBlur={() => void save()}
        />
      </TableCell>
      <TableCell>
        <Input
          className="w-16"
          type="number"
          value={draft.currentVisibleStock}
          min={0}
          step={1}
          onChange={(e) =>
            updateField("currentVisibleStock", Number(e.target.value))
          }
          onBlur={() => void save()}
        />
      </TableCell>
      <TableCell>
        <Input
          className="w-16"
          type="number"
          value={draft.totalStock}
          min={0}
          step={1}
          onChange={(e) => updateField("totalStock", Number(e.target.value))}
          onBlur={() => void save()}
        />
      </TableCell>
      <TableCell>
        <Input
          className="w-16"
          type="number"
          value={draft.maxVisibleStock}
          min={0}
          step={1}
          onChange={(e) =>
            updateField("maxVisibleStock", Number(e.target.value))
          }
          onBlur={() => void save()}
        />
      </TableCell>
      <TableCell className="text-center">
        {product.isActive ? "Y" : "N"}
      </TableCell>
      <TableCell>
        <div className="flex flex-wrap items-center justify-center gap-2">
          <Button variant="outline" size="sm" onClick={() => onEdit(product)}>
            수정
          </Button>
          <Button
            variant="destructive"
            size="sm"
            onClick={() => onDelete(product.id)}
          >
            삭제
          </Button>
        </div>
      </TableCell>
    </TableRow>
  );
}
