import type { ProductBrowseSearchParams } from "@/lib/product-browse";
import GameSealedBrowsePage from "@/components/game/GameSealedBrowsePage";

export default function RiftSealedPage({
    searchParams,
}: {
    searchParams: Promise<ProductBrowseSearchParams>;
}) {
    return <GameSealedBrowsePage gameCode="rift" searchParams={searchParams} />;
}
