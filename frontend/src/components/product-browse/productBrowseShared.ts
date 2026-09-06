import {
    buildProductBrowseInitQuery,
    buildRequestQuery,
    resolveProductBrowseParams,
    type ProductBrowseSearchParams,
    type ResolvedProductBrowseParams,
} from "@/lib/product-browse";

export type ProductBrowseLayoutOptions = {
    searchParams: ProductBrowseSearchParams;
    defaultSetNames?: string[];
    defaultSetCode?: string;
    defaultProductTypes?: string[];
    defaultManualCategories?: string[];
    showFoilFilter?: boolean;
};

export type ResolvedProductBrowseContext = {
    resolved: ResolvedProductBrowseParams;
    requestParams: ResolvedProductBrowseParams;
    initQueryString: string;
    productsUrl: string;
    initSuspenseKey: string;
    resultsSuspenseKey: string;
    hasScopedDefaults: boolean;
};

export function resolveProductBrowseContext(
    options: ProductBrowseLayoutOptions,
    productsEndpoint: string,
): ResolvedProductBrowseContext {
    const {
        searchParams,
        defaultSetNames,
        defaultSetCode,
        defaultProductTypes,
        defaultManualCategories,
        showFoilFilter = false,
    } = options;

    const defaults = {
        defaultSetNames,
        defaultSetCode,
        defaultProductTypes,
        defaultManualCategories,
    };

    const resolved = resolveProductBrowseParams(searchParams, defaults);
    const requestParams = showFoilFilter
        ? resolved
        : { ...resolved, selectedIsFoil: null };
    const hasScopedDefaults =
        (defaultProductTypes?.length ?? 0) > 0 ||
        (defaultManualCategories?.length ?? 0) > 0;
    const initQuery = buildProductBrowseInitQuery(resolved, {
        includeScopeFilters: hasScopedDefaults,
    });
    const requestQuery = buildRequestQuery(requestParams);

    return {
        resolved,
        requestParams,
        initQueryString: initQuery.toString(),
        productsUrl: `${productsEndpoint}?${requestQuery.toString()}`,
        initSuspenseKey: initQuery.toString(),
        resultsSuspenseKey: requestQuery.toString(),
        hasScopedDefaults,
    };
}
