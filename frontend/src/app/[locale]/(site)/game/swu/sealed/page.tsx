import type { ProductBrowseSearchParams } from "@/lib/product-browse";
import GameSealedBrowsePage from "@/components/game/GameSealedBrowsePage";

export default function SwuSealedPage({
    searchParams,
}: {
    searchParams: Promise<ProductBrowseSearchParams>;
}) {
    return <GameSealedBrowsePage gameCode="swu" searchParams={searchParams} />;
}
