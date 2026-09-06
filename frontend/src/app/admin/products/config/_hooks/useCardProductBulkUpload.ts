"use client"

import { useCallback, useState } from "react"
import { api } from "@/lib/api"
import type { ExcelUploadResultDto } from "@/types/product"

export function useCardProductBulkUpload() {
    const [uploadResult, setUploadResult] = useState<ExcelUploadResultDto | null>(null)
    const [uploadedFileName, setUploadedFileName] = useState<string>("")

    const handleUploadCardProduct = useCallback((e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault()
        const form = e.target as HTMLFormElement
        const file = (new FormData(form).get("cardFile") as File | null) ?? null
        if (!file || file.size === 0) {
            alert("파일을 선택해주세요.")
            return
        }
        setUploadedFileName(file.name)
        const body = new FormData()
        body.append("file", file)
        void api
            .postFormData(`/api/admin/product/single-products/multiple/upload`, body)
            .then((response) => {
                const result = response as ExcelUploadResultDto
                setUploadResult(result)
                if (result.failCount > 0) {
                    alert("에러가 발생했습니다. 확인해주세요.")
                    return
                }
                alert("업로드 완료")
                form.reset()
                return
            })
            .catch(() => {
                setUploadResult(null)
                form.reset()
            })
    }, [])

    return { uploadResult, uploadedFileName, handleUploadCardProduct }
}
