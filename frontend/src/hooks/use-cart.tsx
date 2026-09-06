// useMutation: 조작을 위한 훅 useQuery: 열람을 위한 훅 useQueryClient: 캐시 관리를 위한 훅
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";

import { toast } from "sonner";
import { CartDto } from "@/types/cart";
import { api, getApiErrorMessage } from "@/lib/api.client";
import { CartUpdateRequest } from "@/types/cart";
import { useTranslations } from "next-intl";
//장바구니 열람
export const useCartQuery = () => {
    return useQuery({
        queryKey: ["cart"],
        queryFn: async () => {
            const response = await api.get<CartDto>("/api/cart/me");
            return response;
        },
    });
};



//장바구니 조작
export const useCartMutation = () => {
    const queryClient = useQueryClient();
    const t = useTranslations("cart");
    const invalidate = () => {
        queryClient.invalidateQueries({ queryKey: ["cart"] });
    };


    //장바구니 추가(새 아이템 추가)
    const addCart = useMutation({
        mutationFn: async ({ searchMapId, cartUpdateRequest }: { searchMapId: number, cartUpdateRequest: CartUpdateRequest }) => {
            const response = await api.post<CartDto>(`/api/cart/items/${searchMapId}`, cartUpdateRequest);
            return response;
        },
        onSuccess: () => {
            toast.success(t("addCartSuccess"));
            invalidate();
        },
        onError: (error) => {
            const messageCode = getApiErrorMessage(error);
            if (messageCode === "STOCK_OVERFLOW") {
                toast.error(t("stockOverflowError"));
            } else if (messageCode === "PRODUCT_UNAVAILABLE") {
                toast.error(t("productUnavailableError"));
                invalidate();
            } else {
                toast.error(t("addCartError"));
            }
        },
    });
    //장바구니 수정(수량 변경)
    const updateCart = useMutation({
        mutationFn: async ({ id, quantity }: { id: number, quantity: number }) => {
            const response = await api.patch<CartDto>(`/api/cart/items/${id}`, { quantity });
            return response;
        },
        onSuccess: () => {
            toast.success(t("updateCartSuccess"));
            invalidate();
        },
        onError: (error) => {
            const messageCode = getApiErrorMessage(error);
            if (messageCode === "STOCK_OVERFLOW") {
                toast.error(t("stockOverflowError"));
            } else if (messageCode === "PRODUCT_UNAVAILABLE") {
                toast.error(t("productUnavailableError"));
                invalidate();
            } else {
                toast.error(t("updateCartError"));
            }
        },
    });
    //장바구니 아이템 삭제
    const removeCartItem = useMutation({
        
        mutationFn: async (id: number) => {
            if (confirm(t("removeCartItemConfirm"))) {
                const response = await api.delete<CartDto>(`/api/cart/items/${id}`);
                return response;
            }
            return null;
        },
        onSuccess: (data) => {
            if (data === null) return;
            toast.success(t("removeCartItemSuccess"));
            invalidate();
        },
        onError: (error) => {
            toast.error(getApiErrorMessage(error) ?? t("removeCartItemError"));
        },
    });
    //장바구니 비우기(전체 삭제)
    const clearCart = useMutation({
        mutationFn: async () => {
            if (confirm(t("clearCartConfirm"))) {
                const response = await api.delete("/api/cart/items");
                return response;
            } else {
                return null;
            }
        },
        onSuccess: (data) => {
            if (data === null) return;
            toast.success(t("clearCartSuccess"));
            invalidate();
        },
        onError: (error) => {
            toast.error(getApiErrorMessage(error) ?? t("clearCartError"));
        },
    });
    return { addCart, updateCart, removeCartItem, clearCart };
};