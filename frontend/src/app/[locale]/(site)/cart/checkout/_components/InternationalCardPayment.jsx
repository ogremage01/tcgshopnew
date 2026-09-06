"use client";
import { loadTossPayments, ANONYMOUS } from "@tosspayments/tosspayments-sdk";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/stores/auth-store";
import { toTossMobilePhone } from "@/utils/phone";
// ------  SDK 초기화 ------
// @docs https://docs.tosspayments.com/sdk/v2/js#토스페이먼츠-초기화
const clientKey = process.env.NEXT_PUBLIC_INTERNATIONAL_CARD_PAYMENT_CLIENT_KEY;
export function ForeignCard({ orderId, orderName, payAmount, customerEmail, customerName, customerMobilePhone, disabled, onBeforePayment }) {
  // auth user.id === User.publicId (ULID)
  const customerKey = useAuthStore((s) => s.user?.id) ?? ANONYMOUS;
  const [payment, setPayment] = useState(null);
  const [amount] = useState({
    currency: "KRW",
    value: payAmount,
  });
  const [selectedPaymentMethod, setSelectedPaymentMethod] = useState(null);
  function selectPaymentMethod(method) {
    setSelectedPaymentMethod(method);
  }
  useEffect(() => {
    async function fetchPayment() {
      try {
        const tossPayments = await loadTossPayments(clientKey);
        // @docs https://docs.tosspayments.com/sdk/v2/js#tosspaymentspayment
        const payment = tossPayments.payment({
          customerKey,
        });
        setPayment(payment);
      } catch (error) {
        console.error("Error fetching payment:", error);
      }
    }
    fetchPayment();
  }, [clientKey, customerKey]);
  // ------ '결제하기' 버튼 누르면 결제창 띄우기 ------
  // @docs https://docs.tosspayments.com/sdk/v2/js#paymentrequestpayment
  async function requestPayment() {
    // 결제를 요청하기 전에 orderId, amount를 서버에 저장하세요.
    // 결제 과정에서 악의적으로 결제 금액이 바뀌는 것을 확인하는 용도입니다.
    await payment.requestPayment({
      method: "CARD", // 카드 및 간편결제
      amount: amount,
      orderId: orderId,
      orderName: orderName,
      successUrl: window.location.origin + "/cart/checkout/toss/success", // 결제 요청이 성공하면 리다이렉트되는 URL
      failUrl: window.location.origin + "/cart/checkout/toss/fail", // 결제 요청이 실패하면 리다이렉트되는 URL
      customerEmail: customerEmail,
      customerName: customerName,
      customerMobilePhone: toTossMobilePhone(customerMobilePhone),
      // 카드 결제에 필요한 정보
      card: {
        useEscrow: false,
        flowMode: "DEFAULT", // 통합결제창 여는 옵션
        useCardPoint: false,
        useAppCardOnly: false,
        useInternationalCardOnly: true, // 다국어 결제창
        language: "EN", // 결제창 초기 언어 (선택)
      },
    });
  }
  return (
    // 결제하기 버튼
    <div className="flex justify-center w-full">
    <Button className="button w-full" disabled={disabled} onClick={() => requestPayment()}>
      International Card Payment
    </Button>
    </div>
  );
}