import { api } from "@/lib/api";
import { getApiErrorMessage } from "@/lib/api.client";
import { toast } from "sonner";

function toastIfOrderAlreadyCancelled(err: unknown): boolean {
    if (getApiErrorMessage(err) === "ORDER_ALREADY_CANCELLED") {
        toast.error("취소된 주문은 변경할 수 없습니다.");
        return true;
    }
    return false;
}

export async function saveAdminOrderDeliveryTrackingNumber(
    orderId: number | string,
    deliveryTrackingNumber: string,
): Promise<void> {
    try {
        await api.put<void>(`/api/admin/orders/${orderId}/delivery-tracking`, {
            deliveryTrackingNumber,
        });
        toast.success("송장번호가 저장되었습니다.");
    } catch (err) {
        console.error(err);
        toast.error("송장번호 저장에 실패했습니다.");
        throw err;
    }
}

export async function saveAdminOrderDeliveryMemo(
    orderId: number | string,
    deliveryMemo: string,
): Promise<void> {
    try {
        await api.put<void>(`/api/admin/orders/${orderId}/delivery-memo`, {
            deliveryMemo,
        });
        toast.success("안내 메시지가 저장되었습니다.");
    } catch (err) {
        console.error(err);
        toast.error("안내 메시지 저장에 실패했습니다.");
        throw err;
    }
}

export async function saveAdminOrderDeliveryInfo(
    orderId: number | string,
    deliveryTrackingNumber: string,
    deliveryMemo: string,
): Promise<void> {
    try {
        await api.put<void>(`/api/admin/orders/${orderId}/delivery-info`, {
            deliveryTrackingNumber,
            deliveryMemo,
        });
        toast.success("배송 정보가 저장되었습니다.");
    } catch (err) {
        console.error(err);
        if (!toastIfOrderAlreadyCancelled(err)) {
            toast.error("배송 정보 저장에 실패했습니다.");
        }
        throw err;
    }
}
