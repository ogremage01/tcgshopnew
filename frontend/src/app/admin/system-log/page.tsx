"use client"

import TCGPMetadataSyncLogTab from "@/app/admin/products/config/_components/TCGPMetadataSyncLogTab"

export default function AdminProductSyncLogPage() {
    return (
        <div className="flex flex-col gap-4">
            <div>
                <h1 className="text-2xl font-bold">시스템 로그</h1>
                <p className="text-sm text-muted-foreground">TCGPlayer·가격 동기화 등 작업 이력을 확인합니다.</p>
            </div>
            <TCGPMetadataSyncLogTab />
        </div>
    )
}
