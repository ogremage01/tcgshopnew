"use client";

import {
  Table,
  TableHeader,
  TableBody,
  TableRow,
  TableCell,
  TableHead,
} from "@/components/ui/table";
import AdminCardProductRow from "./AdminCardProductRow";
import type {
  CardProductManagementResponseDto,
  CardProductPatchRequest,
  StorageDto,
} from "@/types/product";

export const ADMIN_CARD_PRODUCT_TABLE_COL_COUNT = 12;

type AdminCardProductListTableProps = {
  cardList: CardProductManagementResponseDto[];
  storageList: StorageDto[];
  isLoading?: boolean;
  schedulePatch: (id: number, body: CardProductPatchRequest) => void;
  flushPatch: (id: number, body: CardProductPatchRequest) => void;
  savePatch: (id: number, body: CardProductPatchRequest) => Promise<unknown>;
  onDelete: (id: number) => void;
};

export default function AdminCardProductListTable({
  cardList,
  storageList,
  isLoading = false,
  schedulePatch,
  flushPatch,
  savePatch,
  onDelete,
}: AdminCardProductListTableProps) {
  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>이미지</TableHead>
          <TableHead>
            <div className="flex flex-col gap-1">
              <span>(언어)상품 이름</span>
              <span className="text-sm text-muted-foreground">코드</span>
              <span className="text-sm text-muted-foreground">가격</span>
            </div>
          </TableHead>
          <TableHead className="text-center">상태</TableHead>
          <TableHead className="text-center">현재 표시</TableHead>
          <TableHead className="text-center">총 수량</TableHead>
          <TableHead className="text-center">최대 표시</TableHead>
          <TableHead className="text-center">공개</TableHead>
          <TableHead className="text-center">가격 연동</TableHead>
          <TableHead className="w-16">수량 갱신</TableHead>
          <TableHead className="text-center">보관소</TableHead>
          <TableHead className="text-center">메모</TableHead>
          <TableHead className="text-center">삭제</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {isLoading ? (
          <TableRow>
            <TableCell
              colSpan={ADMIN_CARD_PRODUCT_TABLE_COL_COUNT}
              className="text-center"
            >
              불러오는 중...
            </TableCell>
          </TableRow>
        ) : cardList.length === 0 ? (
          <TableRow>
            <TableCell
              colSpan={ADMIN_CARD_PRODUCT_TABLE_COL_COUNT}
              className="text-center"
            >
              검색 결과가 없습니다.
            </TableCell>
          </TableRow>
        ) : (
          cardList.map((card) => (
            <AdminCardProductRow
              key={card.id}
              card={card}
              storageList={storageList}
              schedulePatch={schedulePatch}
              flushPatch={flushPatch}
              savePatch={savePatch}
              onDelete={onDelete}
            />
          ))
        )}
      </TableBody>
    </Table>
  );
}
