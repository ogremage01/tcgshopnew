import { serverApi } from "@/lib/api.server"
import { BannerDto } from "@/types/banner"
import { FabSetInfoDto } from "@/types/tcgMetadata"
import MainCarousel from "../../_components/MainCarousel"
import FabSetList from "./_components/FabSetList"

type PageProps = { params: Promise<{ locale: string }> }
export default async function FabPage({ params }: PageProps) {
    const { locale } = await params
    const [banners, sets] = await Promise.all([
        serverApi.get<BannerDto[]>("/api/fab/banners"),
        serverApi.get<FabSetInfoDto[]>("/api/fab/sets"),
    ])
    return (
        <div className="container p-8 mx-auto">
            <section className="py-8">
                <MainCarousel banners={banners} />
            </section>
            <section className="mt-8 py-8">
                <FabSetList initialSets={sets} />
            </section>
        </div>
    )
}
