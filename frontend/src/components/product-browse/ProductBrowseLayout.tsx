import { Suspense } from "react";
import { Separator } from "@/components/ui/separator";
import ProductBrowseSidebarSection from "./ProductBrowseSidebarSection";
import ProductBrowseResultsSection from "./ProductBrowseResultsSection";
import { ProductResultsLoading } from "./ProductBrowseLoading";
import { resolveProductBrowseContext } from "./productBrowseShared";
import type { ProductBrowseSearchParams } from "@/lib/product-browse";

type Props = {
    searchParams: ProductBrowseSearchParams;
    headerContent?: React.ReactNode;
    productsEndpoint: string;
    defaultSetNames?: string[];
    defaultSetCode?: string;
    defaultProductTypes?: string[];
    defaultManualCategories?: string[];
    emptyContent?: React.ReactNode;
    showFoilFilter?: boolean;
};

export default function ProductBrowseLayout({
    searchParams,
    headerContent,
    productsEndpoint,
    defaultSetNames,
    defaultSetCode,
    defaultProductTypes,
    defaultManualCategories,
    emptyContent,
    showFoilFilter = false,
}: Props) {
    const context = resolveProductBrowseContext(
        {
            searchParams,
            defaultSetNames,
            defaultSetCode,
            defaultProductTypes,
            defaultManualCategories,
            showFoilFilter,
        },
        productsEndpoint,
    );

    return (
        <div className="container mx-auto">
            {headerContent != null && (
                <>
                    <div className="my-4 flex flex-col gap-2 rounded-md bg-gray-100 p-4">
                        {headerContent}
                    </div>
                    <Separator />
                </>
            )}
            <div
                key={context.initSuspenseKey}
                className="flex flex-col lg:flex-row gap-4"
            >
                <Suspense fallback={null}>
                    <ProductBrowseSidebarSection
                        context={context}
                        showFoilFilter={showFoilFilter}
                    />
                </Suspense>
                <Suspense
                    key={context.resultsSuspenseKey}
                    fallback={<ProductResultsLoading />}
                >
                    <ProductBrowseResultsSection
                        context={context}
                        emptyContent={emptyContent}
                    />
                </Suspense>
            </div>
        </div>
    );
}
