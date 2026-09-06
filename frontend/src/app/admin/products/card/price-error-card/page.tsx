"use client";

import AdminCardProductTable from "../_components/AdminCardProductTable";
import { usePriceErrorCards } from "../_hooks/usePriceErrorCards";

export default function PriceErrorCardPage() {
    const {
        data,
        currentPage,
        totalPages,
        totalElements,
        isLoading,
        handlePageChange,
        removeFromList,
    } = usePriceErrorCards();

    return (
        <div className="p-6 space-y-4">
            <div className="flex items-center justify-between">
                <h1 className="text-2xl font-bold">가격 오류 카드 관리</h1>
                <span className="text-sm text-muted-foreground">
                    총 {totalElements}건
                </span>
            </div>
            <p className="text-sm text-muted-foreground">
                시장가(UnionPrice.price)가 0인 카드 제품 목록입니다. 가격 ingestion 후 자동으로 비공개 처리되며, 가격이 정상화되면 자동 재공개됩니다.
            </p>
            <AdminCardProductTable
                cardList={data}
                facets={[]}
                filters={{}}
                isLoading={isLoading}
                currentPage={currentPage}
                totalPages={totalPages}
                onPageChange={handlePageChange}
                onApplyFilters={() => {}}
                onProductDeleted={removeFromList}
            />
        </div>
    );
}
