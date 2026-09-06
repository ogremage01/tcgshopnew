import { api, getApiErrorMessage } from "@/lib/api.client";
import type { AdminOrderCancelOptions } from "@/app/admin/order/_components/AdminOrderCancelDialog";
import type {
    RequestAdminOrderCancelConfirm,
    RunWithCancelLoading,
} from "@/app/admin/order/_components/useAdminOrderCancelDialog";
import {
    buildAdminOrderCancelSuccessMessage,
    toastAdminOrderAdjustmentResult,
} from "@/lib/admin-order-adjustment";
import { ORDER_STATUS } from "@/lib/order-status";
import { AdminOrderAdjustmentResponse } from "@/types/order";
import { toast } from "sonner";

const ORDER_ALREADY_CANCELLED = "ORDER_ALREADY_CANCELLED";
const TOSS_REFUND_FAILED = "TOSS_REFUND_FAILED";

async function runMaybeWithLoading<T>(
    runWithCancelLoading: RunWithCancelLoading | undefined,
    fn: () => Promise<T>,
): Promise<T> {
    if (runWithCancelLoading) {
        return runWithCancelLoading(fn);
    }
    return fn();
}

function toastIfOrderAlreadyCancelled(err: unknown): boolean {
    const message = getApiErrorMessage(err);
    if (message === ORDER_ALREADY_CANCELLED) {
        toast.error("취소된 주문은 변경할 수 없습니다.");
        return true;
    }
    return false;
}

function toastIfTossRefundFailed(err: unknown): boolean {
    const message = getApiErrorMessage(err);
    if (message === TOSS_REFUND_FAILED) {
        toast.error(
            "토스 환불에 실패했습니다. 주문은 유지되었으니 잠시 후 다시 취소해 주세요.",
        );
        return true;
    }
    return false;
}

export function requiresAdminOrderPgRefund(orderInfo?: {
    paymentStatus?: string | null;
    pgTransactionId?: string | null;
    paymentMethod?: string | null;
    actualPaymentAmount?: number | null;
} | null): boolean {
    if (!orderInfo) {
        return false;
    }
    if (orderInfo.paymentMethod === "DIRECT") {
        return false;
    }
    if (orderInfo.paymentStatus !== "PAYMENT_COMPLETED") {
        return false;
    }
    if (!orderInfo.pgTransactionId) {
        return false;
    }
    const amount = orderInfo.actualPaymentAmount ?? 0;
    return amount > 0;
}

/**
 * 관리자 주문 취소 API (`PUT /api/admin/orders/{id}/cancel`).
 * Dialog에서 받은 options로 호출. 성공 시 조정 결과 반환.
 */
export async function cancelAdminOrder(
    orderId: number | string,
    options: AdminOrderCancelOptions,
): Promise<AdminOrderAdjustmentResponse> {
    try {
        const result = await cancelAdminOrderWithoutConfirm(
            orderId,
            options.restoreStock,
            options.restoreCartItems,
            options.cancelReason,
        );
        toastAdminOrderAdjustmentResult(
            result,
            buildAdminOrderCancelSuccessMessage(result, options.restoreStock),
            "주문 취소 완료",
        );
        return result;
    } catch (err) {
        console.error(err);
        if (
            !toastIfOrderAlreadyCancelled(err) &&
            !toastIfTossRefundFailed(err)
        ) {
            toast.error("주문 취소에 실패했습니다.");
        }
        throw err;
    }
}

/** Dialog/confirm 없이 주문 취소 API만 호출한다. */
export async function cancelAdminOrderWithoutConfirm(
    orderId: number | string,
    restoreStock: boolean,
    restoreCart = false,
    cancelReason = "",
): Promise<AdminOrderAdjustmentResponse> {
    return api.put<AdminOrderAdjustmentResponse>(`/api/admin/orders/${orderId}/cancel`, {
        restoreStock,
        restoreCart,
        cancelReason,
    });
}

/**
 * 관리자 주문 상태 변경 API 호출.
 * 취소 선택 시 Dialog에서 확인. 취소(닫기) 시 null, 성공 시 적용된 상태 문자열.
 */
export async function applyAdminOrderStatusChange(
    orderId: number | string,
    currentStatus: string,
    nextStatus: string,
    requestCancelConfirm: RequestAdminOrderCancelConfirm,
    options?: {
        isGuestOrder?: boolean;
        requiresPgRefund?: boolean;
        runWithCancelLoading?: RunWithCancelLoading;
    },
): Promise<string | null> {
    if (nextStatus === currentStatus) {
        return null;
    }

    if (nextStatus === ORDER_STATUS.ORDER_CANCELLED) {
        const cancelOptions = await requestCancelConfirm({
            isGuest: options?.isGuestOrder,
            requiresPgRefund: options?.requiresPgRefund,
        });
        if (!cancelOptions) {
            return null;
        }
        await runMaybeWithLoading(options?.runWithCancelLoading, () =>
            cancelAdminOrder(orderId, cancelOptions),
        );
        return ORDER_STATUS.ORDER_CANCELLED;
    }

    try {
        await api.put<void>(`/api/admin/orders/${orderId}/status`, {
            orderStatus: nextStatus,
        });
        toast.success("주문 상태가 변경되었습니다.");
        return nextStatus;
    } catch (err) {
        console.error(err);
        if (!toastIfOrderAlreadyCancelled(err)) {
            toast.error("주문 상태 변경에 실패했습니다.");
        }
        throw err;
    }
}

/** Select onValueChange용 핸들러 (낙관적 업데이트 + 실패 시 롤백). */
export function createAdminOrderStatusChangeHandler(
    orderId: number | string,
    getStatus: () => string,
    setStatus: (status: string) => void,
    requestCancelConfirm: RequestAdminOrderCancelConfirm,
    options?: {
        isGuestOrder?: boolean | (() => boolean | undefined);
        requiresPgRefund?: boolean | (() => boolean | undefined);
        runWithCancelLoading?: RunWithCancelLoading;
    },
) {
    return async (value: string) => {
        const current = getStatus();
        if (current === ORDER_STATUS.ORDER_CANCELLED) {
            return;
        }
        if (value === current) {
            return;
        }

        const guest =
            typeof options?.isGuestOrder === "function"
                ? options.isGuestOrder()
                : options?.isGuestOrder;
        const requiresPgRefund =
            typeof options?.requiresPgRefund === "function"
                ? options.requiresPgRefund()
                : options?.requiresPgRefund;

        if (value === ORDER_STATUS.ORDER_CANCELLED) {
            try {
                const applied = await applyAdminOrderStatusChange(
                    orderId,
                    current,
                    value,
                    requestCancelConfirm,
                    {
                        isGuestOrder: guest,
                        requiresPgRefund: requiresPgRefund ?? false,
                        runWithCancelLoading: options?.runWithCancelLoading,
                    },
                );
                if (applied) {
                    setStatus(applied);
                }
            } catch {
                // toast는 cancelAdminOrder / applyAdminOrderStatusChange에서 처리
            }
            return;
        }

        const previous = current;
        setStatus(value);
        try {
            const applied = await applyAdminOrderStatusChange(
                orderId,
                previous,
                value,
                requestCancelConfirm,
                {
                    isGuestOrder: guest,
                    requiresPgRefund: requiresPgRefund ?? false,
                    runWithCancelLoading: options?.runWithCancelLoading,
                },
            );
            if (applied) {
                setStatus(applied);
            }
        } catch {
            setStatus(previous);
        }
    };
}
