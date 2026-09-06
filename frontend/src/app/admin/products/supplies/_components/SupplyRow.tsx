"use client";

import Image from "next/image";
import { TableRow, TableCell } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { resolveAssetUrl } from "@/lib/public-asset-url";
import type { SupplyDto } from "@/types/product";

type Props = {
  supply: SupplyDto;
  onEdit: (supply: SupplyDto) => void;
  onDelete: (id: number) => void;
};

export function SupplyRow({ supply, onEdit, onDelete }: Props) {
  return (
    <TableRow>
      <TableCell>{supply.id}</TableCell>
      <TableCell>
        {supply.imgUrl ? (
          <Image
            src={resolveAssetUrl(supply.imgUrl)}
            alt={supply.nameKo ?? "상품 이미지"}
            width={100}
            height={100}
          />
        ) : (
          <span className="text-muted-foreground text-xs">—</span>
        )}
      </TableCell>
      <TableCell className="truncate">{supply.nameKo}</TableCell>
      <TableCell className="truncate">{supply.nameEn}</TableCell>
      <TableCell className="truncate">{supply.maker}</TableCell>
      <TableCell className="truncate">{supply.supplyType}</TableCell>
      <TableCell className="text-right">
        {supply.price?.toLocaleString()}
      </TableCell>
      <TableCell className="text-right">{supply.stock}</TableCell>
      <TableCell className="text-center">
        {supply.isVisible ? "Y" : "N"}
      </TableCell>
      <TableCell>
        <div className="flex flex-wrap items-center justify-center gap-2">
          <Button variant="outline" size="sm" onClick={() => onEdit(supply)}>
            수정
          </Button>
          <Button
            variant="destructive"
            size="sm"
            onClick={() => onDelete(supply.id)}
          >
            삭제
          </Button>
        </div>
      </TableCell>
    </TableRow>
  );
}
