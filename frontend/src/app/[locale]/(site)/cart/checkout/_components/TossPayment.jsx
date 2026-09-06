"use client";
import { loadTossPayments, ANONYMOUS } from "@tosspayments/tosspayments-sdk";
import { useTranslations } from "next-intl";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useAuthStore } from "@/stores/auth-store";
import { toTossMobilePhone } from "@/utils/phone";
const clientKey = process.env.NEXT_PUBLIC_TOSS_PAYMENTS_CLIENT_KEY;
const hasWidgetClientKey = /^(test|live)_gck_/.test(clientKey ?? "");

export function CheckoutPage({
  orderId,
  orderName,
  payAmount,
  customerEmail,
  customerName,
  customerMobilePhone,
  disabled = false,
  onBeforePayment,
}) {
const t = useTranslations("checkoutPage");
// auth user.id === User.publicId (ULID)
const customerKey = useAuthStore((s) => s.user?.id) ?? ANONYMOUS;
const [amount, setAmount] = useState({
  currency: "KRW",
  value: payAmount,
});
const [ready, setReady] = useState(false);
const [widgets, setWidgets] = useState(null);
const [paying, setPaying] = useState(false);

useEffect(() => {
  setAmount({
    currency: "KRW",
    value: payAmount,
  });
}, [payAmount]);

useEffect(() => {
  async function fetchPaymentWidgets() {
      if (!hasWidgetClientKey) {
        console.error("Toss Payments widget client key is missing or invalid.");
        return;
      }

    // ------  결제위젯 초기화 ------
    const tossPayments = await loadTossPayments(clientKey);
    const widgets = tossPayments.widgets({
      customerKey,
    });

    setWidgets(widgets);
  }

  fetchPaymentWidgets();
}, [clientKey, customerKey]);

useEffect(() => {
  async function renderPaymentWidgets() {
    if (widgets == null) {
      return;
    }
    // ------ 주문의 결제 금액 설정 ------
    await widgets.setAmount(amount);

    await Promise.all([
      // ------  결제 UI 렌더링 ------
      widgets.renderPaymentMethods({
        selector: "#payment-method",
        variantKey: "DEFAULT",
      }),
      // ------  이용약관 UI 렌더링 ------
      widgets.renderAgreement({
        selector: "#agreement",
        variantKey: "AGREEMENT",
      }),
    ]);

    setReady(true);
  }

  renderPaymentWidgets();
}, [widgets]);

useEffect(() => {
  if (widgets == null) {
    return;
  }

  widgets.setAmount(amount);
}, [widgets, amount]);

return (
  <div className="wrapper flex justify-center w-full">
    <div className="box_section">
      {!hasWidgetClientKey && (
        <p className="mb-4 text-sm text-destructive" role="alert">
          결제 서비스를 준비하지 못했습니다. 잠시 후 다시 시도해주세요.
        </p>
      )}
      {/* 결제 UI */}
      <div id="payment-method" />
      {/* 이용약관 UI */}
      <div id="agreement" />
      {/* 쿠폰 체크박스 */}
      {/* <div>
        <div>
          <Label htmlFor="coupon-box">
            <input
              id="coupon-box"
              type="checkbox"
              aria-checked="true"
              disabled={!ready}
              onChange={(event) => {
                // ------  주문서의 결제 금액이 변경되었을 경우 결제 금액 업데이트 ------
                setAmount(event.target.checked ? amount - 5_000 : amount + 5_000);
              }}
            />
            <span>5,000원 쿠폰 적용</span>
          </Label>
        </div>
      </div> */}

      {/* 결제하기 버튼 */}
      <Button
        className="button w-full"
        disabled={!ready || disabled || paying}
        onClick={async () => {
          setPaying(true);
          try {
            if (onBeforePayment) {
              await onBeforePayment();
            }
            const nextAmount = {
              currency: "KRW",
              value: payAmount,
            };
            setAmount(nextAmount);
            await widgets.setAmount(nextAmount);
            // ------ '결제하기' 버튼 누르면 결제창 띄우기 ------
            // 결제를 요청하기 전에 orderId, amount를 서버에 저장하세요.
            // 결제 과정에서 악의적으로 결제 금액이 바뀌는 것을 확인하는 용도입니다.
            await widgets.requestPayment({
              orderId,
              orderName,
              successUrl: window.location.origin + "/cart/checkout/toss/success",
              failUrl: window.location.origin + "/cart/checkout/toss/fail",
              customerEmail: customerEmail || undefined,
              customerName: customerName || undefined,
              customerMobilePhone: toTossMobilePhone(customerMobilePhone),

            });
          } catch (error) {
            // 에러 처리하기
            console.error(error);
          } finally {
            setPaying(false);
          }
        }}
      >
        {paying ? t("opening_payment_window") : t("pay")}
      </Button>
    </div>
  </div>
);
}