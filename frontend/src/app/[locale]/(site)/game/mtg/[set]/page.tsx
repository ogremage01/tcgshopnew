import type { ProductBrowseSearchParams } from "@/lib/product-browse";
import ProductBrowseSection from "@/components/product-browse/ProductBrowseSection";
import { SetBannerSection } from "@/components/set-banner/SetBannerSection";
import { fetchSetBanner } from "@/lib/fetch-set-banner";
import { serverApi } from "@/lib/api.server";
import { MtgSetInfoDto } from "@/types/tcgMetadata";
import { getTranslations } from "next-intl/server";
import NoProductsFoundComponent from "@/components/product-browse/NoProductFoundCompoent";

export default async function MtgSetPage({
    params,
    searchParams,
}: {
    params: Promise<{ set: string }>;
    searchParams: Promise<ProductBrowseSearchParams>;
}) {
    const { set } = await params;
    const resolved = await searchParams;
    const encodedSet = encodeURIComponent(set);
    const [setData, setBanner] = await Promise.all([
        serverApi.get<MtgSetInfoDto>(`/api/mtg/sets/${encodedSet}`),
        fetchSetBanner("mtg", set),
    ]);
    const t = await getTranslations("game");

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
                <SetBannerSection banner={setBanner} />
                <h1 className="text-2xl font-bold">
                    {setData?.name} {t("setList.sets")}
                </h1>
                <p className="text-sm text-muted-foreground">
                    {t("setList.code")}: {setData?.setCode}
                </p>
                <p className="text-sm text-muted-foreground">
                    {t("setList.releaseDate")}:{" "}
                    {new Date(setData?.releaseDate ?? "").toLocaleDateString()}
                </p>
            </div>
            <ProductBrowseSection
                searchParams={resolved}
                productsEndpoint={`/api/products/search/game/mtg/${encodedSet}`}
                defaultSetCode={set}
                showFoilFilter
                headerContent={
                    <h1 className="text-2xl font-bold">
                        {setData?.name} {t("setList.productsList")}
                    </h1>
                }
                emptyContent={
                    <NoProductsFoundComponent />
                }
            />
        </div>
    );
}
