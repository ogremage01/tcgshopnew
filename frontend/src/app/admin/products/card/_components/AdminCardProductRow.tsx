"use client";

import { useEffect, useRef, useState } from "react";
import { TableRow, TableCell } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Trash2 } from "lucide-react";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";
import type {
  CardProductManagementResponseDto,
  CardProductPatchRequest,
  StorageDto,
} from "@/types/product";
import Image from "next/image";
import { upperCase } from "lodash-es";
import { cn } from "@/lib/utils";

function numFromRaw(raw: string) {
  return raw === "" ? 0 : Number(raw);
}

type AdminCardNumericField =
  | "currentVisibleStock"
  | "maxVisibleStock"
  | "totalStock"
  | "pricingRate"
  | "price";

/** 1) 현재 표시 / 최대 표시 / 총 수량은 서버 PATCH와 화면 표시를 같이 맞추기 위해 한 객체로 둔다. */
type StockDraft = {
  currentVisibleStock: number;
  maxVisibleStock: number;
  totalStock: number;
};

/** 2) 입력 숫자와 이전 재고 상태로, 다음 화면에 그릴 재고 상태를 계산한다. */
function nextStockFromInput(
  prev: StockDraft,
  field: keyof StockDraft,
  num: number,
): StockDraft {
  const nextTotal =
    field === "totalStock"
      ? num
      : field === "currentVisibleStock" && num > prev.totalStock
        ? num
        : prev.totalStock;

  const nextCurrent =
    field === "currentVisibleStock"
      ? num
      : field === "totalStock"
        ? Math.min(prev.currentVisibleStock, num)
        : prev.currentVisibleStock;

  return {
    currentVisibleStock: nextCurrent,
    maxVisibleStock: field === "maxVisibleStock" ? num : prev.maxVisibleStock,
    totalStock: nextTotal,
  };
}

export type AdminCardProductRowProps = {
  card: CardProductManagementResponseDto;
  storageList: StorageDto[];
  schedulePatch: (id: number, body: CardProductPatchRequest) => void;
  flushPatch: (id: number, body: CardProductPatchRequest) => void;
  savePatch: (id: number, body: CardProductPatchRequest) => Promise<unknown>;
  onDelete: (id: number) => void;
};

/**
 * 목록 데이터는 부모 조회로만 갱신.
 * 재고 3필드는 로컬 state로 제어해 낙관적 PATCH 직후에도 표시가 DB와 어긋나 보이지 않게 한다.
 */
export default function AdminCardProductRow({
  card,
  storageList,
  schedulePatch,
  flushPatch,
  savePatch,
  onDelete,
}: AdminCardProductRowProps) {
  const [priceLinked, setPriceLinked] = useState(
    () => card.isPriceLinked ?? false,
  );
  const [isVisible, setIsVisible] = useState(() => card.isVisible ?? false);
  const [memoDraft, setMemoDraft] = useState(() => card.memo ?? "");
  const [isAutoUpdatedStock, setIsAutoUpdatedStock] = useState(
    () => card.isAutoUpdatedStock ?? false,
  );
  /** 3) 행 마운트 시점의 서버 값으로 재고 표시 초기값을 만든다. */
  const [stock, setStock] = useState<StockDraft>(() => ({
    currentVisibleStock: card.currentVisibleStock ?? 0,
    maxVisibleStock: card.maxVisibleStock ?? 0,
    totalStock: card.totalStock ?? 0,
  }));

  /**
   * 같은 렌더 틱에 입력이 연달아 올 때도 이전 값에서 이어 계산하도록, 최신 재고를 ref에도 맞춘다.
   * (이벤트 핸들러에서는 ref를 먼저 갱신한다.)
   */
  const stockRef = useRef(stock);

  /**
   * 4) 부모가 같은 카드에 대해 서버에서 새 데이터를 받아 props가 바뀌면, 표시를 서버와 다시 맞춘다.
   * (다른 행으로 바뀌면 card.id가 바뀌므로 함께 반영된다.)
   */
  useEffect(() => {
    const next: StockDraft = {
      currentVisibleStock: card.currentVisibleStock ?? 0,
      maxVisibleStock: card.maxVisibleStock ?? 0,
      totalStock: card.totalStock ?? 0,
    };
    const timer = setTimeout(() => {
      stockRef.current = next;
      setStock(next);
    }, 0);
    return () => clearTimeout(timer);
  }, [
    card.id,
    card.currentVisibleStock,
    card.maxVisibleStock,
    card.totalStock,
  ]);

  const withMemo = (
    body: Omit<CardProductPatchRequest, "memo">,
  ): CardProductPatchRequest => ({
    ...body,
    memo: memoDraft,
  });

  /**
   * 5) 재고 필드 onChange: 화면 표시(state/ref)만 갱신하고, PATCH는 하지 않는다.
   * 저장은 Enter(onKeyDown) 또는 blur(onBlur) 시에만 수행한다.
   */
  const patchStockChange = (field: keyof StockDraft, raw: string) => {
    const num = numFromRaw(raw);
    const next = { ...stockRef.current, [field]: num };
    stockRef.current = next;
    setStock(next);
  };

  /** 6) onBlur 시에는 디바운스를 취소하고 즉시 저장한다. */
  const patchStockBlur = (field: keyof StockDraft, raw: string) => {
    const num = numFromRaw(raw);
    const prev = stockRef.current;
    const prevTotal = prev.totalStock;
    const next = nextStockFromInput(prev, field, num);
    stockRef.current = next;
    setStock(next);
    if (field === "currentVisibleStock" && num > prevTotal) {
      flushPatch(
        card.id,
        withMemo({ currentVisibleStock: num, totalStock: num }),
      );
    } else if (field === "totalStock" && num < prev.currentVisibleStock) {
      flushPatch(
        card.id,
        withMemo({
          totalStock: num,
          currentVisibleStock: next.currentVisibleStock,
        }),
      );
    } else {
      flushPatch(card.id, withMemo({ [field]: num }));
    }
  };

  /** 7) 가격 등 재고가 아닌 숫자 필드는 기존처럼 비제어 + PATCH만 스케줄. */
  const patchNumChange = (field: AdminCardNumericField, raw: string) => {
    const num = numFromRaw(raw);
    schedulePatch(card.id, withMemo({ [field]: num }));
  };

  const patchNumBlur = (field: AdminCardNumericField, raw: string) => {
    const num = numFromRaw(raw);
    flushPatch(card.id, withMemo({ [field]: num }));
  };

  return (
    <TableRow>
      {/* <TableCell>{card.id}</TableCell> */}
      <TableCell>
        {card.imageUrl ? (
          <Image
            src={card.imageUrl}
            alt={card.name ?? "card image"}
            width={80}
            height={80}
            className="object-cover"
          />
        ) : (
          <div className="h-12 w-12 flex items-center justify-center bg-muted">
            <span className="text-lg">?</span>
          </div>
        )}
      </TableCell>
      <TableCell>
        <div className="flex flex-col gap-1">
          <span className="font-medium">
            ({card.language}) {card.name}
          </span>
          <span
            className={cn(
              "text-sm ",
              upperCase(card.unionPrice?.checkCodeRefined).includes("FOIL")
                ? "font-bold"
                : "text-muted-foreground",
            )}
          >
            {card.unionPrice?.checkCodeRefined ?? "없음"}
          </span>
          <span className="text-sm text-muted-foreground">
            {card.unionPrice?.price ?? "없음"}
          </span>
        </div>
      </TableCell>
      <TableCell>{card.condition}</TableCell>
      <TableCell>
        <Input
          className="w-16"
          type="number"
          value={stock.currentVisibleStock}
          onChange={(e) =>
            patchStockChange("currentVisibleStock", e.target.value)
          }
          onKeyDown={(e) => {
            if (e.key === "Enter")
              patchStockBlur("currentVisibleStock", e.currentTarget.value);
          }}
          onBlur={(e) => patchStockBlur("currentVisibleStock", e.target.value)}
          min={0}
          step={1}
        />
      </TableCell>
      <TableCell>
        <Input
          className="w-20"
          type="number"
          value={stock.totalStock}
          onChange={(e) => patchStockChange("totalStock", e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter")
              patchStockBlur("totalStock", e.currentTarget.value);
          }}
          onBlur={(e) => patchStockBlur("totalStock", e.target.value)}
          min={0}
          step={1}
        />
      </TableCell>
      <TableCell>
        <Input
          className="w-16"
          type="number"
          value={stock.maxVisibleStock}
          onChange={(e) => patchStockChange("maxVisibleStock", e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter")
              patchStockBlur("maxVisibleStock", e.currentTarget.value);
          }}
          onBlur={(e) => patchStockBlur("maxVisibleStock", e.target.value)}
          min={0}
          step={1}
        />
      </TableCell>
      <TableCell>
        <Checkbox
          checked={isVisible}
          onCheckedChange={(checked) => {
            const v = checked === true;
            setIsVisible(v);
            void savePatch(card.id, withMemo({ isVisible: v }));
          }}
        />
      </TableCell>
      <TableCell>
        <div className="flex flex-row items-center justify-center gap-2">
          <Checkbox
            checked={priceLinked}
            onCheckedChange={(checked) => {
              const v = checked === true;
              setPriceLinked(v);
              void savePatch(card.id, withMemo({ isPriceLinked: v }));
            }}
          />
          <Input
            className={`${priceLinked ? "" : "hidden"} w-16`}
            type="number"
            defaultValue={card.pricingRate ?? ""}
            onChange={(e) => patchNumChange("pricingRate", e.target.value)}
            onBlur={(e) => patchNumBlur("pricingRate", e.target.value)}
            min={0.01}
            max={2.0}
            step={0.01}
          />
          <Input
            className={`${priceLinked ? "hidden" : ""} w-28`}
            type="number"
            defaultValue={card.price ?? ""}
            onChange={(e) => patchNumChange("price", e.target.value)}
            onBlur={(e) => patchNumBlur("price", e.target.value)}
            min={0}
            step={1}
          />
        </div>
      </TableCell>
      <TableCell>
        <Checkbox
          className="m-auto"
          checked={isAutoUpdatedStock}
          onCheckedChange={(checked) => {
            const v = checked === true;
            setIsAutoUpdatedStock(v);
            void savePatch(card.id, withMemo({ isAutoUpdatedStock: v }));
          }}
        />
      </TableCell>
      <TableCell>
        <Select
          defaultValue={card.storageId?.toString()}
          onValueChange={(val) => {
            void savePatch(card.id, withMemo({ storageId: Number(val) }));
          }}
        >
          <SelectTrigger>
            <SelectValue placeholder="저장소 선택" />
          </SelectTrigger>
          <SelectContent>
            {storageList.map((storage) => (
              <SelectItem key={storage.id} value={storage.id.toString()}>
                {storage.storageName}
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
      </TableCell>
      <TableCell>
        <Textarea
          className="h-16 resize-none"
          value={memoDraft}
          onChange={(e) => setMemoDraft(e.target.value)}
          onBlur={() => flushPatch(card.id, { memo: memoDraft })}
        />
      </TableCell>
      <TableCell>
        <Button
          type="button"
          variant="destructive"
          className="m-auto"
          onClick={() => onDelete(card.id)}
        >
          <Trash2 />
        </Button>
      </TableCell>
    </TableRow>
  );
}
