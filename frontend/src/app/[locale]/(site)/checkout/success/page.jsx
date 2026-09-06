"use client";

import { useLocale } from "next-intl";
import Link from "next/link";

import { useTossConfirmRedirect } from "@/hooks/use-toss-confirm-redirect";

export default function SuccessPage() {
  const locale = useLocale();
  const {
    status,
    errorMessage,
    retrying,
    orderId,
    amount,
    retryConfirm,
  } = useTossConfirmRedirect({
    failPath: "/checkout/fail",
  });

  if (status === "unknown") {
    return (
      <div className="result wrapper">
        <div className="box_section">
          <h2>결제 결과를 확인하는 중입니다</h2>
          <p>
            서버와의 연결이 잠시 끊겼습니다. 결제가 실패했다고 단정할 수 없으니,
            잠시 후 다시 확인하거나 주문 내역을 살펴봐 주세요.
          </p>
          <p>{`주문번호: ${orderId}`}</p>
          <button
            type="button"
            disabled={retrying}
            onClick={() => void retryConfirm()}
          >
            {retrying ? "확인 중…" : "결제 결과 다시 확인"}
          </button>
          <p>
            <Link href={`/${locale}/mypage/order`}>주문 내역</Link>
            {" / "}
            <Link href={`/${locale}/guest-order-check`}>비회원 주문 조회</Link>
          </p>
        </div>
      </div>
    );
  }

  if (status === "error") {
    return (
      <div className="result wrapper">
        <div className="box_section">
          <h2>결제 승인 실패</h2>
          <p>{errorMessage}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="result wrapper">
      <div className="box_section">
        <h2>결제 승인 처리 중…</h2>
        <p>잠시만 기다려 주세요.</p>
        <p>{`주문번호: ${orderId}`}</p>
        <p>{`결제 금액: ${amount.toLocaleString()}원`}</p>
      </div>
    </div>
  );
}
