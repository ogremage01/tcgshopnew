"use client"

import { useCallback, useEffect, useMemo } from "react"
import { toast } from "sonner"
import debounce from "lodash-es/debounce"
import { api, getApiErrorMessage } from "@/lib/api"
import type { ManualProductDto, ManualProductPatchRequest } from "@/types/product"

type ManualProductSaveArgs = {
    id: number
    base: ManualProductDto
    patch: ManualProductPatchRequest
}

export function useManualProductPatch() {
    const savePatch = useCallback(({ id, base, patch }: ManualProductSaveArgs) => {
        return api
            .put<ManualProductDto>(`/api/admin/product/manual-products/${id}`, { ...base, ...patch })
            .then(() => {
                toast("수정되었습니다.", {
                    description: "수정된 내용이 저장되었습니다.",
                })
            })
            .catch((err) => console.error(getApiErrorMessage(err) ?? err))
    }, [])

    const debouncedSavePatch = useMemo(
        () =>
            debounce((args: ManualProductSaveArgs) => {
                void savePatch(args)
            }, 600),
        [savePatch]
    )

    useEffect(() => {
        return () => debouncedSavePatch.cancel()
    }, [debouncedSavePatch])

    const schedulePatch = useCallback(
        (args: ManualProductSaveArgs) => {
            debouncedSavePatch(args)
        },
        [debouncedSavePatch]
    )

    const flushPatch = useCallback(
        (args: ManualProductSaveArgs) => {
            debouncedSavePatch.cancel()
            void savePatch(args)
            toast("수정되었습니다.", {
                description: "수정된 내용이 저장되었습니다.",
            })
        },
        [debouncedSavePatch, savePatch]
    )

    return { savePatch, schedulePatch, flushPatch }
}
