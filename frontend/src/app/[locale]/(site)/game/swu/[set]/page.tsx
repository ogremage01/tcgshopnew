import { SetBannerSection } from "@/components/set-banner/SetBannerSection";
import { fetchSetBanner } from "@/lib/fetch-set-banner";
import { serverApi } from "@/lib/api.server";
import { TcgPSetInfoDto } from "@/types/tcgMetadata";
import type { ProductBrowseSearchParams } from "@/lib/product-browse";
import ProductBrowseSection from "@/components/product-browse/ProductBrowseSection";
import { getTranslations } from "next-intl/server";
import NoProductsFoundComponent from "@/components/product-browse/NoProductFoundCompoent";

export default async function SWUSetPage({
    params,
    searchParams,
}: {
    params: Promise<{ set: string }>;
    searchParams: Promise<ProductBrowseSearchParams>;
}) {
    const { set } = await params;
    const encodedSet = encodeURIComponent(set);
    const [setData, setBanner] = await Promise.all([
        serverApi.get<TcgPSetInfoDto>(`/api/game/swu/sets/${encodedSet}`),
        fetchSetBanner("swu", set),
    ]);
    const resolved = await searchParams;
    const t = await getTranslations("game");

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-4">
                <SetBannerSection banner={setBanner} />
                <h1 className="text-2xl font-bold">
                    {setData?.setName} {t("setList.sets")}
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
                productsEndpoint={`/api/products/search/game/swu/${setData?.setCode ?? ""}`}
                defaultSetCode={setData?.setCode}
                showFoilFilter
                headerContent={
                    <h1 className="text-2xl font-bold">
                        {setData?.setName} {t("setList.productsList")}
                    </h1>
                }
                emptyContent={
                    <NoProductsFoundComponent />
                }
            />
        </div>
    );
}
