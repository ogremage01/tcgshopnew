"use client";

import * as DialogPrimitive from "@radix-ui/react-dialog";
import {
  Dialog,
  DialogOverlay,
  DialogPortal,
  DialogTitle,
} from "@/components/ui/dialog";
import { Spinner } from "@/components/ui/spinner";

interface AdminOrderCancelLoadingDialogProps {
  open: boolean;
}

/** 주문 취소 API 대기용. 닫기·바깥 클릭·Esc 불가. */
export function AdminOrderCancelLoadingDialog({
  open,
}: AdminOrderCancelLoadingDialogProps) {
  return (
    <Dialog open={open}>
      <DialogPortal>
        <DialogOverlay className="z-[100]" />
        <DialogPrimitive.Content
          className="fixed left-1/2 top-1/2 z-[100] flex w-[min(100%,20rem)] -translate-x-1/2 -translate-y-1/2 flex-col items-center gap-4 rounded-lg border bg-background p-6 shadow-lg outline-none"
          onPointerDownOutside={(event) => event.preventDefault()}
          onInteractOutside={(event) => event.preventDefault()}
          onEscapeKeyDown={(event) => event.preventDefault()}
        >
          <DialogTitle className="sr-only">주문 취소 처리 중</DialogTitle>
          <Spinner className="size-8 text-muted-foreground" />
          <p className="text-center text-sm text-muted-foreground">
            주문 취소 처리 중입니다.
            <br />
            잠시만 기다려 주세요.
          </p>
        </DialogPrimitive.Content>
      </DialogPortal>
    </Dialog>
  );
}
