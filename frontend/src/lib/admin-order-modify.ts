import { api } from "@/lib/api";
import {
    AdminOrderAdjustmentResponse,
    AdminOrderProductModifyRequest,
} from "@/types/order";

/**
 * 부분 주문 수정 API (`PUT /api/admin/orders/{id}/products`).
 * 수량 감소·라인 삭제·재고 복구를 처리하고 PG 환불 결과를 반환한다.
 */
export async function modifyAdminOrderProducts(
    orderId: number | string,
    request: AdminOrderProductModifyRequest,
): Promise<AdminOrderAdjustmentResponse> {
    return api.put<AdminOrderAdjustmentResponse>(
        `/api/admin/orders/${orderId}/products`,
        request,
    );
}
