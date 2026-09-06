"use client";

import { useCallback, useEffect, useState } from "react";
import { useLocale } from "next-intl";
import { useRouter, useSearchParams } from "next/navigation";

import {
  confirmTossPayment,
  isRetriableTossConfirmError,
} from "@/lib/toss-payment-confirm";
import { useAuthStore } from "@/stores/auth-store";

export type TossConfirmUiStatus = "loading" | "unknown" | "error";

type Options = {
  /** locale 없는 실패 경로. 예: `/cart/checkout/toss/fail` */
  failPath: string;
  waitForSession?: boolean;
};

export function useTossConfirmRedirect({
  failPath,
  waitForSession = false,
}: Options) {
  const router = useRouter();
  const locale = useLocale();
  const searchParams = useSearchParams();
  const sessionChecked = useAuthStore((s) => s.sessionChecked);

  const [status, setStatus] = useState<TossConfirmUiStatus>("loading");
  const [errorMessage, setErrorMessage] = useState("");
  const [retrying, setRetrying] = useState(false);

  const paymentKey = searchParams?.get("paymentKey") ?? "";
  const orderId = searchParams?.get("orderId") ?? "";
  const amountRaw = searchParams?.get("amount") ?? "";
  const amount = Number(amountRaw);

  const confirmAndRedirect = useCallback(async () => {
    const result = await confirmTossPayment({
      paymentKey,
      orderId,
      amount,
    });
    const q = new URLSearchParams({
      orderId: String(result.orderId),
    });
    if (result.guestVerificationCode) {
      q.set("guestCode", result.guestVerificationCode);
    }
    router.replace(`/${locale}/cart/checkout/complete?${q.toString()}`);
  }, [amount, locale, orderId, paymentKey, router]);

  const handleConfirmError = useCallback(
    (error: unknown) => {
      const message =
        error instanceof Error && error.message
          ? error.message
          : "결제 승인 처리에 실패했습니다.";
      if (isRetriableTossConfirmError(error)) {
        setStatus("unknown");
        setErrorMessage(message);
        return;
      }
      setStatus("error");
      setErrorMessage(message);
      router.replace(
        `/${locale}${failPath}?message=${encodeURIComponent(message)}`,
      );
    },
    [failPath, locale, router],
  );

  const sessionReady = waitForSession ? sessionChecked : true;

  useEffect(() => {
    if (!sessionReady) {
      return;
    }
    if (!searchParams) {
      return;
    }

    if (
      !paymentKey ||
      !orderId ||
      !amountRaw ||
      Number.isNaN(amount) ||
      amount <= 0
    ) {
      setStatus("error");
      setErrorMessage("결제 승인 정보가 올바르지 않습니다.");
      return;
    }

    let cancelled = false;
    async function confirm() {
      try {
        await confirmAndRedirect();
      } catch (error) {
        if (!cancelled) {
          handleConfirmError(error);
        }
      }
    }
    void confirm();
    return () => {
      cancelled = true;
    };
  }, [
    amount,
    amountRaw,
    confirmAndRedirect,
    handleConfirmError,
    orderId,
    paymentKey,
    searchParams,
    sessionReady,
  ]);

  const retryConfirm = useCallback(async () => {
    setRetrying(true);
    try {
      await confirmAndRedirect();
    } catch (error) {
      handleConfirmError(error);
    } finally {
      setRetrying(false);
    }
  }, [confirmAndRedirect, handleConfirmError]);

  return {
    status,
    errorMessage,
    retrying,
    orderId,
    amount,
    retryConfirm,
  };
}
