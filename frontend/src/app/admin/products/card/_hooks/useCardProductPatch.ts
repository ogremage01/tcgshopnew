"use client"

import { useCallback, useEffect, useMemo } from "react"
import { toast } from "sonner"
import debounce from "lodash-es/debounce"
import { api, getApiErrorMessage } from "@/lib/api"
import type { CardProductManagementResponseDto, CardProductPatchRequest } from "@/types/product"

type UseCardProductPatchOptions = {
    onDeleted?: (id: number) => void
}

export function useCardProductPatch(options?: UseCardProductPatchOptions) {
    const onDeleted = options?.onDeleted
    const savePatch = useCallback((id: number, body: CardProductPatchRequest) => {
        return api
            .patch<CardProductManagementResponseDto>(`/api/admin/product/single-products/${id}`, body)
            .then(() => {
                toast("수정되었습니다.", {
                    description: "수정된 내용이 저장되었습니다.",
                })
            })
            .catch((err) => console.error(getApiErrorMessage(err) ?? err))
    }, [])

    const debouncedSavePatch = useMemo(
        () =>
            debounce((id: number, body: CardProductPatchRequest) => {
                void savePatch(id, body)
            }, 600),
        [savePatch]
    )

    useEffect(() => {
        return () => debouncedSavePatch.cancel()
    }, [debouncedSavePatch])

    const schedulePatch = useCallback(
        (id: number, body: CardProductPatchRequest) => {
            debouncedSavePatch(id, body)
        },
        [debouncedSavePatch]
    )

    const flushPatch = useCallback(
        (id: number, body: CardProductPatchRequest) => {
            debouncedSavePatch.cancel()
            void savePatch(id, body)
        },
        [debouncedSavePatch, savePatch]
    )

    const deleteCard = useCallback(
        (id: number) => {
            if (!window.confirm("해당 카드 상품을 삭제하시겠습니까?"))
                return

            return api
                .post<void>(`/api/admin/product/single-products/delete/${id}`)
                .then(() => {
                    toast("삭제되었습니다.", {
                        description: "삭제된 내용이 저장되었습니다.",
                    })
                    onDeleted?.(id)
                })
                .catch((err) => console.error(getApiErrorMessage(err) ?? err))
        },
        [onDeleted]
    )

    return { savePatch, schedulePatch, flushPatch, deleteCard }
}

