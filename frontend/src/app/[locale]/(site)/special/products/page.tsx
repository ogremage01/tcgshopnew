import ProductBrowseLayout from "@/components/product-browse/ProductBrowseLayout";
import type { ProductBrowseSearchParams } from "@/lib/product-browse";

const DEFAULT_PRODUCT_TYPES = ["ManualProducts"] as const;

export default async function ProductsPage({
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
