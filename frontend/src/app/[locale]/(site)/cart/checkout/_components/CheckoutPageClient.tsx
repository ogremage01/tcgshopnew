"use client";

import { useLocale } from "next-intl";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useForm, type Resolver } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { formatCheckoutAmount } from "@/utils/formatShowingPrice";
import ProductImage from "@/components/product/ProductImage";
import { useSearchParams } from "next/navigation";
import { useRouter } from "@/i18n/navigation";
import { toast } from "sonner";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Separator } from "@/components/ui/separator";
import { Table, TableBody, TableCell, TableRow } from "@/components/ui/table";
import {
  parseCheckoutConflict,
  useCheckoutDraftQuery,
  useConfirmCheckoutDraftMutation,
  usePatchCheckoutDraftMutation,
  useValidateCheckoutDraftMutation,
} from "@/hooks/use-checkout-draft";
import type { DeliveryMethod } from "@/types/checkout";
import { useAuthStore } from "@/stores/auth-store";
import { Textarea } from "@/components/ui/textarea";
import { FieldError } from "@/components/ui/field";
// import { Checkbox } from "@/components/ui/checkbox";
// import { api } from "@/lib/api.client";
// import { OrderConfigDto } from "@/types/order";
import { useTranslations } from "next-intl";
import PostalCodeSearchDialog, {
  type PostalCodeSelection,
} from "@/components/postal-code/PostalCodeSearchDialog";
// TossPaymentSection: 토스 예시(JSX) 유지 위해 미사용. 실제 렌더는 TossPayment.jsx.
// import { TossPaymentSection } from "./TossPaymentSection";
import { CheckoutPage } from "./TossPayment";
import { ForeignCard } from "./InternationalCardPayment";

type CheckoutFormValues = {
  name: string;
  email: string;
  phone: string;
  address: string;
  postalCode: string;
  addressDetail: string;
  orderRequest: string;
};

/** 화면 표시용 배송비. 확정 금액은 서버가 다시 계산한다. */
function computeCheckoutDeliveryFee(
  delivery: "DELIVERY" | "STORE_PICKUP",
  productSubtotal: number,
  standardDeliveryFee: number | null | undefined,
  freeShippingThreshold: number | null | undefined,
): number {
  if (delivery !== "DELIVERY") return 0;
  const fee = Math.round(Number(standardDeliveryFee) || 0);
  if (
    freeShippingThreshold != null &&
    Number.isFinite(Number(freeShippingThreshold)) &&
    productSubtotal >= Number(freeShippingThreshold)
  ) {
    return 0;
  }
  return fee;
}

function createCheckoutFormSchema(
  delivery: "DELIVERY" | "STORE_PICKUP",
  eventTicketRequired: boolean,
) {
  return z
    .object({
      name: z.string().trim().min(1, "validation.nameRequired"),
      email: z
        .string()
        .trim()
        .min(1, "validation.emailRequired")
        .email("validation.emailInvalid"),
      // 임시: 형식 검증 없이 trim 후 비어 있지 않으면 통과. 토스 전달은 toTossMobilePhone(숫자만) 유지.
      phone: z.string().trim().min(1, "validation.phoneRequired"),
      address: z.string().trim(),
      postalCode: z.string().trim(),
      addressDetail: z.string().trim(),
      orderRequest: z.string().trim(),
    })
    .superRefine((data, ctx) => {
      if (delivery === "DELIVERY" && !data.address) {
        ctx.addIssue({
          code: "custom",
          message: "validation.addressRequired",
          path: ["address"],
        });
      }
      if (delivery === "DELIVERY" && !data.addressDetail) {
        ctx.addIssue({
          code: "custom",
          message: "validation.addressDetailRequired",
          path: ["addressDetail"],
        });
      }
      if (delivery === "DELIVERY") {
        if (!data.postalCode) {
          ctx.addIssue({
            code: "custom",
            message: "validation.postalCodeRequired",
            path: ["postalCode"],
          });
        }
        // 우편번호 5자리 숫자 제약. 직접 입력 허용 시 주석 유지, 복원 시 아래를 해제한다.
        // else if (!/^\d{5}$/.test(data.postalCode)) {
        //   ctx.addIssue({
        //     code: "custom",
        //     message: "validation.postalCodeInvalid",
        //     path: ["postalCode"],
        //   });
        // }
      }
      if (eventTicketRequired && !data.orderRequest) {
        ctx.addIssue({
          code: "custom",
          message: "event_ticket_required",
          path: ["orderRequest"],
        });
      }
    });
}

export default function CheckoutPageClient() {
  const locale = useLocale();
  const router = useRouter();
  const searchParams = useSearchParams();
  const draftPublicId = searchParams?.get("draftId") ?? undefined;

  const {
    data: draft,
    isLoading,
    error,
    refetch,
  } = useCheckoutDraftQuery(draftPublicId);
  const patchDraft = usePatchCheckoutDraftMutation(draftPublicId);
  const validateDraft = useValidateCheckoutDraftMutation(draftPublicId);
  const confirmDraft = useConfirmCheckoutDraftMutation(draftPublicId);
  const user = useAuthStore((s) => s.user);

  const [delivery, setDelivery] = useState<"DELIVERY" | "STORE_PICKUP">(
    "STORE_PICKUP",
  );
  const [usedPoints, setUsedPoints] = useState(0);
  const [required, setRequired] = useState(false);
  // const [paymentCurrency, setPaymentCurrency] = useState<"KRW" | "USD">("KRW");
  // const [paymentCurrencyRate, setPaymentCurrencyRate] = useState(0);
  const [postalCode, setPostalCode] = useState<string | undefined>(undefined);
  // locale에 따른 단일 주소검색 다이얼로그 open 상태. 복원 시 아래를 해제한다.
  // const [postalSearchOpen, setPostalSearchOpen] = useState(false);
  const hydratedDraftIdRef = useRef<string | null>(null);

  const t = useTranslations("checkoutPage");

  const checkoutSchema = useMemo(
    () => createCheckoutFormSchema(delivery, required),
    [delivery, required],
  );

  const resolver = useCallback<Resolver<CheckoutFormValues>>(
    (values, context, options) =>
      zodResolver(checkoutSchema)(values, context, options),
    [checkoutSchema],
  );

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<CheckoutFormValues>({
    resolver,
    defaultValues: {
      name: "",
      email: "",
      phone: "",
      address: "",
      postalCode: "",
      addressDetail: "",
      orderRequest: "",
    },
  });

  useEffect(() => {
    if (!draftPublicId) {
      toast.error(t("invalid_checkout_entry"));
      router.replace("/cart");
    }
  }, [draftPublicId, router, t]);

  useEffect(() => {
    if (!draft) return;
    if (draft.status === "REPLACED") {
      toast.message(t("new_order_page_opened"));
    }

    const isInitialHydrate = hydratedDraftIdRef.current !== draft.publicId;
    if (!isInitialHydrate) {
      setUsedPoints(Number(draft.usedPointAmount) || 0);
      setRequired(!!draft.hasEventTicket);
      return;
    }

    hydratedDraftIdRef.current = draft.publicId;
    setDelivery(
      draft.deliveryMethod === "DELIVERY" ? "DELIVERY" : "STORE_PICKUP",
    );
    reset({
      name: draft.recipientName ?? "",
      email: draft.recipientEmail ?? "",
      phone: draft.recipientPhone ?? "",
      address:
        draft.deliveryMethod === "DELIVERY"
          ? (draft.recipientAddress ?? "")
          : t("store_pickup_address"),
      addressDetail: draft.recipientAddressDetail ?? "",
      postalCode: draft.recipientPostalCode ?? "",
      orderRequest: draft.orderRequest ?? "",
    });
    setPostalCode(draft.recipientPostalCode ?? "");
    setUsedPoints(Number(draft.usedPointAmount) || 0);
    setRequired(!!draft.hasEventTicket);
  }, [draft, reset, t]);

  const apiError = error as
    | { response?: { status?: number; data?: { message?: string } } }
    | undefined;
  const httpStatus = apiError?.response?.status;

  useEffect(() => {
    if (httpStatus === 403) {
      toast.error(t("access_denied"));
      router.replace("/cart");
    }
    if (httpStatus === 409) {
      toast.error(t("checkout_expired"));
    }
    if (httpStatus === 410) {
      toast.error(t("checkout_timeout"));
      router.replace("/cart");
    }
  }, [httpStatus, router, t]);

  // USD 결제 (추후 지원)
  // useEffect(() => {
  //   api
  //     .get<OrderConfigDto[]>("/api/checkout/config")
  //     .then((res) => {
  //       const raw = res.find((c) => c.configKey === "USD_KRW_CURRENCY")?.configValue;
  //       const n = raw == null ? NaN : Number(String(raw).trim().replace(/,/g, ""));
  //       setPaymentCurrencyRate(Number.isFinite(n) && n > 0 ? n : 0);
  //     })
  //     .catch(() => {
  //       toast.error("결제 통화 설정을 불러올 수 없습니다.");
  //     });
  // }, []);

  const deliveryMethod: DeliveryMethod =
    delivery === "STORE_PICKUP" ? "STORE_PICKUP" : "DELIVERY";

  /**
   * 배송 수령 방식 변경 핸들러
   * @param value - 배송 수령 방식
   * @description 배송 수령 방식이 변경되면 주소를 초기화합니다.
   * @description 배송 수령 방식이 매장 수령이면 주소를 매장 주소로 설정합니다.
   * @description 배송 수령 방식이 배송 수령이면 주소를 초기화합니다.
   */

  const handleDeliveryMethodChange = (value: "DELIVERY" | "STORE_PICKUP") => {
    setDelivery(value);
    if (value === "DELIVERY") {
      setValue("address", "");
      setValue("postalCode", "");
      setValue("addressDetail", "");
      setPostalCode("");
    } else {
      setValue("address", t("store_pickup_address"));
      setValue("postalCode", "");
      setValue("addressDetail", "");
      setPostalCode("");
    }
  };

  const handlePostalCodeSelect = ({
    postalCode: selectedPostalCode,
    address,
  }: PostalCodeSelection) => {
    setPostalCode(selectedPostalCode);
    setValue("postalCode", selectedPostalCode, {
      shouldValidate: true,
    });
    setValue("address", address, { shouldValidate: true });
  };

  // const handlePaymentCurrencyChange = (checked: boolean | "indeterminate") => {
  //   setPaymentCurrency(checked === true ? "USD" : "KRW");
  // };

  /** 배송비와 포인트를 반영한 결제 예정액(KRW). 요약 표와 동일한 기준입니다. */
  const chargedDeliveryFee = computeCheckoutDeliveryFee(
    delivery,
    Number(draft?.totalProductAmount ?? draft?.subtotalAmount) || 0,
    draft?.standardDeliveryFee,
    draft?.freeShippingThreshold,
  );
  const goodsAndDeliveryKrw = !draft
    ? 0
    : Math.round(draft.subtotalAmount + chargedDeliveryFee);
  const maxUsablePoints = Math.max(
    0,
    Math.min(Number(user?.point) || 0, goodsAndDeliveryKrw),
  );
  const payableKrw = Math.max(0, goodsAndDeliveryKrw - usedPoints);

  useEffect(() => {
    setUsedPoints((prev) => Math.min(Math.max(0, prev), maxUsablePoints));
  }, [maxUsablePoints]);

  const formatAmount = (krw: number) =>
    formatCheckoutAmount(locale, Math.round(krw));

  const isFreeDelivery =
    delivery === "DELIVERY" &&
    chargedDeliveryFee === 0 &&
    Number(draft?.standardDeliveryFee) > 0;

  const deliveryFeeDisplay = (() => {
    if (delivery !== "DELIVERY") {
      return formatAmount(0);
    }
    if (isFreeDelivery) {
      return (
        <div className="flex flex-col items-end gap-0.5">
          <span className="text-muted-foreground line-through">
            {formatAmount(Number(draft?.standardDeliveryFee) || 0)}
          </span>
          <span className="font-bold text-red-500">{formatAmount(0)}</span>
        </div>
      );
    }
    return formatAmount(chargedDeliveryFee);
  })();

  const displayPaymentAmount = formatCheckoutAmount(locale, payableKrw);

  const tossOrderName = useMemo(() => {
    const items = draft?.items ?? [];
    if (items.length === 0) return "";
    const firstName = items[0].productNameKo || items[0].productNameEn;
    if (items.length === 1) return firstName;
    return `${firstName} 외 ${items.length - 1}건`;
  }, [draft?.items]);

  const handleCheckoutError = (e: unknown) => {
    const conflict = parseCheckoutConflict(e);
    if (conflict?.code === "PRICE_CHANGED") {
      toast.error(t("price_changed"));
      return true;
    }
    if (conflict?.code === "POINT_AMOUNT_CHANGED") {
      toast.error(t("point_amount_changed"));
      void refetch();
      return true;
    }
    if (conflict?.code === "OUT_OF_STOCK") {
      toast.error(t("out_of_stock"));
      return true;
    }
    if (conflict?.code === "PRODUCT_UNAVAILABLE") {
      toast.error(t("product_unavailable"));
      return true;
    }
    if (conflict?.code === "DRAFT_REPLACED") {
      toast.error(t("draft_replaced"));
      void refetch();
      return true;
    }
    if (conflict?.code === "DRAFT_EXPIRED") {
      toast.error(t("draft_expired"));
      router.replace("/cart");
      return true;
    }
    if (conflict?.code === "INSUFFICIENT_POINTS") {
      toast.error(t("insufficient_points"));
      return true;
    }
    return false;
  };

  const saveDraftFromForm = async (values: CheckoutFormValues) => {
    if (!draftPublicId) return;
    await patchDraft.mutateAsync({
      deliveryMethod,
      recipientName: values.name,
      recipientEmail: values.email,
      recipientPhone: values.phone,
      recipientAddress: delivery === "DELIVERY" ? values.address : undefined,
      orderRequest: values.orderRequest || undefined,
      usedPointAmount: user ? usedPoints : 0,
      paymentCurrency: "KRW",
      settleKrwAmount: payableKrw,
      recipientAddressDetail: values.addressDetail,
      recipientPostalCode: values.postalCode,
    });
  };

  const onSubmit = async (values: CheckoutFormValues) => {
    if (!draftPublicId) return;

    try {
      await saveDraftFromForm(values);
      const result = await confirmDraft.mutateAsync();
      const q = new URLSearchParams({
        orderId: String(result.orderId),
      });
      if (result.guestVerificationCode) {
        q.set("guestCode", result.guestVerificationCode);
      }
      router.push(`/cart/checkout/complete?${q.toString()}`);
    } catch (e) {
      if (handleCheckoutError(e)) {
        return;
      }
      toast.error(t("order_processing_failed"));
    }
  };

  const onBeforeTossPayment = (): Promise<void> =>
    new Promise((resolve, reject) => {
      void handleSubmit(
        async (values) => {
          try {
            await saveDraftFromForm(values);
            await validateDraft.mutateAsync();
            resolve();
          } catch (e) {
            if (handleCheckoutError(e)) {
              reject(e);
              return;
            }
            toast.error(t("order_processing_failed"));
            reject(e);
          }
        },
        () => reject(new Error("validation failed")),
      )();
    });

  const busy =
    patchDraft.isPending || validateDraft.isPending || confirmDraft.isPending;

  const customerName = watch("name");
  const customerEmail = watch("email");
  const customerPhone = watch("phone");

  const stockHint = !draft?.items?.length
    ? null
    : t("stock_hint", {
        types: draft.items.length,
        quantity: draft.items.reduce((a, i) => a + i.quantity, 0),
      });

  if (!draftPublicId) {
    return null;
  }

  if (isLoading) {
    return (
      <p className="text-muted-foreground py-12 text-center">
        {t("loading_order_info")}
      </p>
    );
  }

  if (!draft && !isLoading) {
    return (
      <p className="text-muted-foreground py-12 text-center">
        {t("order_info_not_found")}
      </p>
    );
  }

  if (!draft) {
    return null;
  }

  if (draft.status === "CONFIRMED" && draft.confirmedOrderId) {
    return (
      <p className="py-12 text-center">
        {t("order_confirmed")}
        <Button
          variant="link"
          className="px-1"
          onClick={() => router.push("/mypage/order")}
        >
          {t("view_order_history")}
        </Button>
        {t("to_move")}
      </p>
    );
  }

  return (
    <div className="w-full my-4 space-y-6">
      <div>
        <h1 className="text-2xl font-bold">{t("checkout")}</h1>
        <p className="text-sm text-muted-foreground">
          {t("checkout_description")}
        </p>
      </div>

      <div className="flex flex-col lg:flex-row gap-6">
        <div className="flex-1 space-y-6">
          <Card>
            <CardHeader>
              <CardTitle>{t("delivery_method")}</CardTitle>
            </CardHeader>
            <CardContent>
              <RadioGroup
                className="flex flex-row gap-6"
                value={delivery}
                onValueChange={handleDeliveryMethodChange}
              >
                <div className="flex items-center gap-2">
                  <RadioGroupItem value="DELIVERY" id="dm-delivery" />
                  <Label htmlFor="dm-delivery">{t("delivery")}</Label>
                </div>

                <div className="flex items-center gap-2">
                  <RadioGroupItem value="STORE_PICKUP" id="dm-store" />
                  <Label htmlFor="dm-store">{t("store_pickup")}</Label>
                </div>
              </RadioGroup>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>{t("recipient_info")}</CardTitle>
            </CardHeader>
            <CardContent className="grid gap-3 max-w-lg">
              <div className="flex flex-col md:flex-row gap-2">
                <div className="flex flex-col gap-2 w-full">
                  <Input
                    placeholder={t("name")}
                    {...register("name")}
                    aria-invalid={!!errors.name}
                    onChange={(e) => setValue("name", e.target.value)}
                    required
                  />
                  {errors.name?.message ? (
                    <FieldError>{t(errors.name.message)}</FieldError>
                  ) : (
                    <span className="text-sm text-red-500">
                      * {t("required_field")}
                    </span>
                  )}
                </div>
                {user && (
                  <Button
                    size="sm"
                    type="button"
                    onClick={() =>
                      setValue("name", user?.name ?? "", {
                        shouldValidate: true,
                      })
                    }
                  >
                    {t("fill_with_buyer_info_name")}
                  </Button>
                )}
              </div>
              <div className="flex flex-col md:flex-row gap-2">
                <div className="flex flex-col gap-2 w-full">
                  <Input
                    placeholder={t("email")}
                    {...register("email")}
                    type="email"
                    aria-invalid={!!errors.email}
                    onChange={(e) => setValue("email", e.target.value)}
                    required
                  />
                  {errors.email?.message ? (
                    <FieldError>{t(errors.email.message)}</FieldError>
                  ) : (
                    <span className="text-sm text-red-500">
                      * {t("required_field")}
                    </span>
                  )}
                </div>
                {user && (
                  <Button
                    size="sm"
                    type="button"
                    onClick={() =>
                      setValue("email", user?.email ?? "", {
                        shouldValidate: true,
                      })
                    }
                  >
                    {t("fill_with_buyer_info_email")}
                  </Button>
                )}
              </div>
              <div className="flex flex-col gap-2">
                <Input
                  placeholder={t("phone")}
                  {...register("phone")}
                  type="tel"
                  inputMode="tel"
                  autoComplete="tel"
                  aria-invalid={!!errors.phone}
                  onChange={(e) => setValue("phone", e.target.value)}
                  required
                />
                {errors.phone?.message ? (
                  <FieldError>{t(errors.phone.message)}</FieldError>
                ) : (
                  <span className="text-sm text-red-500">
                    * {t("required_field")}
                  </span>
                )}
              </div>

              <div
                className={
                  delivery === "STORE_PICKUP" ? "hidden" : "flex flex-col gap-2"
                }
              >
                <div className="flex flex-row flex-wrap gap-2">
                  {/* 우편번호 검색 전용(5자리 숫자). 직접 입력 허용 시 주석 유지, 복원 시 아래를 해제하고 현재 Input을 되돌린다.
                  <Input
                    placeholder={t("postal_code")}
                    {...register("postalCode")}
                    type="text"
                    inputMode="numeric"
                    maxLength={5}
                    autoComplete="postal-code"
                    aria-invalid={!!errors.postalCode}
                    value={postalCode ?? ""}
                    onChange={(e) => setPostalCode(e.target.value)}
                  />
                  */}
                  <Input
                    placeholder={t("postal_code")}
                    {...register("postalCode")}
                    type="text"
                    autoComplete="postal-code"
                    aria-invalid={!!errors.postalCode}
                    value={postalCode ?? ""}
                    onChange={(e) => {
                      const value = e.target.value;
                      setPostalCode(value);
                      setValue("postalCode", value, { shouldValidate: true });
                    }}
                  />
                  {errors.postalCode?.message && (
                    <FieldError>{t(errors.postalCode.message)}</FieldError>
                  )}
                  {/* locale에 따른 단일 주소검색 버튼. 복원 시 아래를 해제하고 영문/한글 버튼을 제거한다.
                  <PostalCodeSearchDialog
                    open={postalSearchOpen}
                    onOpenChange={setPostalSearchOpen}
                    onSelect={({ postalCode: selectedPostalCode, address }) => {
                      setPostalCode(selectedPostalCode);
                      setValue("postalCode", selectedPostalCode, {
                        shouldValidate: true,
                      });
                      setValue("address", address, { shouldValidate: true });
                    }}
                  />
                  */}
                  <PostalCodeSearchDialog
                    apiLocale="en"
                    onSelect={handlePostalCodeSelect}
                  />
                  <PostalCodeSearchDialog
                    apiLocale="ko"
                    onSelect={handlePostalCodeSelect}
                  />
                </div>
                {/* 주소는 register만 사용하던 입력. 직접 입력 허용 시 주석 유지, 복원 시 아래를 해제하고 현재 Input을 되돌린다.
                <Input
                  placeholder={t("address")}
                  {...register("address")}
                  aria-invalid={!!errors.address}
                />
                */}
                <Input
                  placeholder={t("address")}
                  {...register("address")}
                  aria-invalid={!!errors.address}
                  onChange={(e) =>
                    setValue("address", e.target.value, {
                      shouldValidate: true,
                    })
                  }
                />
                {errors.address?.message && (
                  <FieldError>{t(errors.address.message)}</FieldError>
                )}
                <Input
                  placeholder={t("address_detail")}
                  {...register("addressDetail")}
                  aria-invalid={!!errors.addressDetail}
                />
                {errors.addressDetail?.message && (
                  <FieldError>{t(errors.addressDetail.message)}</FieldError>
                )}
              </div>

              <div className="flex flex-col gap-2">
                <Textarea
                  placeholder={t("order_request")}
                  {...register("orderRequest")}
                  aria-invalid={!!errors.orderRequest}
                  required={required}
                />
                {errors.orderRequest?.message ? (
                  <FieldError>{t(errors.orderRequest.message)}</FieldError>
                ) : (
                  <Label className="text-sm text-red-500 font-bold">
                    {required ? `* ${t("event_ticket_required")}` : ""}
                  </Label>
                )}
              </div>
            </CardContent>
          </Card>

          {user && (
            <Card>
              <CardHeader>
                <CardTitle>{t("points")}</CardTitle>
              </CardHeader>
              <CardContent className="space-y-2 max-w-xs">
                <p className="text-sm text-muted-foreground">
                  {t("points_balance")}: {user.point?.toLocaleString() ?? 0} P
                </p>
                <Input
                  type="number"
                  min={0}
                  max={maxUsablePoints}
                  value={usedPoints}
                  onChange={(e) => {
                    const raw = Number(e.target.value);
                    if (!Number.isFinite(raw) || raw < 0) {
                      setUsedPoints(0);
                      return;
                    }
                    setUsedPoints(Math.min(Math.floor(raw), maxUsablePoints));
                  }}
                />
              </CardContent>
            </Card>
          )}
        </div>

        <Card className="lg:w-[380px] h-fit">
          <CardHeader>
            <CardTitle>{t("order_summary")}</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="space-y-3 max-h-[280px] overflow-y-auto pr-1 justify-center">
              {draft.items.map((item) => {
                const thumbSrc =
                  item.imageUrlEn?.trim() ||
                  item.imageUrlKo?.trim() ||
                  item.imageUrl?.trim() ||
                  null;
                return (
                  <div key={item.searchMapId} className="flex gap-3 text-sm">
                    <div className="relative h-14 w-14 shrink-0 overflow-hidden rounded border bg-muted">
                      <ProductImage
                        src={thumbSrc}
                        alt=""
                        fill
                        className="object-cover"
                        sizes="56px"
                      />
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="font-medium line-clamp-2">
                        {item.productNameKo || item.productNameEn}
                      </p>
                      <p className="text-muted-foreground">
                        {formatAmount(Number(item.snapshotUnitPrice))} x{" "}
                        {item.quantity}
                      </p>
                    </div>
                    <div className="shrink-0 font-medium">
                      {formatAmount(Number(item.snapshotTotalPrice))}
                    </div>
                  </div>
                );
              })}
            </div>
            <Separator />
            <Table>
              <TableBody>
                <TableRow>
                  <TableCell className="text-muted-foreground">
                    {t("product_types_quantity")}
                  </TableCell>
                  <TableCell className="text-end">{stockHint}</TableCell>
                </TableRow>
                <TableRow>
                  <TableCell className="text-muted-foreground">
                    {t("product_total")}
                  </TableCell>
                  <TableCell className="text-end">
                    {formatAmount(draft.subtotalAmount)}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell className="text-muted-foreground">
                    {t("shipping_fee")}
                  </TableCell>
                  <TableCell className="text-end">
                    {deliveryFeeDisplay}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell className="text-muted-foreground">
                    {t("points_used")}
                  </TableCell>
                  <TableCell className="text-end">
                    Pts {usedPoints.toLocaleString()}
                  </TableCell>
                </TableRow>
                <TableRow>
                  <TableCell className="font-semibold">
                    {t("pending_payment_amount")}
                  </TableCell>
                  <TableCell className="text-end font-semibold text-lg">
                    {displayPaymentAmount}
                  </TableCell>
                </TableRow>
                {user && (
                  <TableRow>
                    <TableCell className="font-semibold">
                      {t("willearnpoints")}
                    </TableCell>
                    <TableCell className="text-end">
                      Pts{" "}
                      {draft.items
                        .reduce(
                          (a, i) => a + Number(i.snapshotPointAmount ?? 0),
                          0,
                        )
                        .toLocaleString()}
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
            {/* 추후 지원 예정 */}
            {/* <div className="flex flex-row gap-2">
              <Checkbox
                checked={paymentCurrency === "USD"}
                onCheckedChange={handlePaymentCurrencyChange}
              />
              
              <Label>
              USD로 결제하겠습니다. (추후 지원 $1: ₩
              {paymentCurrencyRate > 0 ? paymentCurrencyRate.toLocaleString() : "—"})
            </Label>
            </div> */}
            <Separator />
            {/* <p className="text-xs text-muted-foreground">
              {t("payment_notice")}
            </p> */}
            <Button
              variant="outline"
              className="w-full"
              size="lg"
              disabled={busy}
              onClick={() => void handleSubmit(onSubmit)()}
            >
              {busy ? t("processing") : t("pay_in_store")}
            </Button>
            {/* <p className="text-xs text-muted-foreground">
              {t("international_payment_notice")}
            </p> */}
            {/* <p className="text-xs text-red-500">
              {t("international_payment_notice_2")}
            </p> */}

            {payableKrw > 0 && (
              <>
                {/* <ForeignCard
                  orderId={draft?.publicId ?? ""}
                  orderName={tossOrderName}
                  payAmount={payableKrw}
                  customerEmail={customerEmail}
                  customerName={customerName}
                  customerMobilePhone={customerPhone}
                  disabled={busy}
                  onBeforePayment={onBeforeTossPayment}
                /> */}
                {/* 토스 지원되면 활성화 */}
                <CheckoutPage
                  orderId={draft?.publicId ?? ""}
                  orderName={tossOrderName}
                  payAmount={payableKrw}
                  customerEmail={customerEmail}
                  customerName={customerName}
                  customerMobilePhone={customerPhone}
                  disabled={busy}
                  onBeforePayment={onBeforeTossPayment}
                />
              </>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
