import ProductResultComponent from "./ProductResultComponent";
import { fetchProductBrowseProducts } from "@/lib/product-browse/fetch.server";
import { getTranslations } from "next-intl/server";
import type { ResolvedProductBrowseContext } from "./productBrowseShared";

type Props = {
    context: ResolvedProductBrowseContext;
    emptyContent?: React.ReactNode;
};

export default async function ProductBrowseResultsSection({
    context,
    emptyContent,
}: Props) {
    const t = await getTranslations("productBrowse");
    const { resolved, productsUrl } = context;

    const products = await fetchProductBrowseProducts(productsUrl);

    if (products.content.length === 0) {
        return (
            emptyContent ?? (
                <div className="flex flex-col items-center justify-center h-full mx-auto my-4 w-full">
                    <p className="text-2xl font-bold">{t("noResults")}</p>
                </div>
            )
        );
    }

    return (
        <ProductResultComponent
            products={products}
            sortBy={resolved.sortBy}
            size={resolved.size}
        />
    );
}
