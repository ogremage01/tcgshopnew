import type { Metadata } from "next"
import { getTranslations } from "next-intl/server"
import MainCarousel from "./_components/MainCarousel"
import { ContentBlock } from "./_components/ContentBlock"
import { serverApi } from "@/lib/api.server"
import { BannerDto } from "@/types/banner"
import type { MainPageContentDto } from "@/types/mainPageContent"
export const dynamic = "force-dynamic"

type PageProps = { params: Promise<{ locale: string }> }

export async function generateMetadata({ params }: PageProps): Promise<Metadata> {
  const { locale } = await params
  const t = await getTranslations({ locale })
  return {
    title: t("siteTitle"),
    description: t("siteDescription"),
  }
}

export default async function Home({ params }: PageProps) {
  const { locale } = await params
  const banners = await serverApi.get<BannerDto[]>("/api/main/banners")
  const contentBlocks = await serverApi.get<MainPageContentDto[]>("/api/main/content-blocks").catch(() => [])
  return (
    <div className="flex flex-col gap-8">
      <section className="text-center py-8 sm:py-12">
        <MainCarousel banners={banners} />
      </section>

      {contentBlocks.length > 0 && (
        <section className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 my-4">
          {contentBlocks.map((item) => (
            <ContentBlock key={item.id} item={item} />
          ))}
        </section>
      )}
    </div>
  )
}

