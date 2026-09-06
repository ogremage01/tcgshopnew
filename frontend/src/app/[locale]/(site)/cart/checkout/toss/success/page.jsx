"use client";

import { useLocale } from "next-intl";
import Link from "next/link";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useTossConfirmRedirect } from "@/hooks/use-toss-confirm-redirect";
import { useAuthStore } from "@/stores/auth-store";

export default function SuccessPage() {
  const locale = useLocale();
  const user = useAuthStore((s) => s.user);
  const {
    status,
    errorMessage,
    retrying,
    orderId,
    amount,
    retryConfirm,
  } = useTossConfirmRedirect({
    failPath: "/cart/checkout/toss/fail",
    waitForSession: true,
  });

  if (status === "unknown") {
    return (
      <div className="w-full my-8 max-w-lg mx-auto space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>결제 결과를 확인하는 중입니다</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <p>
              서버와의 연결이 잠시 끊겼습니다. 결제가 실패했다고 단정할 수 없으니,
              잠시 후 다시 확인하거나 주문 내역을 살펴봐 주세요.
            </p>
            <p>{`주문번호: ${orderId}`}</p>
            <div className="flex flex-col gap-2">
              <Button disabled={retrying} onClick={() => void retryConfirm()}>
                {retrying ? "확인 중…" : "결제 결과 다시 확인"}
              </Button>
              {user ? (
                <Button variant="outline" asChild>
                  <Link href={`/${locale}/mypage/order`}>주문 내역 보기</Link>
                </Button>
              ) : (
                <Button variant="outline" asChild>
                  <Link href={`/${locale}/guest-order-check`}>비회원 주문 조회</Link>
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className="w-full my-8 max-w-lg mx-auto space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>결제 승인 실패</CardTitle>
          </CardHeader>
          <CardContent>
            <p>{errorMessage}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="w-full my-8 max-w-lg mx-auto space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>결제 승인 처리 중…</CardTitle>
        </CardHeader>
        <CardContent>
          <p>잠시만 기다려 주세요.</p>
          <p>{`주문번호: ${orderId}`}</p>
          <p>{`결제 금액: ${amount.toLocaleString()}원`}</p>
        </CardContent>
      </Card>
    </div>
  );
}
