import { cache } from "react";
import { unstable_cache } from "next/cache";
import { serverApi, serverApiPublic } from "@/lib/api.server";
import type { PageResponse, ProductItemDto, SearchInitResponseDto } from "@/types/product";

/** facet init — page 제외 쿼리 기준으로 캐시 (페이지 이동 시 재호출 방지) */
export const fetchProductBrowseInitCached = unstable_cache(
    async (initQueryString: string) =>
        serverApiPublic.get<SearchInitResponseDto>(
            `/api/products/search/init?${initQueryString}`,
        ),
    ["product-browse-init"],
    { revalidate: 60 },
);

/** 동일 렌더 패스 내 products 중복 요청 방지. 목록 재고는 이 API만 사용한다. */
export const fetchProductBrowseProducts = cache(
    async (productsUrl: string) =>
        serverApi.get<PageResponse<ProductItemDto>>(productsUrl),
);
