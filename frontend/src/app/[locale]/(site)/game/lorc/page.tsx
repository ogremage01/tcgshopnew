import { serverApi } from "@/lib/api.server"
import { BannerDto } from "@/types/banner"
import { TcgPSetInfoDto } from "@/types/tcgMetadata"
import MainCarousel from "../../_components/MainCarousel"
import TcgpSetList from "../_components/TcgpSetList"

const LORC_PRODUCT_LINE_ID = 71

type PageProps = { params: Promise<{ locale: string }> }
export default async function LorcPage({ params }: PageProps) {
    const { locale } = await params
    const [banners, sets] = await Promise.all([
        serverApi.get<BannerDto[]>("/api/lorc/banners"),
        serverApi.get<TcgPSetInfoDto[]>(`/api/game/sets/${LORC_PRODUCT_LINE_ID}`),
    ])
    return (
        <div className="container p-8 mx-auto">
            <section className="py-8">
                <MainCarousel banners={banners} />
            </section>
            <section className="mt-8 py-8">
                <TcgpSetList
                    productLineId={LORC_PRODUCT_LINE_ID}
                    game="lorc"
                    productLineName="Disney Lorcana"
                    initialSets={sets}
                />
            </section>
        </div>
    )
}