import type { ProductBrowseSearchParams } from "@/lib/product-browse";
import GameSealedBrowsePage from "@/components/game/GameSealedBrowsePage";

export default function MtgSealedPage({
    searchParams,
}: {
    searchParams: Promise<ProductBrowseSearchParams>;
}) {
    return <GameSealedBrowsePage gameCode="mtg" searchParams={searchParams} />;
}
