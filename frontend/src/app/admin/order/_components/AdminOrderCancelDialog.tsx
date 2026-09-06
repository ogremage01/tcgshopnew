"use client";

import { useState } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";

export interface AdminOrderCancelOptions {
  restoreStock: boolean;
  restoreCartItems: boolean;
  cancelReason: string;
}

interface AdminOrderCancelDialogProps {
  open: boolean;
  isGuest?: boolean;
  requiresPgRefund?: boolean;
  onConfirm: (options: AdminOrderCancelOptions) => void;
  onCancel: () => void;
}

export function AdminOrderCancelDialog({
  open,
  isGuest = false,
  requiresPgRefund = false,
  onConfirm,
  onCancel,
}: AdminOrderCancelDialogProps) {
  const [cancelReason, setCancelReason] = useState("");
  const [restoreStock, setRestoreStock] = useState(false);
  const [restoreCartItems, setRestoreCartItems] = useState(false);

  const trimmedReason = cancelReason.trim();
  const canConfirm = trimmedReason.length > 0;

  const reset = () => {
    setCancelReason("");
    setRestoreStock(false);
    setRestoreCartItems(false);
  };

  const handleConfirm = () => {
    if (!canConfirm) {
      return;
    }
    onConfirm({
      restoreStock,
      restoreCartItems,
      cancelReason: trimmedReason,
    });
    reset();
  };

  const handleCancel = () => {
    reset();
    onCancel();
  };

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleCancel()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>취소하시겠습니까?</DialogTitle>
          <DialogDescription>
            주문을 취소하려면 취소 사유를 입력한 뒤 확인해 주세요.
            {requiresPgRefund && (
              <span className="text-destructive font-medium block mt-1">
                이 주문은 토스 PG 결제입니다. 취소 시 결제 금액이 환불됩니다.
              </span>
            )}
          </DialogDescription>
        </DialogHeader>

        <div className="flex flex-col gap-4 py-2">
          <div className="flex flex-col gap-2">
            <Label htmlFor="cancelReason">
              취소 사유{requiresPgRefund ? " (환불 사유)" : ""}
            </Label>
            <Textarea
              id="cancelReason"
              placeholder="취소 사유를 입력하세요"
              value={cancelReason}
              onChange={(e) => setCancelReason(e.target.value)}
              rows={3}
              maxLength={200}
            />
          </div>

          <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
              <Checkbox
                id="restoreStock"
                checked={restoreStock}
                onCheckedChange={(v) => setRestoreStock(!!v)}
              />
              <Label htmlFor="restoreStock" className="cursor-pointer">
                주문 상품 재고 복구
              </Label>
            </div>
            {!isGuest && (
              <div className="flex items-center gap-2">
                <Checkbox
                  id="restoreCartItems"
                  checked={restoreCartItems}
                  onCheckedChange={(v) => setRestoreCartItems(!!v)}
                />
                <Label htmlFor="restoreCartItems" className="cursor-pointer">
                  주문 상품 장바구니 복구
                </Label>
              </div>
            )}
          </div>
        </div>

        <DialogFooter>
          <Button variant="outline" onClick={handleCancel}>
            취소
          </Button>
          <Button
            variant="destructive"
            onClick={handleConfirm}
            disabled={!canConfirm}
          >
            확인
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
