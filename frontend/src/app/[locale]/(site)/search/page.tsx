import type { ProductBrowseSearchParams } from "@/lib/product-browse";
import ProductBrowseSection from "@/components/product-browse/ProductBrowseSection";

export default async function SearchPage({
    searchParams,
}: {
    searchParams: Promise<ProductBrowseSearchParams>;
}) {
    const resolved = await searchParams;
    const q = resolved.q ?? "";

    return (
        <>
            <div className="container mx-auto">
                <div className="my-4 flex flex-col gap-2 rounded-md bg-gray-100 p-4">
                    <h1 className="text-2xl font-bold">검색 결과</h1>
                    <p className="text-sm text-gray-500">검색어: {q}</p>
                </div>
            </div>
            <ProductBrowseSection
                searchParams={resolved}
                productsEndpoint="/api/products/search"
                headerContent={null}
                showFoilFilter
            />
        </>
    );
}
