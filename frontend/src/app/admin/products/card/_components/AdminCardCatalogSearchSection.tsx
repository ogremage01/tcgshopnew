"use client"

import AdminSearchBar from "@/app/admin/_components/AdminSearchBar"
import { ADMIN_CARD_CATALOG_SEARCH_CARDS_URL } from "../_hooks/useUnionPriceSearch"

type AdminCardCatalogSearchSectionProps = {
    onSearchRequest: (keyword: string) => void
}

export default function AdminCardCatalogSearchSection({
    onSearchRequest,
}: AdminCardCatalogSearchSectionProps) {
    return (
        <AdminSearchBar
            url={ADMIN_CARD_CATALOG_SEARCH_CARDS_URL}
            queryParam="keyword"
            pageParam="page"
            sizeParam="size"
            placeholder="검색"
            onSearchRequest={({ keyword }) => onSearchRequest(keyword)}
            description="싱글카드 검색"
        />
    )
}
