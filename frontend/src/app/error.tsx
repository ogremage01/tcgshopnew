"use client"

import ErrorFallback from "@/components/ErrorFallback"
export default function Error() {
    return (
        <ErrorFallback
            title="에러가 발생했습니다."
            description="관리자에게 문의해주세요."
            homeLabel="메인으로 돌아가기"
            homeHref="/"
        />
    )
}