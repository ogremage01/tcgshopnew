import { serverApi } from "@/lib/api.server"
import { BannerDto } from "@/types/banner"
import MainCarousel from "../../_components/MainCarousel"
import MtgSetList from "./_components/MtgSetList"
import { Separator } from "@/components/ui/separator"
type PageProps = { params: Promise<{ locale: string }> }
export default async function MTGPage({ params }: PageProps) {
    const { locale } = await params
    const banners = await serverApi.get<BannerDto[]>("/api/mtg/banners", {
    })
    return (
        <div className="container p-8 mx-auto">
            <section className="py-8">
                <MainCarousel banners={banners} />
            </section>
            <section className="mt-8 py-8">

                <MtgSetList />
            </section>
        </div>
    )
}