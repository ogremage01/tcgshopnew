"use client";

import { useRef } from "react";
import { Printer, ArrowLeft } from "lucide-react";
import { useReactToPrint } from "react-to-print";
import { useTranslations } from "next-intl";
import { isAxiosError } from "axios";
import { Link, useRouter } from "@/i18n/navigation";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { Button } from "@/components/ui/button";
import {
    Table,
    TableHeader,
    TableBody,
    TableRow,
    TableHead,
    TableCell,
} from "@/components/ui/table";
import ProductImage from "@/components/product/ProductImage";
import { getOrderStatusLabelKo } from "@/lib/order-status";
import { resolveDeliveryCompanyCode } from "@/lib/delivery-company";
import {
    UserOrderDetailDto,
    UserOrderCardProductDto,
    UserOrderManualProductDto,
    UserOrderSealedProductDto,
    UserOrderSupplyProductDto,
    OrderInfoDto,
} from "@/types/order";
import {
    useUserOrderDetail,
    UserOrderDetailVariant,
} from "@/hooks/use-user-order-detail";
import PrintTypeLabel from "../product/PrintTypeLabel";

/** DB에 `totalProductAmount`가 없는 구 주문: 소계 ≒ 최종 + 포인트 − 배송비 */
function resolveProductSubtotal(info: OrderInfoDto): number | null {
    if (info.totalProductAmount != null) {
        return info.totalProductAmount;
    }
    const total = info.totalPaymentAmount;
    const delivery = info.deliveryFee ?? 0;
    const used = info.usedPointAmount ?? 0;
    if (typeof total !== "number") {
        return null;
    }
    return total - delivery + used;
}

type UserOrderDetailProps = {
    variant: UserOrderDetailVariant;
    orderId: string;
    guestCode?: string;
};

function OrderDetailProducts({ orderDetail }: { orderDetail: UserOrderDetailDto }) {
    return (
        <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-3 xl:grid-cols-4 2xl:grid-cols-6 flex-wrap gap-2">
            {orderDetail.orderCardProductGroups?.map((group) => (
                <div key={group.game} className="col-span-full flex flex-col gap-2">
                    <h3 className="text-sm font-semibold border-b pb-1">{group.game}</h3>
                    <div className="flex flex-row flex-wrap gap-2">
                        {group.products.map((orderCardProduct: UserOrderCardProductDto) => (
                            <div
                                key={orderCardProduct.id}
                                className="flex flex-col border p-2 gap-1 w-fit max-w-full break-inside-avoid [page-break-inside:avoid]"
                            >
                                <div className="flex flex-col gap-2">
                                    <span className="text-xs">
                                        {orderCardProduct.setName ?? "empty"}
                                    </span>
                                    <span className="text-xs">
                                        {orderCardProduct.language ?? ""}-
                                        {orderCardProduct.setCode ?? ""}-
                                        {orderCardProduct.setNumber ?? ""}
                                    </span>
                                    {orderCardProduct.language === "en" ? (
                                        <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                                        {orderCardProduct.productNameEn ?? ""}
                                    </span>)
                                        : (
                                        <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                                            {orderCardProduct.productNameKo ?? ""}
                                        </span>
                                    )}
                                </div>
                                <div className="flex flex-row gap-2">
                                    <ProductImage
                                        src={orderCardProduct.imageUrl}
                                        alt={
                                            [orderCardProduct.setCode, orderCardProduct.setNumber]
                                                .filter(Boolean)
                                                .join(" ") ||
                                            `product-${orderCardProduct.productId}`
                                        }
                                        width={100}
                                        height={100}
                                    />
                                    <div className="flex flex-col gap-2">
                                        <span className="text-xs">
                                            {orderCardProduct.condition ?? ""}-
                                            
                                        </span>
                                        <PrintTypeLabel
                                            printType={orderCardProduct.printType}
                                            printing={orderCardProduct.printing}
                                            foilHighlight="badge"
                                            className="text-xs"
                                        />
                                        <span className="text-xs text-end">₩{orderCardProduct.price}</span>
                                        <span className="text-xs text-end">
                                            {orderCardProduct.quantity}
                                        </span>
                                        <Separator />
                                        <span className="text-xs text-end">
                                            ₩{orderCardProduct.totalPrice}
                                        </span>
                                        {orderCardProduct.rewardPoints != null &&
                                            orderCardProduct.rewardPoints > 0 && (
                                                <span className="text-xs text-blue-600 text-end">
                                                    +
                                                    {(
                                                        orderCardProduct.rewardPoints *
                                                        (orderCardProduct.quantity ?? 1)
                                                    ).toLocaleString()}
                                                    pts
                                                </span>
                                            )}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            ))}
            {orderDetail.orderManualProducts?.map((orderManualProduct: UserOrderManualProductDto) => (
                <div
                    key={orderManualProduct.id}
                    className="flex flex-col border p-2 gap-1 w-full bg-gray-100 break-inside-avoid [page-break-inside:avoid]"
                >
                    <div className="flex flex-col gap-2">
                        <span className="text-xs">{orderManualProduct.productId}</span>
                        <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                            {orderManualProduct.productNameEn ?? ""}
                        </span>
                        <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                            {orderManualProduct.productNameKo ?? ""}
                        </span>
                    </div>
                    <div className="flex flex-row gap-2">
                        <ProductImage
                            src={orderManualProduct.imageUrl}
                            alt={`product-${orderManualProduct.productId}`}
                            width={100}
                            height={100}
                        />
                        <div className="flex flex-col gap-2">
                            <span className="text-xs">{orderManualProduct.price}</span>
                            <span className="text-xs">{orderManualProduct.quantity}</span>
                            <span className="text-xs">{orderManualProduct.totalPrice}</span>
                        </div>
                    </div>
                </div>
            ))}
            {orderDetail.orderSealedProducts?.map((orderSealedProduct: UserOrderSealedProductDto) => (
                <div
                    key={orderSealedProduct.id}
                    className="flex flex-col border p-2 gap-1 w-full bg-gray-100 break-inside-avoid [page-break-inside:avoid]"
                >
                    <div className="flex flex-col gap-2">
                        <span className="text-xs">
                            {orderSealedProduct.language ?? "없음"}-{orderSealedProduct.game ?? "없음"}
                        </span>
                        <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                            {orderSealedProduct.productNameEn ?? "없음"}
                        </span>
                        <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                            {orderSealedProduct.productNameKo ?? "없음"}
                        </span>
                    </div>
                    <div className="flex flex-row gap-2">
                        <ProductImage
                            src={orderSealedProduct.imageUrl}
                            alt={`product-${orderSealedProduct.productId}`}
                            width={100}
                            height={100}
                        />
                        <div className="flex flex-col gap-2">
                            <span className="text-xs">{orderSealedProduct.price}</span>
                            <span className="text-xs">{orderSealedProduct.quantity}</span>
                            <span className="text-xs">{orderSealedProduct.totalPrice}</span>
                        </div>
                    </div>
                </div>
            ))}
            {orderDetail.orderSupplyProducts?.map((orderSupplyProduct: UserOrderSupplyProductDto) => (
                <div
                    key={orderSupplyProduct.id}
                    className="flex flex-col border p-2 gap-1 w-full bg-gray-100 break-inside-avoid [page-break-inside:avoid]"
                >
                    <div className="flex flex-col gap-2">
                        <span className="text-xs">
                            {orderSupplyProduct.supplyType ?? ""}
                            {orderSupplyProduct.maker ? `-${orderSupplyProduct.maker}` : ""}
                        </span>
                        <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                            {orderSupplyProduct.productNameEn ?? ""}
                        </span>
                        <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                            {orderSupplyProduct.productNameKo ?? ""}
                        </span>
                    </div>
                    <div className="flex flex-row gap-2">
                        <ProductImage
                            src={orderSupplyProduct.imageUrl}
                            alt={`product-${orderSupplyProduct.productId}`}
                            width={100}
                            height={100}
                        />
                        <div className="flex flex-col gap-2">
                            <span className="text-xs">{orderSupplyProduct.price}</span>
                            <span className="text-xs">{orderSupplyProduct.quantity}</span>
                            <span className="text-xs">{orderSupplyProduct.totalPrice}</span>
                        </div>
                    </div>
                </div>
            ))}
        </div>
    );
}

export default function UserOrderDetail({ variant, orderId, guestCode }: UserOrderDetailProps) {
    const t = useTranslations("order");
    const tc = useTranslations("common");
    const router = useRouter();
    const isGuest = variant === "guest";
    const orderIdNum = Number(orderId);
    const verificationCode = guestCode?.trim() ?? "";

    const componentRef = useRef<HTMLDivElement>(null);
    const componentRef2 = useRef<HTMLDivElement>(null);

    const handlePrint = useReactToPrint({
        contentRef: componentRef,
        onPrintError: (error) => {
            console.error(error);
        },
    });
    const handlePrint2 = useReactToPrint({
        contentRef: componentRef2,
        onPrintError: (error) => {
            console.error(error);
        },
    });

    const { data: orderDetail, isLoading, error } = useUserOrderDetail({
        variant,
        orderId,
        guestCode,
    });

    if (
        isGuest &&
        (!orderId || Number.isNaN(orderIdNum) || orderIdNum <= 0 || !verificationCode)
    ) {
        return (
            <div className="p-4 space-y-3 max-w-4xl mx-auto">
                <p className="text-muted-foreground">{t("guestMissingParams")}</p>
                <Button variant="outline" asChild>
                    <Link href="/guest-order-check">{t("guestOrderCheckLink")}</Link>
                </Button>
            </div>
        );
    }

    if (isLoading) {
        return (
            <div className={isGuest ? "p-4 max-w-4xl mx-auto" : "p-4"}>Loading...</div>
        );
    }

    if (error) {
        if (isAxiosError(error)) {
            const status = error.response?.status;
            if (!isGuest && status === 401) {
                router.replace("/login");
                return null;
            }
            if (!isGuest && status === 403) {
                return <div className="p-4 text-destructive">{t("detailForbidden")}</div>;
            }
            if (status === 404) {
                if (isGuest) {
                    return (
                        <div className="p-4 space-y-3 max-w-4xl mx-auto">
                            <p className="text-muted-foreground">{t("guestLookupMismatch")}</p>
                            <Button variant="outline" asChild>
                                <Link href="/guest-order-check">{t("backToGuestOrderCheck")}</Link>
                            </Button>
                        </div>
                    );
                }
                return <div className="p-4 text-muted-foreground">{t("detailNotFound")}</div>;
            }
        }
        return <div className="p-4 text-destructive">{tc("error")}</div>;
    }

    const info = orderDetail?.orderInfo;
    const productSubtotal = info ? resolveProductSubtotal(info) : null;
    const grossBeforePoints =
        productSubtotal != null ? productSubtotal + (info?.deliveryFee ?? 0) : null;
    const orderStatusLabel = info?.orderStatus
        ? t.has(`status.${info.orderStatus}` as never)
            ? t(`status.${info.orderStatus}` as never)
            : getOrderStatusLabelKo(info.orderStatus)
        : "—";
    const deliveryCompanyCode = resolveDeliveryCompanyCode(info?.deliveryCompany);
    const deliveryCompanyLabel = t.has(`deliveryCompany.${deliveryCompanyCode}` as never)
        ? t(`deliveryCompany.${deliveryCompanyCode}` as never)
        : deliveryCompanyCode;

    const paymentMethod = orderDetail?.orderInfo?.paymentMethod;
    const paymentMethodLabel =
        paymentMethod && t.has(`paymentMethod.${paymentMethod}` as never)
            ? t(`paymentMethod.${paymentMethod}` as never)
            : paymentMethod ?? "";

    const title = isGuest
        ? `${t("guestOrderDetailTitle")}: ${orderId}`
        : `${t("orderDetail")}: ${orderId}`;

    const card = (
        <Card ref={componentRef}>
            <CardHeader className="p-2">
                <CardTitle className="flex flex-row flex-wrap items-center gap-2 p-2">
                    {title}
                    <Button variant="outline" onClick={handlePrint}>
                        <Printer className="w-4 h-4" /> {t("print")}
                    </Button>
                    <Button variant="outline" onClick={handlePrint2}>
                        <Printer className="w-4 h-4" /> {t("printOnlyOrderProducts")}
                    </Button>
                </CardTitle>
            </CardHeader>
            <Separator className="my-2" />
            <CardContent className="p-3 pt-0">
                <div ref={componentRef2}>
                    {orderDetail && <OrderDetailProducts orderDetail={orderDetail} />}
                </div>
                <Separator className="my-2" />
                <div className="grid grid-cols-1 xl:grid-cols-2 gap-2">
                    <div className="flex flex-col gap-2">
                        <Table className="w-full border text-xs">
                            <TableHeader>
                                <TableRow>
                                    <TableHead
                                        colSpan={4}
                                        className="h-8 px-2 py-1 text-sm font-semibold bg-muted/50"
                                    >
                                        {t("orderInfoLabel")}
                                    </TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                <TableRow>
                                    <TableHead className="h-8 w-24 px-2 py-1 whitespace-nowrap">
                                        {t("orderNumberLabel")}
                                    </TableHead>
                                    <TableCell className="px-2 py-1">
                                        {orderDetail?.orderInfo?.id}
                                    </TableCell>
                                    <TableHead className="h-8 w-24 px-2 py-1 whitespace-nowrap">
                                        {t("orderDateLabel")}
                                    </TableHead>
                                    <TableCell className="px-2 py-1">
                                        {orderDetail?.orderInfo?.paymentDate != null
                                            ? new Date(
                                                  orderDetail.orderInfo.paymentDate,
                                              ).toLocaleString()
                                            : ""}
                                    </TableCell>
                                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                        {t("orderTypeQuantityLabel")}
                                    </TableHead>
                                    <TableCell className="px-2 py-1" colSpan={3}>
                                        {orderDetail?.orderInfo?.orderLineCount}
                                        {t("typeUnit")}/{orderDetail?.orderInfo?.totalQuantity}
                                        {t("quantityUnit")}
                                    </TableCell>
                                </TableRow>
                            </TableBody>
                        </Table>
                        <Table className="w-full border text-xs">
                            <TableHeader>
                                <TableRow>
                                    <TableHead
                                        colSpan={4}
                                        className="h-8 px-2 py-1 text-sm font-semibold bg-muted/50"
                                    >
                                        {t("paymentInfoLabel")}
                                        <span className="ml-2 font-normal text-muted-foreground">
                                            (
                                            {orderDetail?.orderInfo?.paymentDate != null
                                                ? new Date(
                                                      orderDetail.orderInfo.paymentDate,
                                                  ).toLocaleString()
                                                : t("unpaid")}
                                            )
                                        </span>
                                    </TableHead>
                                    <TableHead className="h-8 w-28 px-2 py-1 whitespace-nowrap">
                                        {t("paymentMethodLabel")}
                                    </TableHead>
                                    <TableCell className="px-2 py-1" colSpan={3}>
                                        {paymentMethodLabel}
                                    </TableCell>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                <TableRow>
                                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                        {t("productSubtotalLabel")}(a)
                                    </TableHead>
                                    <TableCell className="px-2 py-1">
                                    ₩{productSubtotal != null
                                            ? productSubtotal.toLocaleString()
                                            : "—"}
                                    </TableCell>
                                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                        {t("deliveryFeeLabel")}(b)
                                    </TableHead>
                                    <TableCell className="px-2 py-1">
                                    ₩{orderDetail?.orderInfo?.deliveryFee?.toLocaleString() ?? "0"}
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap bg-muted">
                                        {t("totalPaymentAmountLabel")}(c=a+b)
                                        <span className="block text-[10px] font-normal text-muted-foreground">
                                            {t("totalPaymentAmountDescription")}
                                        </span>
                                    </TableHead>
                                    <TableCell className="px-2 py-1 bg-muted">
                                    ₩{grossBeforePoints != null
                                            ? grossBeforePoints.toLocaleString()
                                            : "—"}
                                    </TableCell>
                                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                        {t("usedPointAmountLabel")}(d)
                                    </TableHead>
                                    <TableCell className="px-2 py-1">
                                        {orderDetail?.orderInfo?.usedPointAmount?.toLocaleString()}
                                    </TableCell>
                                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                        {t("actualPaymentAmountLabel")}(e=c-d)
                                    </TableHead>
                                    <TableCell className="px-2 py-1">
                                    ₩{orderDetail?.orderInfo?.actualPaymentAmount?.toLocaleString()}
                                    </TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                        {t("earnedPointLabel")}
                                    </TableHead>
                                    <TableCell className="px-2 py-1" colSpan={5}>
                                        {orderDetail?.orderInfo?.totalEarnedPoints != null
                                            ? orderDetail.orderInfo.totalEarnedPoints.toLocaleString()
                                            : "—"}
                                    </TableCell>
                                </TableRow>
                            </TableBody>
                        </Table>
                    </div>
                    <Table className="w-full border text-xs">
                        <TableHeader>
                            <TableRow>
                                <TableHead
                                    colSpan={6}
                                    className="h-8 px-2 py-1 text-sm font-semibold bg-muted/50"
                                >
                                    {t("deliveryInfoLabel")}
                                </TableHead>
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            <TableRow>
                                <TableHead className="h-8 w-20 px-2 py-1 whitespace-nowrap">
                                    {t("recipientNameLabel")}
                                </TableHead>
                                <TableCell className="px-2 py-1">
                                    {orderDetail?.orderInfo?.recipientName}
                                </TableCell>
                                <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                    {t("recipientPhoneLabel")}
                                </TableHead>
                                <TableCell className="px-2 py-1">
                                    {orderDetail?.orderInfo?.recipientPhone}
                                </TableCell>
                                <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                    {t("recipientEmailLabel")}
                                </TableHead>
                                <TableCell className="px-2 py-1">
                                    {orderDetail?.orderInfo?.recipientEmail}
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                    {t("deliveryCompanyLabel")}
                                </TableHead>
                                <TableCell className="px-2 py-1">
                                    {deliveryCompanyLabel}
                                </TableCell>
                                <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                    {t("deliveryFeeLabel")}
                                </TableHead>
                                <TableCell className="px-2 py-1">
                                ₩{orderDetail?.orderInfo?.deliveryFee?.toLocaleString()}
                                </TableCell>
                                <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                    {t("orderStatusLabel")}
                                </TableHead>
                                <TableCell className="px-2 py-1">{orderStatusLabel}</TableCell>
                            </TableRow>
                            <TableRow>
                                <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                    {t("recipientAddressLabel")}
                                </TableHead>
                                <TableCell className="px-2 py-1" colSpan={5}>
                                    {orderDetail?.orderInfo?.recipientAddress || "—"}
                                    {orderDetail?.orderInfo?.recipientAddressDetail && (
                                    <span>
                                        {" "}{orderDetail.orderInfo.recipientAddressDetail}
                                    </span>
                                    )}
                                    {orderDetail?.orderInfo?.postalCode && 
                                    <span className="text-xs text-muted-foreground"> ({orderDetail?.orderInfo?.postalCode})</span>}
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                <TableHead className="h-8 px-2 py-1 whitespace-nowrap align-top">
                                    {t("orderRequestLabel")}
                                </TableHead>
                                <TableCell className="px-2 py-1" colSpan={5}>
                                    {orderDetail?.orderInfo?.orderRequest || "—"}
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                                    {t("deliveryTrackingNumberLabel")}
                                </TableHead>
                                <TableCell className="px-2 py-1" colSpan={5}>
                                    {orderDetail?.orderInfo?.deliveryTrackingNumber || "—"}
                                </TableCell>
                            </TableRow>
                            <TableRow>
                                <TableHead className="h-8 px-2 py-1 whitespace-nowrap align-top">
                                    {t("deliveryMemoLabel")}
                                </TableHead>
                                <TableCell className="px-2 py-1" colSpan={5}>
                                    {orderDetail?.orderInfo?.deliveryMemo || "—"}
                                </TableCell>
                            </TableRow>
                        </TableBody>
                    </Table>
                </div>
            </CardContent>
        </Card>
    );

    if (isGuest) {
        return (
            <div className="my-4 max-w-6xl mx-auto space-y-4">
                <Button variant="ghost" size="sm" asChild className="px-0">
                    <Link href="/guest-order-check">
                        <ArrowLeft className="w-4 h-4 mr-1" />
                        {t("backToGuestOrderCheck")}
                    </Link>
                </Button>
                {card}
            </div>
        );
    }

    return card;
}
