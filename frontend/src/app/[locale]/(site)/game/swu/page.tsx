import { serverApi } from "@/lib/api.server"
import { BannerDto } from "@/types/banner"
import { TcgPSetInfoDto } from "@/types/tcgMetadata"
import MainCarousel from "../../_components/MainCarousel"
import TcgpSetList from "../_components/TcgpSetList"
import { getTranslations } from "next-intl/server"

const SWU_PRODUCT_LINE_ID = 79

type PageProps = { params: Promise<{ locale: string }> }
export default async function SWUPage({ params }: PageProps) {
    await params
    const t = await getTranslations("game.productLineName")
    const [banners, sets] = await Promise.all([
        serverApi.get<BannerDto[]>("/api/swu/banners"),
        serverApi.get<TcgPSetInfoDto[]>(`/api/game/sets/${SWU_PRODUCT_LINE_ID}`),
    ])
    return (
        <div className="container p-8 mx-auto">
            <section className="py-8">
                <MainCarousel banners={banners} />
            </section>
            <section className="mt-8 py-8">
                <TcgpSetList
                    productLineId={SWU_PRODUCT_LINE_ID}
                    game="swu"
                    productLineName={t("swu")}
                    initialSets={sets}
                />
            </section>
        </div>
    )
}
