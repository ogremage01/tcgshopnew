"use client"
import Autoplay from "embla-carousel-autoplay"
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from "@/components/ui/carousel"
import { BannerDto } from "@/types/banner"
import { resolveAssetUrl } from "@/lib/public-asset-url"
import Image from "next/image"
import { Link } from "@/i18n/navigation"
export default function MainCarousel({ banners }: { banners: BannerDto[] }) {
    return (
        <Carousel plugins={[Autoplay({ delay: 3000 })]} opts={{ loop: true }}>
            <CarouselContent>
                {banners.map((banner, index) => (
                    <CarouselItem key={banner.id}>
                        <div className="relative w-full overflow-hidden aspect-[3/1]">
                            <Link href={banner.link} className="absolute inset-0">
                                <Image
                                    src={resolveAssetUrl(banner.imageUrl)}
                                    alt={banner.title ?? "banner image"}
                                    fill
                                    className="object-contain"
                                    sizes="(max-width: 768px) 100vw, 1200px"
                                    priority={index === 0}
                                />
                            </Link>
                        </div>
                    </CarouselItem>
                ))}
            </CarouselContent>
            {/* <CarouselNext />
            <CarouselPrevious /> */}
        </Carousel>
    )
}
