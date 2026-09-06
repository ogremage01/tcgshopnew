import { toast } from "sonner";
import { AdminOrderAdjustmentResponse } from "@/types/order";

function formatCartRestoreSummary(result: AdminOrderAdjustmentResponse): string {
    if (!result.cartRestoreApplied) {
        return "";
    }
    const restored = result.cartLinesFullyRestored + result.cartLinesPartiallyRestored;
    let message = `장바구니 ${restored}건 복구`;
    if (result.cartLinesPartiallyRestored > 0) {
        message += ` (${result.cartLinesPartiallyRestored}건 재고 부족으로 일부만 복구)`;
    }
    if (result.cartLinesSkipped > 0) {
        message += ` (${result.cartLinesSkipped}건 스킵)`;
    }
    return message;
}

/** 취소 성공 메시지 (재고·장바구니 복구 옵션 반영) */
export function buildAdminOrderCancelSuccessMessage(
    result: AdminOrderAdjustmentResponse,
    restoreStock: boolean,
): string {
    const parts: string[] = [];
    if (restoreStock) {
        parts.push("주문이 취소되었고 재고가 복구되었습니다.");
    } else {
        parts.push("주문이 취소되었습니다.");
    }
    const cartSummary = formatCartRestoreSummary(result);
    if (cartSummary) {
        parts.push(cartSummary);
    }
    return parts.join(" ");
}

/** 취소·부분수정 공통 PG 환불 결과 toast */
export function toastAdminOrderAdjustmentResult(
    result: AdminOrderAdjustmentResponse,
    successMessage: string,
    pgFailurePrefix: string,
): void {
    if (!result.pgRefundSuccess) {
        toast.error(
            `${pgFailurePrefix} (환불 처리 실패: ${result.pgRefundMessage ?? "알 수 없는 오류"})`,
        );
        return;
    }
    toast.success(successMessage);
}
