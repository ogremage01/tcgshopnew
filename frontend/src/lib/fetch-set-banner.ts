import { serverApi } from "@/lib/api.server"
import type { SetBannerDto } from "@/types/banner"

export async function fetchSetBanner(game: string, set: string): Promise<SetBannerDto | null> {
    const encodedSet = encodeURIComponent(set)
    const path =
        game === "mtg"
            ? `/api/mtg/sets/${encodedSet}/banner`
            : game === "fab"
                ? `/api/fab/sets/${encodedSet}/banner`
            : `/api/game/${game}/sets/${encodedSet}/banner`

    return serverApi.get<SetBannerDto>(path).catch(() => null)
}
