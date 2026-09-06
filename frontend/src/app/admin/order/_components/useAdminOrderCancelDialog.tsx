"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import {
  AdminOrderCancelDialog,
  type AdminOrderCancelOptions,
} from "@/app/admin/order/_components/AdminOrderCancelDialog";
import { AdminOrderCancelLoadingDialog } from "@/app/admin/order/_components/AdminOrderCancelLoadingDialog";

export type RequestAdminOrderCancelConfirm = (options?: {
  isGuest?: boolean;
  requiresPgRefund?: boolean;
}) => Promise<AdminOrderCancelOptions | null>;

export type RunWithCancelLoading = <T>(fn: () => Promise<T>) => Promise<T>;

/**
 * 관리자 주문 취소 Dialog를 Promise로 띄운다.
 * 확인 시 options, 닫기/취소 시 null.
 * API 대기 중에는 로딩 Dialog로 입력·이동을 막는다.
 */
export function useAdminOrderCancelDialog() {
  const [open, setOpen] = useState(false);
  const [loading, setLoading] = useState(false);
  const [isGuest, setIsGuest] = useState(false);
  const [requiresPgRefund, setRequiresPgRefund] = useState(false);
  const resolveRef = useRef<((value: AdminOrderCancelOptions | null) => void) | null>(
    null,
  );

  useEffect(() => {
    if (!loading) {
      return;
    }
    const onBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [loading]);

  const closeWith = useCallback((value: AdminOrderCancelOptions | null) => {
    setOpen(false);
    const resolve = resolveRef.current;
    resolveRef.current = null;
    resolve?.(value);
  }, []);

  const requestCancelConfirm: RequestAdminOrderCancelConfirm = useCallback(
    (options = {}) => {
      if (loading) {
        return Promise.resolve(null);
      }
      if (resolveRef.current) {
        resolveRef.current(null);
        resolveRef.current = null;
      }
      setIsGuest(!!options.isGuest);
      setRequiresPgRefund(!!options.requiresPgRefund);
      setOpen(true);
      return new Promise<AdminOrderCancelOptions | null>((resolve) => {
        resolveRef.current = resolve;
      });
    },
    [loading],
  );

  const runWithCancelLoading: RunWithCancelLoading = useCallback(async (fn) => {
    setLoading(true);
    try {
      return await fn();
    } finally {
      setLoading(false);
    }
  }, []);

  const dialog = (
    <>
      <AdminOrderCancelDialog
        open={open}
        isGuest={isGuest}
        requiresPgRefund={requiresPgRefund}
        onConfirm={(options) => closeWith(options)}
        onCancel={() => closeWith(null)}
      />
      <AdminOrderCancelLoadingDialog open={loading} />
    </>
  );

  return { requestCancelConfirm, runWithCancelLoading, dialog };
}
