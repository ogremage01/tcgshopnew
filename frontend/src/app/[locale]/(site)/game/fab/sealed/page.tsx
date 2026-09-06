import type { ProductBrowseSearchParams } from "@/lib/product-browse";
import GameSealedBrowsePage from "@/components/game/GameSealedBrowsePage";

export default function FabSealedPage({
    searchParams,
}: {
    searchParams: Promise<ProductBrowseSearchParams>;
}) {
    return <GameSealedBrowsePage gameCode="fab" searchParams={searchParams} />;
}
