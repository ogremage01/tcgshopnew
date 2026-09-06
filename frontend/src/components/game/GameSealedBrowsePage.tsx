import type { ProductBrowseSearchParams } from "@/lib/product-browse";
import ProductBrowseSection from "@/components/product-browse/ProductBrowseSection";
import NoProductsFoundComponent from "@/components/product-browse/NoProductFoundCompoent";
import {
    GAME_SEALED_BROWSE_CONFIG,
    type GameSealedBrowseCode,
} from "@/lib/game-sealed-browse";
import { getTranslations } from "next-intl/server";

type Props = {
    gameCode: GameSealedBrowseCode;
    searchParams: Promise<ProductBrowseSearchParams>;
};

export default async function GameSealedBrowsePage({
    gameCode,
    searchParams,
}: Props) {
    const config = GAME_SEALED_BROWSE_CONFIG[gameCode];
    const resolved = await searchParams;
    const mergedSearchParams: ProductBrowseSearchParams = {
        ...resolved,
        games: config.gameFilter,
    };
    const tSealed = await getTranslations(
        `game.sealed.${config.translationKey}`,
    );

    return (
        <div className="flex flex-col gap-4">
            <div className="flex flex-col gap-2">
                <h1 className="text-2xl font-bold">{tSealed("title")}</h1>
            </div>
            <ProductBrowseSection
                searchParams={mergedSearchParams}
                productsEndpoint="/api/products/search"
                defaultProductTypes={["SealedProducts"]}
                headerContent={
                    <h1 className="text-2xl font-bold">{tSealed("listTitle")}</h1>
                }
                emptyContent={<NoProductsFoundComponent />}
            />
        </div>
    );
}
