import ProductFilterSidebar from "./ProductFilterSidebar";
import {
    fetchProductBrowseInitCached,
    fetchProductBrowseProducts,
} from "@/lib/product-browse/fetch.server";
import type { ResolvedProductBrowseContext } from "./productBrowseShared";

type Props = {
    context: ResolvedProductBrowseContext;
    showFoilFilter?: boolean;
};

export default async function ProductBrowseSidebarSection({
    context,
    showFoilFilter = false,
}: Props) {
    const { resolved, initQueryString, productsUrl } = context;

    const [initResponse, products] = await Promise.all([
        fetchProductBrowseInitCached(initQueryString),
        fetchProductBrowseProducts(productsUrl),
    ]);

    if (products.content.length === 0) return null;

    return (
        <ProductFilterSidebar
            filters={initResponse}
            selectedGames={resolved.selectedGames}
            selectedProductTypes={resolved.selectedProductTypes}
            selectedSuppliesTypes={resolved.selectedSuppliesTypes}
            selectedManualCategories={resolved.selectedManualCategories}
            selectedRarities={resolved.selectedRarities}
            selectedSetNames={resolved.selectedSetNames}
            isFoil={resolved.selectedIsFoil}
            isInStock={resolved.selectedIsInStock}
            showFoilFilter={showFoilFilter}
        />
    );
}
