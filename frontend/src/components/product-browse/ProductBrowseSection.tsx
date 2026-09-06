import ProductBrowseLayout from "./ProductBrowseLayout";
import type { ProductBrowseSearchParams } from "@/lib/product-browse";

export type ProductBrowseSectionProps = {
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

export default async function ProductBrowseSection(props: ProductBrowseSectionProps) {
    return <ProductBrowseLayout {...props} />;
}
