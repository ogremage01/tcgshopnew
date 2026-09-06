/** 백엔드 `com.shop.order.enums.DeliveryCompany`와 동일한 코드 */
export const DELIVERY_COMPANY = {
    STORE_PICKUP: "STORE_PICKUP",
    CJ_LOGISTICS: "CJ_LOGISTICS",
} as const;

export type DeliveryCompanyCode =
    (typeof DELIVERY_COMPANY)[keyof typeof DELIVERY_COMPANY];

const STORE_PICKUP_CODES = new Set([
    DELIVERY_COMPANY.STORE_PICKUP,
    "DIRECT_PICKUP",
]);

/** 레거시 `DIRECT_PICKUP`·null은 매장 수령으로 본다. */
export function resolveDeliveryCompanyCode(
    code: string | null | undefined,
): DeliveryCompanyCode | string {
    if (!code || STORE_PICKUP_CODES.has(code)) {
        return DELIVERY_COMPANY.STORE_PICKUP;
    }
    return code;
}

/** 관리자 UI용 한글 라벨 (사용자 마이페이지는 locale `order.deliveryCompany.*` 사용) */
export const DELIVERY_COMPANY_LABEL_KO: Record<DeliveryCompanyCode, string> = {
    STORE_PICKUP: "매장 수령",
    CJ_LOGISTICS: "CJ 대한통운",
};

export function getDeliveryCompanyLabelKo(
    code: string | null | undefined,
): string {
    const resolved = resolveDeliveryCompanyCode(code);
    return (
        DELIVERY_COMPANY_LABEL_KO[resolved as DeliveryCompanyCode] ?? resolved
    );
}
