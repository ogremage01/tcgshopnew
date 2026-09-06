"use client";

import { useQuery } from "@tanstack/react-query";
import { isAxiosError } from "axios";
import { apiClient } from "@/lib/api.client";
import { UserOrderDetailDto } from "@/types/order";

export type UserOrderDetailVariant = "member" | "guest";

type UseUserOrderDetailParams = {
    variant: UserOrderDetailVariant;
    orderId: string;
    guestCode?: string;
};

export function useUserOrderDetail({ variant, orderId, guestCode }: UseUserOrderDetailParams) {
    const orderIdNum = Number(orderId);
    const verificationCode = guestCode?.trim() ?? "";
    const isGuest = variant === "guest";

    return useQuery<UserOrderDetailDto>({
        queryKey: isGuest
            ? ["guestOrderDetail", orderId, verificationCode]
            : ["userOrderDetail", orderId],
        queryFn: () => {
            if (isGuest) {
                return apiClient
                    .post<UserOrderDetailDto>("/api/guest/orders/lookup", {
                        orderId: orderIdNum,
                        verificationCode,
                    })
                    .then((res) => res.data);
            }
            return apiClient
                .get<UserOrderDetailDto>(`/api/user/orders/detail/${orderId}`)
                .then((res) => res.data);
        },
        enabled: isGuest
            ? Boolean(orderId) &&
              !Number.isNaN(orderIdNum) &&
              orderIdNum > 0 &&
              Boolean(verificationCode)
            : Boolean(orderId),
        retry: (failureCount, err) => {
            if (isAxiosError(err)) {
                const status = err.response?.status ?? 0;
                if (isGuest && status === 404) {
                    return false;
                }
                if (!isGuest && [401, 403, 404].includes(status)) {
                    return false;
                }
            }
            return failureCount < 2;
        },
    });
}
