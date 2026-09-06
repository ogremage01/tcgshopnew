/** 백엔드 `com.shop.order.enums.OrderStatus`와 동일한 코드 */
export const ORDER_STATUS_CODES = [
    "ORDER_PENDING",
    "ORDER_COMPLETED",
    "ORDER_RECEIPT_COMPLETED",
    "ORDER_DELIVERY_COMPLETED",
    "ORDER_CANCELLED",
] as const;

export type OrderStatusCode = (typeof ORDER_STATUS_CODES)[number];

export const ORDER_STATUS = {
    ORDER_PENDING: "ORDER_PENDING",
    ORDER_COMPLETED: "ORDER_COMPLETED",
    ORDER_RECEIPT_COMPLETED: "ORDER_RECEIPT_COMPLETED",
    ORDER_DELIVERY_COMPLETED: "ORDER_DELIVERY_COMPLETED",
    ORDER_CANCELLED: "ORDER_CANCELLED",
} as const satisfies Record<OrderStatusCode, OrderStatusCode>;

/** 관리자 UI용 한글 라벨 (사용자 마이페이지는 locale `order.status.*` 사용) */
export const ORDER_STATUS_LABEL_KO: Record<OrderStatusCode, string> = {
    ORDER_PENDING: "결제 확인중",
    ORDER_COMPLETED: "주문 완료",
    ORDER_RECEIPT_COMPLETED: "접수 완료",
    ORDER_DELIVERY_COMPLETED: "발송 완료",
    ORDER_CANCELLED: "취소",
};

export function getOrderStatusLabelKo(status: string): string {
    return ORDER_STATUS_LABEL_KO[status as OrderStatusCode] ?? status;
}

/** 관리자 주문 목록 행 배경 */
export const ORDER_STATUS_ROW_BADGE_CLASS: Record<OrderStatusCode, string> = {
    ORDER_COMPLETED: "bg-green-500",
    ORDER_RECEIPT_COMPLETED: "bg-blue-500",
    ORDER_PENDING: "bg-yellow-500",
    ORDER_DELIVERY_COMPLETED: "bg-green-500",
    ORDER_CANCELLED: "bg-gray-500",
};

export function getOrderStatusRowBadgeClass(status: string): string {
    return ORDER_STATUS_ROW_BADGE_CLASS[status as OrderStatusCode] ?? "";
}

/** 관리자 상태 Select 옵션 (표시 순서) */
export const ADMIN_ORDER_STATUS_SELECT_OPTIONS = ORDER_STATUS_CODES.map((value) => ({
    value,
    label: ORDER_STATUS_LABEL_KO[value],
}));

/** 관리자 목록 필터 UI 키 → API `orderStatusList` */
export const ADMIN_ORDER_STATUS_FILTER_MAP = {
    ACTIVE: ["ORDER_COMPLETED", "ORDER_RECEIPT_COMPLETED", "ORDER_PENDING"],
    ORDER_PENDING: ["ORDER_PENDING"],
    ORDER_COMPLETED: ["ORDER_COMPLETED"],
    ORDER_RECEIPT_COMPLETED: ["ORDER_RECEIPT_COMPLETED"],
    ORDER_DELIVERY_COMPLETED: ["ORDER_DELIVERY_COMPLETED"],
    ORDER_CANCELLED: ["ORDER_CANCELLED"],
    /** 전체: 상태 필터 없음 */
    ALL: [],
} as const satisfies Record<string, readonly OrderStatusCode[]>;

export type AdminOrderStatusFilterKey = keyof typeof ADMIN_ORDER_STATUS_FILTER_MAP;

const ADMIN_ORDER_STATUS_FILTER_LABEL_KO: Record<
    Exclude<AdminOrderStatusFilterKey, "ALL">,
    string
> = {
    ACTIVE: "발송 완료 및 취소 외",
    ORDER_PENDING: ORDER_STATUS_LABEL_KO.ORDER_PENDING,
    ORDER_COMPLETED: ORDER_STATUS_LABEL_KO.ORDER_COMPLETED,
    ORDER_RECEIPT_COMPLETED: ORDER_STATUS_LABEL_KO.ORDER_RECEIPT_COMPLETED,
    ORDER_DELIVERY_COMPLETED: ORDER_STATUS_LABEL_KO.ORDER_DELIVERY_COMPLETED,
    ORDER_CANCELLED: ORDER_STATUS_LABEL_KO.ORDER_CANCELLED,
};

/** 필터 Select — `ALL` 제외 (구분선 아래에 별도 렌더) */
export const ADMIN_ORDER_STATUS_FILTER_OPTIONS = (
    Object.keys(ADMIN_ORDER_STATUS_FILTER_MAP) as AdminOrderStatusFilterKey[]
)
    .filter((key): key is Exclude<AdminOrderStatusFilterKey, "ALL"> => key !== "ALL")
    .map((value) => ({
        value,
        label: ADMIN_ORDER_STATUS_FILTER_LABEL_KO[value],
    }));
