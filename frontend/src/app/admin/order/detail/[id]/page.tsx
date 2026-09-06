"use client";

import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { useParams, useRouter } from "next/navigation";
import { Separator } from "@/components/ui/separator";
import { Button } from "@/components/ui/button";
import { Pencil, Printer } from "lucide-react";
import { useReactToPrint } from "react-to-print";
import { useRef, useState, useEffect, useCallback, useMemo } from "react";
import {
  Table,
  TableHeader,
  TableBody,
  TableFooter,
  TableRow,
  TableHead,
  TableCell,
} from "@/components/ui/table";
import { Textarea } from "@/components/ui/textarea";
import { Input } from "@/components/ui/input";
import { apiClient } from "@/lib/api.client";
import ProductImage from "@/components/product/ProductImage";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import {
  AdminOrderDetailDto,
  AdminOrderProductModifyRequest,
  OrderInfoDto,
} from "@/types/order";
import {
  AdminOrderCardProductDto,
  AdminOrderManualProductDto,
  AdminOrderSealedProductDto,
  AdminOrderSupplyProductDto,
} from "@/types/order";
import {
  cancelAdminOrder,
  createAdminOrderStatusChangeHandler,
  requiresAdminOrderPgRefund,
} from "@/lib/admin-order-status";
import { ORDER_STATUS } from "@/lib/order-status";
import { getDeliveryCompanyLabelKo } from "@/lib/delivery-company";
import { saveAdminOrderDeliveryInfo } from "@/lib/admin-order-delivery";
import { modifyAdminOrderProducts } from "@/lib/admin-order-modify";
import { toastAdminOrderAdjustmentResult } from "@/lib/admin-order-adjustment";
import { getApiErrorMessage } from "@/lib/api.client";
import { toast } from "sonner";
import { AdminOrderStatusSelect } from "@/app/admin/order/_components/AdminOrderStatusSelect";
import { useAdminOrderCancelDialog } from "@/app/admin/order/_components/useAdminOrderCancelDialog";
import PrintingTypeIcon from "@/components/product/PrintTypeLabel";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
  Dialog,
  DialogTrigger,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogClose,
} from "@/components/ui/dialog";
import { Checkbox } from "@/components/ui/checkbox";

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

type ModifyItemState = {
  originalQuantity: number;
  quantity: number;
  deleted: boolean;
  restoreStock: boolean;
};

function wouldRemoveAllOrderProducts(
  modifyItems: Record<number, ModifyItemState>,
): boolean {
  const items = Object.values(modifyItems);
  return items.length > 0 && items.every((state) => state.deleted);
}

function ModifyProductStockControls({
  productId,
  idPrefix,
  itemState,
  onUpdate,
}: {
  productId: number;
  idPrefix: string;
  itemState: ModifyItemState | undefined;
  onUpdate: (update: Partial<ModifyItemState>) => void;
}) {
  const deleted = itemState?.deleted ?? false;
  const quantityDecreased =
    itemState != null &&
    !deleted &&
    itemState.quantity < itemState.originalQuantity;

  return (
    <>
      <div className="flex flex-row items-center gap-2">
        <Checkbox
          id={`delete-${idPrefix}-${productId}`}
          checked={deleted}
          onCheckedChange={(checked) =>
            onUpdate({
              deleted: !!checked,
              restoreStock: !!checked
                ? (itemState?.restoreStock ?? false)
                : false,
            })
          }
        />
        <Label htmlFor={`delete-${idPrefix}-${productId}`} className="text-xs">
          삭제
        </Label>
      </div>
      {deleted || quantityDecreased ? (
        <div className="flex flex-row items-center gap-2">
          <Checkbox
            id={`restore-${idPrefix}-${productId}`}
            checked={itemState?.restoreStock ?? false}
            onCheckedChange={(checked) => onUpdate({ restoreStock: !!checked })}
          />
          <Label
            htmlFor={`restore-${idPrefix}-${productId}`}
            className="text-xs"
          >
            재고 복구
            {quantityDecreased && !deleted
              ? ` (차감 ${itemState.originalQuantity - itemState.quantity}개)`
              : ""}
          </Label>
        </div>
      ) : null}
    </>
  );
}

export default function OrderDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params?.id as string;
  const queryClient = useQueryClient();
  const componentRef = useRef<HTMLDivElement>(null);
  const componentRef2 = useRef<HTMLDivElement>(null);
  const [orderStatus, setOrderStatus] = useState("");
  const [deliveryTrackingNumber, setDeliveryTrackingNumber] = useState("");
  const [deliveryMemo, setDeliveryMemo] = useState("");
  const router = useRouter();
  const [modifyDialogOpen, setModifyDialogOpen] = useState(false);
  const [modifyItems, setModifyItems] = useState<
    Record<number, ModifyItemState>
  >({});
  const [modifyCancelReason, setModifyCancelReason] = useState("");
  const {
    requestCancelConfirm,
    runWithCancelLoading,
    dialog: cancelDialog,
  } = useAdminOrderCancelDialog();

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
  const {
    data: orderDetail,
    isLoading,
    error,
  } = useQuery<AdminOrderDetailDto>({
    queryKey: ["orderDetail", id],
    queryFn: () =>
      apiClient
        .get<AdminOrderDetailDto>(`/api/admin/orders/detail/${id}`)
        .then((res) => res.data),
  });
  const serverOrderStatus = orderDetail?.orderInfo.orderStatus ?? "";
  const serverDeliveryTrackingNumber =
    orderDetail?.orderInfo.deliveryTrackingNumber ?? "";
  const serverDeliveryMemo = orderDetail?.orderInfo.deliveryMemo ?? "";
  useEffect(() => {
    setOrderStatus(serverOrderStatus);
    setDeliveryTrackingNumber(serverDeliveryTrackingNumber);
    setDeliveryMemo(serverDeliveryMemo);
  }, [id, serverOrderStatus, serverDeliveryTrackingNumber, serverDeliveryMemo]);

  const initModifyItems = useCallback(() => {
    const initial: Record<number, ModifyItemState> = {};
    orderDetail?.orderCardProductGroups?.forEach((group) =>
      group.products.forEach((p) => {
        initial[p.id] = {
          originalQuantity: p.quantity,
          quantity: p.quantity,
          deleted: false,
          restoreStock: false,
        };
      }),
    );
    orderDetail?.orderManualProducts?.forEach((p) => {
      initial[p.id] = {
        originalQuantity: p.quantity,
        quantity: p.quantity,
        deleted: false,
        restoreStock: false,
      };
    });
    orderDetail?.orderSealedProducts?.forEach((p) => {
      initial[p.id] = {
        originalQuantity: p.quantity,
        quantity: p.quantity,
        deleted: false,
        restoreStock: false,
      };
    });
    orderDetail?.orderSupplyProducts?.forEach((p) => {
      initial[p.id] = {
        originalQuantity: p.quantity,
        quantity: p.quantity,
        deleted: false,
        restoreStock: false,
      };
    });
    setModifyItems(initial);
  }, [orderDetail]);

  const handleModifySubmit = useCallback(async () => {
    const trimmedReason = modifyCancelReason.trim();
    if (!trimmedReason) {
      toast.error("취소(환불) 사유를 입력해 주세요.");
      return;
    }

    const items: AdminOrderProductModifyRequest["items"] = Object.entries(
      modifyItems,
    )
      .filter(([, state]) => {
        if (state.deleted) return true;
        return state.quantity < state.originalQuantity;
      })
      .map(([productId, state]) => ({
        orderProductId: Number(productId),
        newQuantity: state.deleted ? undefined : state.quantity,
        deleted: state.deleted,
        restoreStock: state.restoreStock,
      }));

    if (items.length === 0) {
      toast.error("변경된 항목이 없습니다.");
      return;
    }

    const allProductsRemoved = wouldRemoveAllOrderProducts(modifyItems);
    const confirmMessage = allProductsRemoved
      ? "주문한 제품이 없어집니다. 주문을 취소하시겠습니까?"
      : "주문 수정을 진행하시겠습니까?";
    if (!confirm(confirmMessage)) {
      return;
    }

    try {
      const result = await modifyAdminOrderProducts(id, {
        items,
        cancelReason: trimmedReason,
      });
      await queryClient.invalidateQueries({ queryKey: ["orderDetail", id] });
      setModifyDialogOpen(false);
      setModifyCancelReason("");

      if (result.orderCancelled) {
        setOrderStatus(ORDER_STATUS.ORDER_CANCELLED);
        toastAdminOrderAdjustmentResult(
          result,
          "주문이 취소되었습니다.",
          "주문 취소 완료",
        );
        return;
      }

      toastAdminOrderAdjustmentResult(
        result,
        "주문 수정이 완료되었습니다.",
        "주문 수정 완료",
      );
    } catch (error) {
      console.error(error);
      const message = getApiErrorMessage(error);
      if (message === "ORDER_ALREADY_CANCELLED") {
        toast.error("취소된 주문은 변경할 수 없습니다.");
      } else if (message === "CANCEL_REASON_REQUIRED") {
        toast.error("취소(환불) 사유를 입력해 주세요.");
      } else if (message === "TOSS_REFUND_FAILED") {
        toast.error(
          "토스 환불에 실패했습니다. 주문은 유지되었으니 잠시 후 다시 시도해 주세요.",
        );
      } else {
        toast.error("주문 수정에 실패했습니다. " + (error as Error).message);
      }
    }
  }, [id, modifyItems, modifyCancelReason, queryClient]);

  const handleOrderStatusChange = useMemo(
    () =>
      createAdminOrderStatusChangeHandler(
        id,
        () => orderStatus,
        setOrderStatus,
        requestCancelConfirm,
        {
          isGuestOrder: () => orderDetail?.orderInfo.guest ?? undefined,
          requiresPgRefund: () =>
            requiresAdminOrderPgRefund(orderDetail?.orderInfo),
          runWithCancelLoading,
        },
      ),
    [
      id,
      orderStatus,
      orderDetail?.orderInfo,
      requestCancelConfirm,
      runWithCancelLoading,
      setOrderStatus,
    ],
  );
  const handleDeliveryTrackingNumberChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setDeliveryTrackingNumber(e.target.value);
    },
    [],
  );
  const handleDeliveryMemoChange = useCallback(
    (e: React.ChangeEvent<HTMLTextAreaElement>) => {
      setDeliveryMemo(e.target.value);
    },
    [],
  );
  const handleDeliverySave = useCallback(async () => {
    try {
      await saveAdminOrderDeliveryInfo(
        id,
        deliveryTrackingNumber,
        deliveryMemo,
      );
      queryClient.setQueryData<AdminOrderDetailDto>(
        ["orderDetail", id],
        (current) =>
          current == null
            ? current
            : {
                ...current,
                orderInfo: {
                  ...current.orderInfo,
                  deliveryTrackingNumber,
                  deliveryMemo,
                },
              },
      );
      await queryClient.invalidateQueries({ queryKey: ["orderDetail", id] });
    } catch {
      // toast는 saveAdminOrderDeliveryInfo에서 처리
    }
  }, [id, deliveryTrackingNumber, deliveryMemo, queryClient]);
  const handleCancelOrder = useCallback(async () => {
    try {
      const options = await requestCancelConfirm({
        isGuest: orderDetail?.orderInfo.guest ?? undefined,
        requiresPgRefund: requiresAdminOrderPgRefund(orderDetail?.orderInfo),
      });
      if (!options) {
        return;
      }
      await runWithCancelLoading(() => cancelAdminOrder(id, options));
      setOrderStatus(ORDER_STATUS.ORDER_CANCELLED);
      await queryClient.invalidateQueries({ queryKey: ["orderDetail", id] });
    } catch {
      // toast는 cancelAdminOrder에서 처리
    }
  }, [
    id,
    orderDetail?.orderInfo,
    queryClient,
    requestCancelConfirm,
    runWithCancelLoading,
  ]);
  if (isLoading) {
    return <div>Loading...</div>;
  }
  if (error) {
    return <div>Error: {error.message}</div>;
  }
  const info = orderDetail?.orderInfo;
  const isOrderCancelled = orderStatus === ORDER_STATUS.ORDER_CANCELLED;
  const productSubtotal = info ? resolveProductSubtotal(info) : null;
  const grossBeforePoints =
    productSubtotal == null ? null : productSubtotal + (info?.deliveryFee ?? 0);
  return (
    <div className="grid grid-cols-1 xl:grid-cols-2 gap-2">
      {cancelDialog}
      <div ref={componentRef} className="contents">
        <Card className="col-span-full">
          <CardHeader className="p-2">
            <CardTitle className="flex flex-row items-center gap-2 p-2">
              주문 상세: {id}
              <Button
                variant="outline"
                onClick={handlePrint}
                className="print:hidden"
              >
                <Printer className="w-4 h-4" /> 프린트
              </Button>
              <Button
                variant="outline"
                onClick={handlePrint2}
                className="print:hidden"
              >
                <Printer className="w-4 h-4" /> 주문 제품만 프린트
              </Button>
              <Dialog
                open={modifyDialogOpen}
                onOpenChange={(open) => {
                  setModifyDialogOpen(open);
                  if (open) {
                    initModifyItems();
                    setModifyCancelReason("");
                  }
                }}
              >
                <DialogTrigger asChild>
                  <Button variant="destructive" disabled={isOrderCancelled}>
                    <Pencil className="w-4 h-4" /> 주문 수정
                  </Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>
                    <DialogTitle>주문 수정</DialogTitle>
                    {requiresAdminOrderPgRefund(orderDetail?.orderInfo) && (
                      <p className="text-sm text-destructive font-medium">
                        이 주문은 토스 PG 결제입니다. 수정 시 차액이 부분
                        환불됩니다.
                      </p>
                    )}
                  </DialogHeader>
                  <ScrollArea className="h-[500px] pr-2">
                    <div className="flex flex-col gap-4">
                      <div className="flex flex-col gap-2">
                        <Label htmlFor="modifyCancelReason">
                          취소(환불) 사유
                        </Label>
                        <Textarea
                          id="modifyCancelReason"
                          placeholder="취소(환불) 사유를 입력하세요"
                          value={modifyCancelReason}
                          onChange={(e) =>
                            setModifyCancelReason(e.target.value)
                          }
                          rows={3}
                          maxLength={200}
                        />
                      </div>
                      {/* 카드 상품 그룹 */}
                      {orderDetail?.orderCardProductGroups?.map((group) => (
                        <div key={group.game}>
                          <h3 className="text-sm font-semibold border-b pb-1 mb-2">
                            {group.game}
                          </h3>
                          <div className="flex flex-col gap-3">
                            {group.products.map((product) => {
                              const itemState = modifyItems[product.id];
                              return (
                                <div
                                  className="flex flex-row gap-2 border rounded p-2"
                                  key={product.id}
                                >
                                  <ProductImage
                                    src={product.imageUrl}
                                    alt={product.productNameEn ?? ""}
                                    width={80}
                                    height={80}
                                  />
                                  <div className="flex flex-col gap-1 flex-1">
                                    <span className="text-sm font-semibold">
                                      {product.productNameEn ?? ""}
                                    </span>
                                    <span className="text-xs text-muted-foreground">
                                      {product.condition ?? ""}{" "}
                                      {product.printing ?? ""} ₩
                                      {product.price?.toLocaleString() ?? 0}
                                    </span>
                                    <div className="flex flex-row items-center gap-2">
                                      <Label className="text-xs whitespace-nowrap">
                                        수량
                                      </Label>
                                      <Input
                                        type="number"
                                        value={
                                          itemState?.quantity ??
                                          product.quantity
                                        }
                                        max={product.quantity}
                                        min={1}
                                        step={1}
                                        disabled={itemState?.deleted}
                                        className="w-16 h-7 text-sm"
                                        onChange={(e) => {
                                          const v = Number(e.target.value);
                                          setModifyItems((prev) => ({
                                            ...prev,
                                            [product.id]: {
                                              ...prev[product.id],
                                              quantity: v,
                                            },
                                          }));
                                        }}
                                      />
                                    </div>
                                    <ModifyProductStockControls
                                      productId={product.id}
                                      idPrefix="card"
                                      itemState={itemState}
                                      onUpdate={(update) =>
                                        setModifyItems((prev) => ({
                                          ...prev,
                                          [product.id]: {
                                            ...prev[product.id],
                                            ...update,
                                          },
                                        }))
                                      }
                                    />
                                  </div>
                                </div>
                              );
                            })}
                          </div>
                        </div>
                      ))}

                      {/* 수동 상품 */}
                      {(orderDetail?.orderManualProducts?.length ?? 0) > 0 && (
                        <div>
                          <h3 className="text-sm font-semibold border-b pb-1 mb-2">
                            기타 상품
                          </h3>
                          <div className="flex flex-col gap-3">
                            {orderDetail?.orderManualProducts?.map(
                              (product) => {
                                const itemState = modifyItems[product.id];
                                return (
                                  <div
                                    className="flex flex-row gap-2 border rounded p-2"
                                    key={product.id}
                                  >
                                    <ProductImage
                                      src={product.imageUrl}
                                      alt={product.productNameEn ?? ""}
                                      width={80}
                                      height={80}
                                    />
                                    <div className="flex flex-col gap-1 flex-1">
                                      <span className="text-sm font-semibold">
                                        {product.productNameEn ?? ""}
                                      </span>
                                      <span className="text-xs text-muted-foreground">
                                        ₩{product.price?.toLocaleString() ?? 0}
                                      </span>
                                      <div className="flex flex-row items-center gap-2">
                                        <Label className="text-xs whitespace-nowrap">
                                          수량
                                        </Label>
                                        <Input
                                          type="number"
                                          value={
                                            itemState?.quantity ??
                                            product.quantity
                                          }
                                          max={product.quantity}
                                          min={1}
                                          step={1}
                                          disabled={itemState?.deleted}
                                          className="w-16 h-7 text-sm"
                                          onChange={(e) => {
                                            const v = Number(e.target.value);
                                            setModifyItems((prev) => ({
                                              ...prev,
                                              [product.id]: {
                                                ...prev[product.id],
                                                quantity: v,
                                              },
                                            }));
                                          }}
                                        />
                                      </div>
                                      <ModifyProductStockControls
                                        productId={product.id}
                                        idPrefix="manual"
                                        itemState={itemState}
                                        onUpdate={(update) =>
                                          setModifyItems((prev) => ({
                                            ...prev,
                                            [product.id]: {
                                              ...prev[product.id],
                                              ...update,
                                            },
                                          }))
                                        }
                                      />
                                    </div>
                                  </div>
                                );
                              },
                            )}
                          </div>
                        </div>
                      )}

                      {/* 밀봉 상품 */}
                      {(orderDetail?.orderSealedProducts?.length ?? 0) > 0 && (
                        <div>
                          <h3 className="text-sm font-semibold border-b pb-1 mb-2">
                            밀봉 상품
                          </h3>
                          <div className="flex flex-col gap-3">
                            {orderDetail?.orderSealedProducts?.map(
                              (product) => {
                                const itemState = modifyItems[product.id];
                                return (
                                  <div
                                    className="flex flex-row gap-2 border rounded p-2"
                                    key={product.id}
                                  >
                                    <ProductImage
                                      src={product.imageUrl}
                                      alt={product.productNameEn ?? ""}
                                      width={80}
                                      height={80}
                                    />
                                    <div className="flex flex-col gap-1 flex-1">
                                      <span className="text-sm font-semibold">
                                        {product.productNameEn ?? ""}
                                      </span>
                                      <span className="text-xs text-muted-foreground">
                                        {product.language ?? ""}{" "}
                                        {product.game ?? ""} ₩
                                        {product.price?.toLocaleString() ?? 0}
                                      </span>
                                      <div className="flex flex-row items-center gap-2">
                                        <Label className="text-xs whitespace-nowrap">
                                          수량
                                        </Label>
                                        <Input
                                          type="number"
                                          value={
                                            itemState?.quantity ??
                                            product.quantity
                                          }
                                          max={product.quantity}
                                          min={1}
                                          step={1}
                                          disabled={itemState?.deleted}
                                          className="w-16 h-7 text-sm"
                                          onChange={(e) => {
                                            const v = Number(e.target.value);
                                            setModifyItems((prev) => ({
                                              ...prev,
                                              [product.id]: {
                                                ...prev[product.id],
                                                quantity: v,
                                              },
                                            }));
                                          }}
                                        />
                                      </div>
                                      <ModifyProductStockControls
                                        productId={product.id}
                                        idPrefix="sealed"
                                        itemState={itemState}
                                        onUpdate={(update) =>
                                          setModifyItems((prev) => ({
                                            ...prev,
                                            [product.id]: {
                                              ...prev[product.id],
                                              ...update,
                                            },
                                          }))
                                        }
                                      />
                                    </div>
                                  </div>
                                );
                              },
                            )}
                          </div>
                        </div>
                      )}

                      {/* 서플라이 상품 */}
                      {(orderDetail?.orderSupplyProducts?.length ?? 0) > 0 && (
                        <div>
                          <h3 className="text-sm font-semibold border-b pb-1 mb-2">
                            서플라이
                          </h3>
                          <div className="flex flex-col gap-3">
                            {orderDetail?.orderSupplyProducts?.map(
                              (product) => {
                                const itemState = modifyItems[product.id];
                                return (
                                  <div
                                    className="flex flex-row gap-2 border rounded p-2"
                                    key={product.id}
                                  >
                                    <ProductImage
                                      src={product.imageUrl}
                                      alt={product.productNameEn ?? ""}
                                      width={80}
                                      height={80}
                                    />
                                    <div className="flex flex-col gap-1 flex-1">
                                      <span className="text-sm font-semibold">
                                        {product.productNameEn ?? ""}
                                      </span>
                                      <span className="text-xs text-muted-foreground">
                                        {product.supplyType ?? ""}{" "}
                                        {product.maker ?? ""} ₩
                                        {product.price?.toLocaleString() ?? 0}
                                      </span>
                                      <div className="flex flex-row items-center gap-2">
                                        <Label className="text-xs whitespace-nowrap">
                                          수량
                                        </Label>
                                        <Input
                                          type="number"
                                          value={
                                            itemState?.quantity ??
                                            product.quantity
                                          }
                                          max={product.quantity}
                                          min={1}
                                          step={1}
                                          disabled={itemState?.deleted}
                                          className="w-16 h-7 text-sm"
                                          onChange={(e) => {
                                            const v = Number(e.target.value);
                                            setModifyItems((prev) => ({
                                              ...prev,
                                              [product.id]: {
                                                ...prev[product.id],
                                                quantity: v,
                                              },
                                            }));
                                          }}
                                        />
                                      </div>
                                      <ModifyProductStockControls
                                        productId={product.id}
                                        idPrefix="supply"
                                        itemState={itemState}
                                        onUpdate={(update) =>
                                          setModifyItems((prev) => ({
                                            ...prev,
                                            [product.id]: {
                                              ...prev[product.id],
                                              ...update,
                                            },
                                          }))
                                        }
                                      />
                                    </div>
                                  </div>
                                );
                              },
                            )}
                          </div>
                        </div>
                      )}
                    </div>
                  </ScrollArea>
                  <DialogFooter>
                    <DialogClose asChild>
                      <Button variant="outline">취소</Button>
                    </DialogClose>
                    <Button
                      variant="destructive"
                      onClick={handleModifySubmit}
                      disabled={!modifyCancelReason.trim()}
                    >
                      저장
                    </Button>
                  </DialogFooter>
                </DialogContent>
              </Dialog>
            </CardTitle>
          </CardHeader>
          <Separator className="my-2" />
          <CardContent className="p-3 pt-0">
            <div className="flex flex-row flex-wrap gap-2" ref={componentRef2}>
              {orderDetail?.orderCardProductGroups?.map((group) => (
                <div
                  key={group.game}
                  className="col-span-full flex flex-col gap-2"
                >
                  <h3 className="text-sm font-semibold border-b pb-1">
                    {group.game}
                  </h3>
                  <div className="flex flex-row flex-wrap gap-2">
                    {group.products.map(
                      (orderCardProduct: AdminOrderCardProductDto) => (
                        <div
                          key={orderCardProduct.id}
                          className="flex flex-col border p-2 gap-1 w-fit max-w-[140px] break-inside-avoid [page-break-inside:avoid]"
                        >
                          <div className="flex flex-col">
                            <div className="flex justify-center flex-col">
                              <ProductImage
                                src={orderCardProduct.imageUrl}
                                alt={
                                  [
                                    orderCardProduct.setCode,
                                    orderCardProduct.setNumber,
                                  ]
                                    .filter(Boolean)
                                    .join(" ") ||
                                  `product-${orderCardProduct.productId}`
                                }
                                width={100}
                                height={100}
                                className="m-auto"
                              />
                              <div className="flex flex-col text-center gap-0.5">
                                {orderCardProduct.language === "ko" ? (
                                  <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                                    {orderCardProduct.productNameKo ?? ""}
                                  </span>
                                ) : (
                                  <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                                    {orderCardProduct.productNameEn ?? ""}
                                  </span>
                                )}

                                <span className="text-xs">
                                  {orderCardProduct.language ?? ""}-
                                  {orderCardProduct.condition ?? ""}-
                                  <PrintingTypeIcon
                                    printing={orderCardProduct.printing}
                                    printType={orderCardProduct.printType}
                                    foilHighlight="badge"
                                    className="text-xs"
                                  />
                                </span>
                                <span className="text-xs text-wrap whitespace-nowrap">
                                  ₩
                                  {orderCardProduct.price?.toLocaleString() ??
                                    0}
                                </span>
                                <span className="text-xs">
                                  {orderCardProduct.setCode ?? ""}-
                                  {orderCardProduct.setNumber ?? ""}
                                </span>
                                <span className="text-xs">
                                  {orderCardProduct.storageName ?? ""}
                                </span>
                                <span className="text-base text-bold">
                                  주문 개수:{" "}
                                  {orderCardProduct.quantity.toLocaleString()}
                                </span>
                                <span className="text-xs">
                                  잔여 재고:{" "}
                                  {orderCardProduct.totalStock?.toLocaleString() ??
                                    0}
                                </span>
                                <span className="text-xs">
                                  {orderCardProduct.memo ?? ""}
                                </span>
                              </div>
                            </div>
                          </div>
                        </div>
                      ),
                    )}
                  </div>
                </div>
              ))}
              {orderDetail?.orderManualProducts?.length &&
              orderDetail?.orderManualProducts?.length > 0 ? (
                <div className="flex flex-col gap-2">
                  <h3 className="text-sm font-semibold border-b pb-1">
                    수동 제품
                  </h3>
                  <div className="flex flex-row flex-wrap gap-2">
                    {orderDetail?.orderManualProducts?.map(
                      (orderManualProduct: AdminOrderManualProductDto) => (
                        <div
                          key={orderManualProduct.id}
                          className="flex flex-col border p-2 gap-1 w-fit max-w-[140px] break-inside-avoid [page-break-inside:avoid]"
                        >
                          <div className="flex justify-center flex-col">
                            <ProductImage
                              src={orderManualProduct.imageUrl}
                              alt={`product-${orderManualProduct.productId}`}
                              width={80}
                              height={80}
                              className="m-auto"
                            />
                            <div className="flex flex-col text-center gap-0.5">
                              <span className="text-xs">
                                {orderManualProduct.productId}
                              </span>
                              <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                                {orderManualProduct.productNameEn ?? "없나?"}
                              </span>
                              <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                                {orderManualProduct.productNameKo ?? "없나?"}
                              </span>
                              <span className="text-xs">
                                ₩
                                {orderManualProduct.price?.toLocaleString() ??
                                  0}
                              </span>
                              <span className="text-base text-bold">
                                주문 개수:{" "}
                                {orderManualProduct.quantity.toLocaleString()}
                              </span>
                            </div>
                          </div>
                        </div>
                      ),
                    )}
                  </div>
                </div>
              ) : null}
              {orderDetail?.orderSealedProducts?.length &&
              orderDetail?.orderSealedProducts?.length > 0 ? (
                <div className="flex flex-col gap-2">
                  <h3 className="text-sm font-semibold border-b pb-1">
                    밀봉 제품
                  </h3>
                  <div className="flex flex-row flex-wrap gap-2">
                    {orderDetail?.orderSealedProducts?.map(
                      (orderSealedProduct: AdminOrderSealedProductDto) => (
                        <div
                          key={orderSealedProduct.id}
                          className="flex flex-col border p-2 gap-1 w-fit max-w-[140px] break-inside-avoid [page-break-inside:avoid]"
                        >
                          <div className="flex justify-center flex-col">
                            <ProductImage
                              src={orderSealedProduct.imageUrl}
                              alt={`product-${orderSealedProduct.productId}`}
                              width={80}
                              height={80}
                              className="m-auto"
                            />
                            <div className="flex flex-col text-center gap-0.5">
                              <span className="text-xs">
                                {orderSealedProduct.language ?? "없음"}-
                                {orderSealedProduct.game ?? "없음"}
                              </span>
                              <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                                {orderSealedProduct.productNameEn ?? "없음"}
                              </span>
                              <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                                {orderSealedProduct.productNameKo ?? "없음"}
                              </span>
                              <span className="text-xs">
                                ₩
                                {orderSealedProduct.price?.toLocaleString() ??
                                  0}
                              </span>
                              <span className="text-base text-bold">
                                주문 개수:{" "}
                                {orderSealedProduct.quantity.toLocaleString()}
                              </span>
                              <span className="text-xs">
                                잔여 재고:{" "}
                                {orderSealedProduct.totalStock?.toLocaleString() ??
                                  0}
                              </span>
                            </div>
                          </div>
                        </div>
                      ),
                    )}
                  </div>
                </div>
              ) : null}
              {orderDetail?.orderSupplyProducts?.length &&
              orderDetail?.orderSupplyProducts?.length > 0 ? (
                <div className="flex flex-col gap-2">
                  <h3 className="text-sm font-semibold border-b pb-1">
                    서플라이
                  </h3>
                  <div className="flex flex-row flex-wrap gap-2">
                    {orderDetail?.orderSupplyProducts?.map(
                      (orderSupplyProduct: AdminOrderSupplyProductDto) => (
                        <div
                          key={orderSupplyProduct.id}
                          className="flex flex-col border p-2 gap-1 w-fit max-w-[140px] break-inside-avoid [page-break-inside:avoid]"
                        >
                          <div className="flex justify-center flex-col">
                            <ProductImage
                              src={orderSupplyProduct.imageUrl}
                              alt={`product-${orderSupplyProduct.productId}`}
                              width={80}
                              height={80}
                              className="m-auto"
                            />
                            <div className="flex flex-col text-center gap-0.5">
                              <span className="text-xs">
                                {orderSupplyProduct.supplyType ?? ""}
                                {orderSupplyProduct.maker
                                  ? `-${orderSupplyProduct.maker}`
                                  : ""}
                              </span>
                              <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                                {orderSupplyProduct.productNameEn ?? ""}
                              </span>
                              <span className="text-xs text-ellipsis overflow-hidden whitespace-nowrap">
                                {orderSupplyProduct.productNameKo ?? ""}
                              </span>
                              <span className="text-xs">
                                ₩
                                {orderSupplyProduct.price?.toLocaleString() ??
                                  0}
                              </span>
                              <span className="text-base text-bold">
                                주문 개수:{" "}
                                {orderSupplyProduct.quantity.toLocaleString()}
                              </span>
                              <span className="text-xs">
                                잔여 재고:{" "}
                                {orderSupplyProduct.totalStock?.toLocaleString() ??
                                  0}
                              </span>
                            </div>
                          </div>
                        </div>
                      ),
                    )}
                  </div>
                </div>
              ) : null}
            </div>
          </CardContent>
        </Card>
        <div className="flex flex-col gap-4">
          <Card>
            <div className="flex flex-col gap-2">
              <Table className="w-full border text-xs">
                <TableHeader>
                  <TableRow>
                    <TableHead
                      colSpan={2}
                      className="h-8 px-2 py-1 text-sm font-semibold bg-muted/50"
                    >
                      주문 정보(
                      {getDeliveryCompanyLabelKo(
                        orderDetail?.orderInfo?.deliveryCompany,
                      )}
                      )
                    </TableHead>
                    <TableHead className="h-8 w-20 px-2 py-1 whitespace-nowrap">
                      <span className="ml-2 font-normal text-muted-foreground">
                        주문자
                      </span>
                    </TableHead>
                    <TableCell className="px-2 py-1">
                      {orderDetail?.orderInfo?.recipientName}(
                      {orderDetail?.orderInfo?.guest ? "비회원" : "회원"})
                      {orderDetail?.orderInfo?.userId
                        ? `(${orderDetail?.orderInfo?.userId})`
                        : ""}
                    </TableCell>
                    <TableHead className="h-8 w-20 px-2 py-1 whitespace-nowrap">
                      <span className="ml-2 font-normal text-muted-foreground">
                        메모
                      </span>
                    </TableHead>
                    <TableCell className="px-2 py-1">
                      {orderDetail?.orderInfo?.userMemo ?? ""}
                    </TableCell>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow>
                    <TableHead className="h-8 w-24 px-2 py-1 whitespace-nowrap">
                      주문 번호
                    </TableHead>
                    <TableCell className="px-2 py-1">
                      {orderDetail?.orderInfo?.id}
                    </TableCell>
                    <TableHead className="h-8 w-24 px-2 py-1 whitespace-nowrap">
                      주문 일시
                    </TableHead>
                    <TableCell className="px-2 py-1">
                      {orderDetail?.orderInfo?.paymentDate != null
                        ? new Date(
                            orderDetail.orderInfo.paymentDate,
                          ).toLocaleString()
                        : ""}
                    </TableCell>
                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                      종류/개수
                    </TableHead>
                    <TableCell className="px-2 py-1" colSpan={3}>
                      {orderDetail?.orderInfo?.orderLineCount}종/
                      {orderDetail?.orderInfo?.totalQuantity}개
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
              <Separator className="my-1" />
              <Table className="w-full border text-xs">
                <TableHeader>
                  <TableRow>
                    <TableHead
                      colSpan={4}
                      className="h-8 px-2 py-1 text-sm font-semibold bg-muted/50"
                    >
                      결제 정보
                      <span className="ml-2 font-normal text-muted-foreground">
                        (
                        {orderDetail?.orderInfo?.paymentDate != null
                          ? new Date(
                              orderDetail.orderInfo.paymentDate,
                            ).toLocaleString()
                          : "미결제"}
                        )
                      </span>
                    </TableHead>
                    <TableHead className="h-8 w-28 px-2 py-1 whitespace-nowrap">
                      결제 방식
                    </TableHead>
                    <TableCell className="px-2 py-1" colSpan={3}>
                      {orderDetail?.orderInfo?.paymentMethod === "카드"
                        ? "CREDIT CARD"
                        : orderDetail?.orderInfo?.paymentMethod}
                    </TableCell>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  <TableRow>
                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                      결제 통화
                    </TableHead>
                    <TableCell className="px-2 py-1">
                      {orderDetail?.orderInfo?.paymentCurrency}
                    </TableCell>
                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                      상품 총(a)
                    </TableHead>
                    <TableCell className="px-2 py-1 text-right">
                      ₩
                      {productSubtotal != null
                        ? productSubtotal.toLocaleString()
                        : "—"}
                    </TableCell>
                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                      배송료(b)
                    </TableHead>
                    <TableCell className="px-2 py-1 text-right">
                      ₩
                      {orderDetail?.orderInfo?.deliveryFee?.toLocaleString() ??
                        "0"}
                    </TableCell>
                  </TableRow>

                  <TableRow>
                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap bg-muted">
                      총 금액(c=a+b)
                      <span className="block text-[10px] font-normal text-muted-foreground">
                        포인트 차감 전
                      </span>
                    </TableHead>
                    <TableCell className="px-2 py-1 bg-muted text-right">
                      ₩
                      {grossBeforePoints != null
                        ? grossBeforePoints.toLocaleString()
                        : "—"}
                    </TableCell>
                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                      포인트(d)
                    </TableHead>
                    <TableCell className="px-2 py-1 text-right">
                      {orderDetail?.orderInfo?.usedPointAmount?.toLocaleString()}
                    </TableCell>
                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                      실결제(e=c-d)
                    </TableHead>
                    <TableCell className="px-2 py-1 text-right">
                      ₩
                      {orderDetail?.orderInfo?.actualPaymentAmount?.toLocaleString()}
                    </TableCell>
                  </TableRow>

                  <TableRow>
                    <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                      적립 포인트(비회원은 적립되지 않음)
                    </TableHead>
                    <TableCell className="px-2 py-1 text-right" colSpan={3}>
                      {orderDetail?.orderInfo?.totalEarnedPoints?.toLocaleString()}
                    </TableCell>
                  </TableRow>
                </TableBody>
              </Table>
            </div>
          </Card>
        </div>
      </div>
      <Card>
        <Table className="w-full border text-xs">
          <TableHeader>
            <TableRow>
              <TableHead
                colSpan={6}
                className="h-8 px-2 py-1 text-sm font-semibold bg-muted/50"
              >
                배송 정보
              </TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            <TableRow>
              <TableHead className="h-8 w-20 px-2 py-1 whitespace-nowrap">
                수령인
              </TableHead>
              <TableCell className="px-2 py-1">
                {orderDetail?.orderInfo?.recipientName}
              </TableCell>
              <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                연락처
              </TableHead>
              <TableCell className="px-2 py-1">
                {orderDetail?.orderInfo?.recipientPhone}
              </TableCell>
              <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                이메일
              </TableHead>
              <TableCell className="px-2 py-1">
                {orderDetail?.orderInfo?.recipientEmail}
              </TableCell>
            </TableRow>

            <TableRow>
              <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                배송 회사
              </TableHead>
              <TableCell className="px-2 py-1">
                {getDeliveryCompanyLabelKo(
                  orderDetail?.orderInfo?.deliveryCompany,
                )}
              </TableCell>
              <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                배송료
              </TableHead>
              <TableCell className="px-2 py-1">
                ₩{orderDetail?.orderInfo?.deliveryFee?.toLocaleString()}
              </TableCell>
              <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                주문 상태
              </TableHead>
              <TableCell className="px-2 py-1">
                <AdminOrderStatusSelect
                  value={orderStatus}
                  onValueChange={handleOrderStatusChange}
                  disabled={isOrderCancelled}
                />
              </TableCell>
            </TableRow>
            <TableRow>
              <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                배송지
              </TableHead>
              <TableCell className="px-2 py-1" colSpan={5}>
                {orderDetail?.orderInfo?.recipientAddress}
                {orderDetail?.orderInfo?.recipientAddressDetail && (
                  <> {orderDetail.orderInfo.recipientAddressDetail}</>
                )}
                {orderDetail?.orderInfo?.postalCode && (
                  <span className="text-xs text-muted-foreground">
                    ({orderDetail.orderInfo.postalCode})
                  </span>
                )}
              </TableCell>
            </TableRow>
            <TableRow>
              <TableHead className="h-8 px-2 py-1 whitespace-nowrap align-top">
                요청 사항
              </TableHead>
              <TableCell className="px-2 py-1" colSpan={5}>
                {orderDetail?.orderInfo?.orderRequest}
              </TableCell>
            </TableRow>
            <TableRow>
              <TableHead className="h-8 px-2 py-1 whitespace-nowrap">
                송장번호
              </TableHead>
              <TableCell className="px-2 py-1" colSpan={5}>
                <Input
                  type="text"
                  className="h-8 text-xs"
                  placeholder="송장번호"
                  value={deliveryTrackingNumber}
                  onChange={handleDeliveryTrackingNumberChange}
                  disabled={isOrderCancelled}
                />
              </TableCell>
            </TableRow>
            <TableRow>
              <TableHead className="h-8 px-2 py-1 whitespace-nowrap align-top">
                안내 메시지
              </TableHead>
              <TableCell className="px-2 py-1" colSpan={5}>
                <Textarea
                  className="min-h-[52px] text-xs resize-none"
                  rows={2}
                  placeholder="안내 메시지"
                  value={deliveryMemo}
                  onChange={handleDeliveryMemoChange}
                  disabled={isOrderCancelled}
                />
              </TableCell>
            </TableRow>
            <TableRow>
              <TableCell colSpan={6} className="px-2 py-1 text-right">
                <div className="flex flex-row gap-1 justify-end">
                  <Button
                    type="button"
                    size="sm"
                    onClick={handleDeliverySave}
                    disabled={isOrderCancelled}
                  >
                    저장
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    variant="destructive"
                    onClick={handleCancelOrder}
                    disabled={isOrderCancelled}
                  >
                    주문 취소
                  </Button>
                  <Button
                    type="button"
                    size="sm"
                    variant="outline"
                    onClick={() => router.back()}
                  >
                    뒤로가기
                  </Button>
                </div>
              </TableCell>
            </TableRow>
          </TableBody>
        </Table>
      </Card>
    </div>
  );
}
