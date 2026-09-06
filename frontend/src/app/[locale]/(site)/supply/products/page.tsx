import ProductBrowseLayout from "@/components/product-browse/ProductBrowseLayout";
import type { ProductBrowseSearchParams } from "@/lib/product-browse";

const DEFAULT_PRODUCT_TYPES = ["Supplies"] as const;

export default async function SupplyProductsPage({
    searchParams,
}: {
    searchParams: Promise<ProductBrowseSearchParams>;
}) {
    const resolved = await searchParams;

    return (
        <div>
            <ProductBrowseLayout
                searchParams={resolved}
                productsEndpoint="/api/products"
                defaultProductTypes={[...DEFAULT_PRODUCT_TYPES]}
            />
        </div>
    );
}
