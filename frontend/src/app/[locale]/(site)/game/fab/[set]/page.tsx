import { SetBannerSection } from "@/components/set-banner/SetBannerSection";
import { fetchSetBanner } from "@/lib/fetch-set-banner";
import { serverApi } from "@/lib/api.server";
import { FabSetInfoDto } from "@/types/tcgMetadata";
import type { ProductBrowseSearchParams } from "@/lib/product-browse";
import ProductBrowseSection from "@/components/product-browse/ProductBrowseSection";
import { getTranslations } from "next-intl/server";
import NoProductsFoundComponent from "@/components/product-browse/NoProductFoundCompoent";

export default async function FabSetPage({
    params,
    searchParams,
}: {
    params: Promise<{ locale: string; set: string }>;
    searchParams: Promise<ProductBrowseSearchParams>;
}) {
    const { locale, set } = await params;
    const t = await getTranslations({ locale });
    const encodedSet = encodeURIComponent(set);
    const [setData, setBanner] = await Promise.all([
        serverApi.get<FabSetInfoDto>(`/api/fab/sets/${encodedSet}`).catch(() => null),
        fetchSetBanner("fab", set),
    ]);
    const resolved = await searchParams;

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-4">
                <SetBannerSection banner={setBanner} />
                <h1 className="text-2xl font-bold">
                    {setData?.name} {t("game.setList.sets")}
                </h1>
                <p className="text-sm text-muted-foreground">
                    {t("game.setList.code")}: {setData?.setCode}
                </p>
            </div>
            <ProductBrowseSection
                searchParams={resolved}
                productsEndpoint={`/api/products/search/game/fab/${setData?.setCode ?? ""}`}
                defaultSetCode={setData?.setCode}
                showFoilFilter
                headerContent={
                    <h1 className="text-2xl font-bold">
                        {setData?.name} {t("game.setList.productsList")}
                    </h1>
                }
                emptyContent={
                    <NoProductsFoundComponent />
                }
            />
        </div>
    );
}
