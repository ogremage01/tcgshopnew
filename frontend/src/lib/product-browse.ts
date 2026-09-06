import { canonicalizeManualCategory } from "@/lib/manual-category-label";

export type ProductBrowseSearchParams = {
    q?: string;
    entryState?: "INITIAL" | "UPDATED";
    searchMode?: "DEFAULT" | "ADVANCED";
    games?: string;
    productTypes?: string;
    suppliesTypes?: string;
    manualCategories?: string;
    rarities?: string;
    setNames?: string;
    setCode?: string;
    isFoil?: string;
    isInStock?: string;
    page?: string;
    size?: string;
    sortBy?: string;
};

export type ResolvedProductBrowseParams = {
    q: string;
    entryState: "INITIAL" | "UPDATED";
    searchMode: "DEFAULT" | "ADVANCED";
    selectedGames: string[];
    selectedProductTypes: string[];
    selectedSuppliesTypes: string[];
    selectedManualCategories: string[];
    selectedRarities: string[];
    selectedSetNames: string[];
    selectedSetCode: string;
    selectedIsFoil: boolean | null;
    selectedIsInStock: boolean;
    page: string;
    size: string;
    sortBy: string;
};

export function parseCsv(value: string): string[] {
    return value
        .split(",")
        .map((v) => v.trim())
        .filter(Boolean);
}

export function parseBooleanParam(value: string): boolean | null {
    if (value === "true") return true;
    if (value === "false") return false;
    return null;
}

export function toBackendSort(value: string): string {
    switch (value) {
        case "0":
            return "setNumber,asc";
        case "1":
            return "productName,asc";
        case "2":
            return "productName,desc";
        case "3":
            return "sortPrice,desc";
        case "4":
            return "sortPrice,asc";
        default:
            return "setNumber,asc";
    }
}

export function resolveProductBrowseParams(
    raw: ProductBrowseSearchParams,
    defaults?: {
        defaultSetNames?: string[];
        defaultSetCode?: string;
        defaultProductTypes?: string[];
        defaultManualCategories?: string[];
    }
): ResolvedProductBrowseParams {
    const {
        q = "",
        entryState = "INITIAL",
        searchMode = "DEFAULT",
        games = "",
        productTypes = "",
        suppliesTypes = "",
        manualCategories = "",
        rarities = "",
        setNames = "",
        setCode = "",
        isFoil = "",
        isInStock = "",
        page = "0",
        size = "24",
        sortBy = "0",
    } = raw;

    const selectedSetNamesFromParam = parseCsv(setNames);
    const mergedSetNames =
        selectedSetNamesFromParam.length > 0
            ? selectedSetNamesFromParam
            : (defaults?.defaultSetNames ?? []);
    const mergedSetCode = setCode || defaults?.defaultSetCode || "";

    const selectedProductTypesFromParam = parseCsv(productTypes);
    const selectedManualCategoriesFromParam = parseCsv(manualCategories);

    return {
        q,
        entryState: entryState as "INITIAL" | "UPDATED",
        searchMode: searchMode as "DEFAULT" | "ADVANCED",
        selectedGames: parseCsv(games),
        selectedProductTypes:
            selectedProductTypesFromParam.length > 0
                ? selectedProductTypesFromParam
                : (defaults?.defaultProductTypes ?? []),
        selectedSuppliesTypes: parseCsv(suppliesTypes),
        // 쿼리는 정식 카테고리명으로 맞춘다. 검색 별칭 확장은 서버 ManualCategoryAliases가 담당한다.
        selectedManualCategories:
            (selectedManualCategoriesFromParam.length > 0
                ? selectedManualCategoriesFromParam
                : (defaults?.defaultManualCategories ?? [])
            ).map(canonicalizeManualCategory),
        selectedRarities: parseCsv(rarities),
        selectedSetNames: mergedSetNames,
        selectedSetCode: mergedSetCode,
        selectedIsFoil: parseBooleanParam(isFoil),
        selectedIsInStock: isInStock === "true",
        page,
        size,
        sortBy,
    };
}

/**
 * 사이드바 facet 조회용 쿼리.
 * 기본적으로 facet 필터(productTypes 등)는 제외해, 선택해도 다른 옵션이 사라지지 않게 한다.
 * {@link includeScopeFilters}=true 이면 페이지 고정 범위(수동 상품 전용 등)만 init에 포함한다.
 */
export function buildSidebarInitQuery(
    params: ResolvedProductBrowseParams,
    options?: { includeScopeFilters?: boolean },
): URLSearchParams {
    const {
        q,
        entryState,
        searchMode,
        selectedGames,
        selectedProductTypes,
        selectedManualCategories,
        selectedSetCode,
        selectedSetNames,
        page,
        size,
        sortBy,
    } = params;

    const query = new URLSearchParams({ q, entryState, searchMode });
    if (selectedGames.length > 0) query.set("games", selectedGames.join(","));
    if (options?.includeScopeFilters) {
        if (selectedProductTypes.length > 0) {
            query.set("productTypes", selectedProductTypes.join(","));
        }
        if (selectedManualCategories.length > 0) {
            query.set("manualCategories", selectedManualCategories.join(","));
        }
    }
    if (selectedSetNames.length > 0) query.set("setNames", selectedSetNames.join(","));
    if (selectedSetCode) query.set("setCode", selectedSetCode);
    query.set("page", page);
    query.set("size", size);
    query.set("sortBy", sortBy);
    query.set("sort", toBackendSort(sortBy));
    return query;
}

export function buildRequestQuery(params: ResolvedProductBrowseParams): URLSearchParams {
    const {
        q,
        entryState,
        searchMode,
        selectedGames,
        selectedProductTypes,
        selectedSuppliesTypes,
        selectedManualCategories,
        selectedRarities,
        selectedSetNames,
        selectedSetCode,
        selectedIsFoil,
        selectedIsInStock,
        page,
        size,
        sortBy,
    } = params;

    const requestQuery = new URLSearchParams({ q, entryState, searchMode });
    if (selectedGames.length > 0) requestQuery.set("games", selectedGames.join(","));
    if (selectedProductTypes.length > 0) requestQuery.set("productTypes", selectedProductTypes.join(","));
    if (selectedSuppliesTypes.length > 0) requestQuery.set("suppliesTypes", selectedSuppliesTypes.join(","));
    if (selectedManualCategories.length > 0) {
        requestQuery.set("manualCategories", selectedManualCategories.join(","));
    }
    if (selectedRarities.length > 0) requestQuery.set("rarities", selectedRarities.join(","));
    if (selectedSetNames.length > 0) requestQuery.set("setNames", selectedSetNames.join(","));
    if (selectedSetCode) requestQuery.set("setCode", selectedSetCode);
    if (selectedIsFoil !== null) requestQuery.set("isFoil", String(selectedIsFoil));
    if (selectedIsInStock) requestQuery.set("isInStock", "true");
    requestQuery.set("page", page);
    requestQuery.set("size", size);
    requestQuery.set("sortBy", sortBy);
    requestQuery.set("sort", toBackendSort(sortBy));
    return requestQuery;
}

import { GAME_SEALED_BROWSE_CODES } from "@/lib/game-sealed-browse";

const GAME_SET_BROWSE_CODES = ["mtg", "fab", "swu", "lorc", "rift"] as const;

/**
 * 사이드바 facet init 조회용 쿼리.
 * page는 facet에 영향 없으므로 0으로 고정해 페이지 이동 시 init 재호출을 줄인다.
 */
export function buildProductBrowseInitQuery(
    params: ResolvedProductBrowseParams,
    options?: { includeScopeFilters?: boolean },
): URLSearchParams {
    return buildSidebarInitQuery({ ...params, page: "0" }, options);
}

/** Suspense boundary key — 필터·정렬·페이지 변경 시 결과 영역 로딩 fallback 재노출 */
export function buildProductBrowseSuspenseKey(
    searchParams: ProductBrowseSearchParams,
    options?: {
        defaultSetNames?: string[];
        defaultSetCode?: string;
        defaultProductTypes?: string[];
        defaultManualCategories?: string[];
    },
): string {
    const resolved = resolveProductBrowseParams(searchParams, options);
    return buildRequestQuery(resolved).toString();
}

/** `/game/{game}/sealed` sealed 상품 목록 경로 여부 (locale 제외 pathname 기준). */
export function isGameSealedBrowsePath(pathname: string): boolean {
    const segments = pathname.replace(/\/+$/, "").split("/").filter(Boolean);
    if (segments.length !== 3 || segments[0] !== "game") return false;
    return (
        segments[2] === "sealed" &&
        (GAME_SEALED_BROWSE_CODES as readonly string[]).includes(segments[1])
    );
}

/** `/game/{game}/{set}` 세트별 상품 목록 경로 여부 (locale 제외 pathname 기준). */
export function isGameSetBrowsePath(pathname: string): boolean {
    const segments = pathname.replace(/\/+$/, "").split("/").filter(Boolean);
    if (segments.length !== 3 || segments[0] !== "game") return false;
    if (segments[2] === "sealed") return false;
    return (GAME_SET_BROWSE_CODES as readonly string[]).includes(segments[1]);
}
